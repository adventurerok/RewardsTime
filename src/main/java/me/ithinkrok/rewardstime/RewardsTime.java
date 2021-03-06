package me.ithinkrok.rewardstime;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;

import me.ithinkrok.rewardstime.RewardsBonus.BonusType;
import me.ithinkrok.rewardstime.listener.*;
import me.ithinkrok.rewardstime.metrics.Metrics;
import me.ithinkrok.rewardstime.permissions.*;
import me.ithinkrok.rewardstime.task.TaskDaily;
import me.ithinkrok.rewardstime.task.TaskDailyBonus;
import me.ithinkrok.rewardstime.vault.IVaultEconomy;
import me.ithinkrok.rewardstime.vault.VaultEconomy;
import me.ithinkrok.rewardstime.votifier.VotifierApi;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class RewardsTime extends JavaPlugin {

	public static enum ArmorMaterial {
		DIAMOND, GOLD, CHAINMAIL, IRON, LEATHER, OTHER
	}

	public static enum ArmorType {
		HELMET, CHESTPLATE, LEGGINGS, BOOTS, OTHER
	}

	public static enum FieldType {
		BOOLEAN, INTEGER, DOUBLE, STRING, BONUSTYPE
	}

	public IVaultEconomy ecoApi = null;
	public IPermissions permsApi = null;

	public DecimalFormat numberFormat = new DecimalFormat("0.##");

	public String title = ChatColor.DARK_PURPLE + "[RewardsTime]" + ChatColor.WHITE + " ";
	public String fieldColor = ChatColor.RED.toString();
	public String valColor = ChatColor.BLUE.toString();
	public String nameColor = ChatColor.GREEN.toString();
	public String typeColor = ChatColor.GOLD.toString();
	public String white = ChatColor.WHITE.toString();
	
	public TaskDaily dailyThread;

	public boolean broadcast = true;
	public boolean scheduled = false;
	public int scheduleTime = 300;
	public int damageTimeout = 60;
	public int voteSaveMinutes = 15;

	public Random rand = new Random();
	
	public HashMap<UUID, RewardsData> fiveChange = new HashMap<>();

	public HashMap<UUID, DamageData> entityDamageData = new HashMap<>();

	public HashMap<UUID, Integer> voteCounts = new HashMap<>();

	public ArrayList<Integer> voteEveryList = new ArrayList<>();

	public boolean log = true;
	public boolean mobRewards = true;
	public boolean craftRewards = true;
	public boolean smeltRewards = true;
	public boolean mobArmorBonus = true;
	public boolean toolBonus = true;
	public boolean blockRewards = true;
	public boolean voteRewards = true;
	public boolean rewardCreative = false;
	
	public boolean mobLootingBonus = true;
	
	public FileConfiguration config;

	public EnumMap<GameMode, Boolean> enabledGameModes = new EnumMap<>(GameMode.class);

	public EnumMap<ArmorMaterial, RewardsBonus> armorMaterial = new EnumMap<>(ArmorMaterial.class);
	public EnumMap<ArmorType, RewardsBonus> armorType = new EnumMap<>(ArmorType.class);

	public HashMap<String, FieldType> fieldTypes = new HashMap<>();

	@Override
	public void onEnable() {
		loadVoteCounts();
		fieldTypes.put("money", FieldType.DOUBLE);
		fieldTypes.put("bonus", FieldType.DOUBLE);
		fieldTypes.put("type", FieldType.BONUSTYPE);
		fieldTypes.put("items", FieldType.STRING);
		fieldTypes.put("exp", FieldType.INTEGER);
		fieldTypes.put("broadcast", FieldType.STRING);
		fieldTypes.put("tell", FieldType.STRING);
		fieldTypes.put("perms", FieldType.STRING);
		
		File conFile = new File(getDataFolder(), "config.yml");
		if (!conFile.exists()) { // Cannot use bukkit default config feature as
									// causes unintended side effects
			conFile.getParentFile().mkdirs();
			try {
				InputStream in = RewardsTime.class.getClassLoader().getResourceAsStream("configdefault.yml");
				FileOutputStream out = new FileOutputStream(conFile);
				int b = -1;
				while ((b = in.read()) != -1) {
					out.write(b);
				}
				in.close();
				out.close();
				getLogger().info("Created default config");
			} catch (IOException e) {
				getLogger().warning("Failed to copy default config");
				getLogger().warning(e.getMessage());
			}
			reloadConfig();
		}
		loadConfigValues();
		
		getServer().getPluginManager().registerEvents(new MineListener(this), this);
		getServer().getPluginManager().registerEvents(new CraftListener(this), this);
		getServer().getPluginManager().registerEvents(new MobListener(this), this);
		
		if(Bukkit.getPluginManager().isPluginEnabled("GroupManager")) {
			getLogger().info("Found GroupManager. Subgroups will work");
			permsApi = new GroupManagerPerms();
		}
		
		if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
			ecoApi = new VaultEconomy();
			if(!ecoApi.enabled()) ecoApi = null;
			if(permsApi == null){
				permsApi = new VaultPerms();
				if(!permsApi.enabled()) permsApi = null;
			}
		}
		if (Bukkit.getPluginManager().isPluginEnabled("Votifier")) {
			new VotifierApi().createListener(this);
		}
		
		if(permsApi == null){
			permsApi = new DefaultPerms();
		}
		
		
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {

			@Override
			public void run() {
				checkDamageTimeout();

			}
		}, damageTimeout * 20, damageTimeout * 20);

		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {

			@Override
			public void run() {
				saveVoteCounts();

			}
		}, voteSaveMinutes * 1200, voteSaveMinutes * 1200);
		
		dailyThread = new TaskDaily(this, config.getString("dailytime"));
		dailyThread.start();
		
		if(config.getBoolean("metrics", true)){
			try {
			    Metrics metrics = new Metrics(this);
			    metrics.start();
			} catch (IOException e) {
			    // Failed to submit the stats :-(
			}
		}
	}

	public void checkDamageTimeout() {
		long time = System.nanoTime();
		HashMap<UUID, DamageData> result = new HashMap<>();
		for (Entry<UUID, DamageData> entry : entityDamageData.entrySet()) {
			double diff = time - entry.getValue().lastDamage;
			diff /= 1_000_000_000d;
			if (diff <= damageTimeout) {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		entityDamageData.clear();
		entityDamageData = result;
	}

	@Override
	public void onDisable() {
		dailyThread.stop();
		saveVoteCounts();
		if (ecoApi != null) ecoApi.disable();
		if (permsApi != null) permsApi.disable();
	}

	public Object getFieldValue(CommandSender sender, FieldType type, String parse) {
		switch (type) {
		case DOUBLE:
			try {
				return Double.parseDouble(parse);
			} catch (NumberFormatException e) {
				sender.sendMessage("Field must be a number");
				return null;
			}
		case INTEGER:
			try {
				return Integer.parseInt(parse);
			} catch (NumberFormatException e) {
				sender.sendMessage("Field must be an integer");
			}
		case STRING:
			return parse;
		case BOOLEAN:
			if (!parse.equalsIgnoreCase("true") && !parse.equalsIgnoreCase("false")) {
				sender.sendMessage("Field must be a boolean (true/false)");
				return null;
			} else
				return Boolean.parseBoolean(parse);
		case BONUSTYPE:
			BonusType bonus = null;
			try {
				bonus = BonusType.valueOf(parse.toUpperCase());
			} catch (IllegalArgumentException e) {
			}
			if (bonus == null) {
				sender.sendMessage("Field must be a BonusType (multiply/add)");
			}
			return bonus;
		default:
			return null;
		}
	}

	public void loadConfigValues() {
		config = getConfig();
		mobRewards = config.getBoolean("rewards.mob", true);
		craftRewards = config.getBoolean("rewards.craft", true);
		smeltRewards = config.getBoolean("rewards.smelt", true);
		blockRewards = config.getBoolean("rewards.block", true);
		voteRewards = config.getBoolean("rewards.vote", true);

		mobArmorBonus = config.getBoolean("bonus.mobarmor", true);
		toolBonus = config.getBoolean("bonus.tool", true);

		mobLootingBonus = config.getBoolean("moblootingbonus", true);

		armorMaterial.clear();
		armorMaterial.put(ArmorMaterial.DIAMOND, loadBonus("mobarmor.material.diamond"));
		armorMaterial.put(ArmorMaterial.IRON, loadBonus("mobarmor.material.iron"));
		armorMaterial.put(ArmorMaterial.GOLD, loadBonus("mobarmor.material.gold"));
		armorMaterial.put(ArmorMaterial.LEATHER, loadBonus("mobarmor.material.leather"));
		armorMaterial.put(ArmorMaterial.CHAINMAIL, loadBonus("mobarmor.material.chainmail"));
		armorMaterial.put(ArmorMaterial.OTHER, loadBonus("mobarmor.material.other"));

		armorType.clear();
		armorType.put(ArmorType.HELMET, loadBonus("mobarmor.type.helmet"));
		armorType.put(ArmorType.CHESTPLATE, loadBonus("mobarmor.type.chestplate"));
		armorType.put(ArmorType.LEGGINGS, loadBonus("mobarmor.type.leggings"));
		armorType.put(ArmorType.BOOTS, loadBonus("mobarmor.type.boots"));
		armorType.put(ArmorType.OTHER, loadBonus("mobarmor.type.other"));

		enabledGameModes.clear();
		enabledGameModes.put(GameMode.SURVIVAL, config.getBoolean("gamemodes.survival", true));
		enabledGameModes.put(GameMode.CREATIVE, config.getBoolean("gamemodes.creative", false));
		enabledGameModes.put(GameMode.ADVENTURE, config.getBoolean("gamemodes.adventure", true));

		scheduleTime = config.getInt("rewardtime", 500);
		damageTimeout = config.getInt("damagetimeout", 60);
		voteSaveMinutes = config.getInt("votesaveminutes", 15);

		voteEveryList.clear();
		Map<String, Object> map = config.getValues(true);
		Object votesObject = map.get("votes");
		if (votesObject != null && votesObject instanceof ConfigurationSection) {
			map = ((ConfigurationSection) votesObject).getValues(true);
			Object everyObject = map.get("every");
			if (everyObject != null && everyObject instanceof ConfigurationSection) {
				Set<String> keys = ((ConfigurationSection) everyObject).getKeys(true);
				for (String s : keys) {
					try {
						int i = Integer.parseInt(s);
						voteEveryList.add(i);
					} catch (NumberFormatException e) {
						continue;
					}
				}
			}
		}

		// for(Integer i : voteEveryList){
		// getLogger().info("Detected reward for voting every " + i + " times");
		// }
	}

	public Object parseObject(String str) {
		if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")) {
			return Boolean.parseBoolean(str);
		}
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
		}
		try {
			return Double.parseDouble(str);
		} catch (NumberFormatException e) {
		}
		return str;
	}

	public void playerReward(OfflinePlayer player, double gain, double bonus, double loss) {
		if (ecoApi == null)
			return;
		double total = gain + bonus - loss;
		if (total > 0)
			economyDeposit(player, total);
		else if (total < 0)
			economyWithdraw(player, -total);
		if (!player.isOnline())
			return;
		RewardsData data = fiveChange.get(player.getUniqueId());
		if (data == null) {
			data = new RewardsData();
			fiveChange.put(player.getUniqueId(), data);
		}
		data.change(gain, bonus, loss);
		if (!scheduled) {
			BukkitScheduler scheduler = Bukkit.getScheduler();
			scheduler.scheduleSyncDelayedTask(this, new Runnable() {

				@Override
				public void run() {
					alertRewards();

				}
			}, scheduleTime * 20);
			scheduled = true;
		}

	}

	public void entityDamage(Entity entity, Player player, double amount) {
		DamageData data = entityDamageData.get(entity.getUniqueId());
		if (data == null) {
			data = new DamageData();
			entityDamageData.put(entity.getUniqueId(), data);
		}
		data.damageFrom(player.getUniqueId(), amount);
	}

	public boolean hasEntity(Entity entity) {
		return entityDamageData.get(entity.getUniqueId()) != null;
	}

	public void alertRewards() {
		for (Entry<UUID, RewardsData> entry : fiveChange.entrySet()) {
			Player player = Bukkit.getPlayer(entry.getKey());
			if (player == null)
				continue;
			RewardsData data = entry.getValue();
			alertRewards(player, data);
		}
		fiveChange.clear();
		scheduled = false;
	}
	
	public void alertRewards(Player player){
		RewardsData data = fiveChange.get(player.getUniqueId());
		if(data == null) player.sendMessage(title + "You have gained no rewards recently");
		else{
			alertRewards(player, data);
			fiveChange.remove(player.getUniqueId());
		}
	}
	
	public void alertRewards(Player player, RewardsData data){
		String gainStr = valColor + "$" + numberFormat.format(data.gained) + white;
		String bonusStr = valColor + "$" + numberFormat.format(data.gainedBonus) + white;
		String lossStr = valColor + "$" + numberFormat.format(data.lost) + white;
		String totalStr = valColor + "$" + numberFormat.format(data.getTotal()) + white;
		String message = title + "Over the last " + timeString(scheduleTime) + ", you gained " + gainStr + " (+"
				+ bonusStr + " bonus) and lost " + lossStr + " (Total: " + totalStr + ")";
		player.sendMessage(message);
	}

	public String timeString(int seconds) {
		if (seconds % 60 == 0) {
			return ChatColor.RED + "" + (seconds / 60) + white + " minutes";
		} else
			return ChatColor.RED + "" + (seconds / 60) + white + "m" + ChatColor.RED + "" + (seconds % 60) + white
					+ "s";
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("rewardstime")) {
			if (args.length < 1) {
				sender.sendMessage(title + "RewardsTime commands: ");
				if(sender.hasPermission("rewardstime.reload"))sender.sendMessage(ChatColor.GOLD + "/rewardstime reload" + white + ": Reloads the config");
				if(sender.hasPermission("rewardstime.set")){
					sender.sendMessage(ChatColor.GOLD + "/rewardstime field" + white + ": Gets/sets a field in the config");
					sender.sendMessage(ChatColor.GOLD + "/rewardstime config" + white + ": Gets/sets anything in the config");
				} else if(sender.hasPermission("rewardstime.get")){
					sender.sendMessage(ChatColor.GOLD + "/rewardstime field" + white + ": Gets a field in the config");
					sender.sendMessage(ChatColor.GOLD + "/rewardstime config" + white + ": Gets anything in the config");
				}
				if(sender instanceof Player && sender.hasPermission("rewardstime.rewards")){
					sender.sendMessage(ChatColor.GOLD + "/rewardstime rewards" + white + ": View your recent rewards");
					sender.sendMessage(ChatColor.GOLD + "/rewardstime perks" + white + ": Check your rewardstime perk levels");
				}
				if(sender.hasPermission("rewardstime.votes.self") || sender.hasPermission("rewardstime.votes.other")){
					sender.sendMessage(ChatColor.GOLD + "/rewardstime votes" + white + ": Check your vote count or someone elses vote count");
				}
				
				return true;
			} else if ("reload".equalsIgnoreCase(args[0])) {
				if(!sender.hasPermission("rewardstime.reload")){
					sender.sendMessage(title + "You do not have permission to use /rewardstime reload");
					return true;
				}
				dailyThread.stop();
				reloadConfig();
				loadConfigValues();
				dailyThread = new TaskDaily(this, config.getString("dailytime"));
				dailyThread.start();
				sender.sendMessage(title + "Config reloaded successfully!");
				return true;
			} else if("test".equalsIgnoreCase(args[0])){
				testFunc();
				return true;
			} else if ("field".equalsIgnoreCase(args[0])) {
				if (args.length < 4) {
					sender.sendMessage("Usage: /rewardstime field <type> <name> <field> [newvalue]");
					sender.sendMessage(" - <type>: The type of reward to get/set (craft/smelt/block/mob)");
					sender.sendMessage(" - <name>: The name of the reward to get/set (item/mob name)");
					sender.sendMessage(" - <field>: The field to get/set (use \"money\" to get/set the money reward)");
					sender.sendMessage(" - [newvalue]: The new value to set the field to");
					return true;
				}
				String type = args[1];
				String name = args[2];
				String field = args[3];
				if (!checkFieldCommand(sender, type, name, field))
					return true;
				String str = type + "." + name.toLowerCase() + "." + field;
				String strChat = typeColor + type + white + "." + nameColor + name.toLowerCase() + white + "."
						+ fieldColor + field;
				if (args.length < 5) {
					if(!sender.hasPermission("rewardstime.get")){
						sender.sendMessage(title + "You do not have permission to get values");
						return true;
					}
					if (!config.contains(str)) {
						sender.sendMessage(title + "No value is set for " + strChat);
					} else {
						sender.sendMessage(title + strChat + white + " is set to " + valColor + config.get(str));
					}
				} else {
					if(!sender.hasPermission("rewardstime.set")){
						sender.sendMessage(title + "You do not have permission to set values");
						return true;
					}
					Object val = getFieldValue(sender, fieldTypes.get(field), args[4]);
					if (val == null)
						return true;
					config.set(str, val);
					sender.sendMessage(title + strChat + white + " set to " + valColor + val);
					saveConfig();
					loadConfigValues();
				}
				return true;
			} else if ("config".equalsIgnoreCase(args[0])) {
				if (args.length < 2) {
					sender.sendMessage("Usage: /rewardstime config <field> [newvalue]");
					sender.sendMessage(" - <field>: The field to get/set");
					sender.sendMessage(" - [newvalue]: The value to set the field to");
					return true;
				}
				String field = args[1];
				if (args.length < 3) {
					if(!sender.hasPermission("rewardstime.get")){
						sender.sendMessage(title + "You do not have permission to get values");
						return true;
					}
					if (!config.contains(field)) {
						sender.sendMessage(title + "No value is set for " + fieldColor + field);
					} else {
						sender.sendMessage(title + fieldColor + field + white + " is set to " + valColor
								+ config.get(field));
					}
				} else {
					if(!sender.hasPermission("rewardstime.set")){
						sender.sendMessage(title + "You do not have permission to set values");
						return true;
					}
					config.set(field, parseObject(args[2]));
					sender.sendMessage(title + fieldColor + field + white + " set to " + valColor + args[2]);
					saveConfig();
					loadConfigValues();
				}
				return true;
			} else if("rewards".equalsIgnoreCase(args[0])){
				if(!sender.hasPermission("rewardstime.rewards")){
					sender.sendMessage(title + "You do not have permission to use /rewardstime rewards");
					return true;
				}
				if(!(sender instanceof Player)){
					sender.sendMessage(title + "You must be a player to use /rewardstime rewards");
					return true;
				}
				Player player = (Player) sender;
				alertRewards(player);
				return true;
			} else if("perks".equalsIgnoreCase(args[0])){
				if(!(sender instanceof Player)){
					sender.sendMessage(title + "You must be a player to use /rewardstime rewards");
					return true;
				}
				Player player = (Player) sender;
				sender.sendMessage(title + "Your perks are: ");
				sender.sendMessage(title + "Money Multiplier: " + getPlayerMoneyPerk(player));
				sender.sendMessage(title + "Xp Multiplier: " + getPlayerExpPerk(player));
				sender.sendMessage(title + "Item Multiplier: " + getPlayerItemPerk(player));
				return true;
			} else if("votes".equalsIgnoreCase(args[0])){
				if(!(sender instanceof Player) && args.length < 2){
					sender.sendMessage(title + "You must specify a player if you are the console");
					return true;
				}
				if(args.length == 1){
					if(!sender.hasPermission("rewardstime.votes.self")){
						sender.sendMessage(title + "You do not have permission to use this");
						return true;
					}
					sender.sendMessage(title + "You have voted " + valColor + getVotes(((Player)sender).getUniqueId()) + white + " times");
				} else {
					if(!sender.hasPermission("rewardstime.votes.other")){
						sender.sendMessage(title + "You do not have permission to use this");
						return true;
					}
					OfflinePlayer plr = Bukkit.getOfflinePlayer(args[1]);
					if(plr == null){
						sender.sendMessage(title + "That player does not exist");
						return true;
					}
					sender.sendMessage(title + plr.getName() + " has voted " + valColor + getVotes(plr.getUniqueId()) + white + " times");
				}
				return true;
			}
			return false;
		}
		return false;
	}

	private void testFunc() {
		new TaskDailyBonus(dailyThread).run();
	}

	public boolean checkFieldCommand(CommandSender sender, String type, String name, String field) {
		String[] nameParts = name.split("/");
		if (nameParts.length > 2) {
			sender.sendMessage(title + "Only one metadata slash is allowed");
			return true;
		}

		boolean isBonus = false;

		switch (type) {
		case "craft":
		case "smelt":
		case "block":
			if (Material.getMaterial(nameParts[0].toUpperCase()) == null) {
				sender.sendMessage(title + "Unknown item: " + nameColor + nameParts[0]);
				return false;
			}
			break;
		case "mob":
			EntityType t = null;
			try {
				t = EntityType.valueOf(nameParts[0].toUpperCase());
			} catch (IllegalArgumentException e) {
			}
			if (t == null) {
				sender.sendMessage(title + "Unknown entity: " + nameColor + nameParts[0]);
				return false;
			}
			break;
		case "mobarmor.material":
			ArmorMaterial mat = null;
			try {
				mat = ArmorMaterial.valueOf(name.toUpperCase());
			} catch (IllegalArgumentException e) {
			}
			if (mat == null) {
				sender.sendMessage(title + "Unknown armor material: " + nameColor + name);
				return false;
			}
			isBonus = true;
			break;
		case "mobarmor.type":
			ArmorType part = null;
			try {
				part = ArmorType.valueOf(name.toUpperCase());
			} catch (IllegalArgumentException e) {
			}
			if (part == null) {
				sender.sendMessage(title + "Unknown armor type: " + name + name);
				return false;
			}
			isBonus = true;
			break;
		case "tool.enchant":
			Enchantment enchant = Enchantment.getByName(nameParts[0]);
			if (enchant == null) {
				sender.sendMessage(title + "Unknown enchantment: " + nameParts[0]);
				return false;
			}
			isBonus = true;
			break;
		default:
			sender.sendMessage(title + "Unknown type: " + typeColor + type + white
					+ ", types are: [craft, smelt, block, mob, mobarmor.material, mobarmor.type]");
			return false;
		}

		if (isBonus) {
			if (!field.equals("type") && !field.equals("bonus")) {
				sender.sendMessage(title + "Unknown bonus field: " + fieldColor + field + white
						+ ", fields are [type, bonus]");
				return false;
			}
		} else {
			switch(field){
			case "money":
			case "exp":
			case "items":
			case "perms":
			case "broadcast":
			case "tell":
				return true;
			default:
				sender.sendMessage(title + "Unknown field: " + fieldColor + field + white + ", fields are: [money,items,exp,perms,broadcast,tell]");
				return false;
			}
		}
		return true;
	}

	public void economyWithdraw(OfflinePlayer player, double amount) {
		if (ecoApi != null)
			ecoApi.withdraw(player, amount);
	}

	public void economyDeposit(OfflinePlayer player, double amount) {
		if (ecoApi != null)
			ecoApi.deposit(player, amount);
	}

	public RewardsBonus loadBonus(String base) {
		double amount = config.getDouble(base + ".bonus", 0);
		String ty = config.getString(base + ".type", "none").toUpperCase();
		BonusType type = BonusType.NONE;
		try {
			type = BonusType.valueOf(ty);
		} catch (IllegalArgumentException e) {
		}
		return new RewardsBonus(type, amount);
	}

	public ArmorMaterial getArmorMaterial(Material mat) {
		switch (mat) {
		case DIAMOND_HELMET:
		case DIAMOND_CHESTPLATE:
		case DIAMOND_LEGGINGS:
		case DIAMOND_BOOTS:
			return ArmorMaterial.DIAMOND;
		case IRON_HELMET:
		case IRON_CHESTPLATE:
		case IRON_LEGGINGS:
		case IRON_BOOTS:
			return ArmorMaterial.IRON;
		case GOLD_HELMET:
		case GOLD_CHESTPLATE:
		case GOLD_LEGGINGS:
		case GOLD_BOOTS:
			return ArmorMaterial.GOLD;
		case LEATHER_HELMET:
		case LEATHER_CHESTPLATE:
		case LEATHER_LEGGINGS:
		case LEATHER_BOOTS:
			return ArmorMaterial.LEATHER;
		case CHAINMAIL_HELMET:
		case CHAINMAIL_CHESTPLATE:
		case CHAINMAIL_LEGGINGS:
		case CHAINMAIL_BOOTS:
			return ArmorMaterial.CHAINMAIL;
		default:
			return ArmorMaterial.OTHER;
		}
	}

	public void incrementVoteCount(UUID voter) {
		Integer count = voteCounts.get(voter);
		if (count == null)
			count = 0;
		voteCounts.put(voter, count + 1);
	}

	public int getVotes(UUID voter) {
		Integer count = voteCounts.get(voter);
		if (count == null)
			count = 0;
		return count;
	}

	public ArmorType getArmorType(Material mat) {
		switch (mat) {
		case DIAMOND_HELMET:
		case GOLD_HELMET:
		case IRON_HELMET:
		case LEATHER_HELMET:
		case CHAINMAIL_HELMET:
			return ArmorType.HELMET;
		case DIAMOND_CHESTPLATE:
		case GOLD_CHESTPLATE:
		case IRON_CHESTPLATE:
		case LEATHER_CHESTPLATE:
		case CHAINMAIL_CHESTPLATE:
			return ArmorType.CHESTPLATE;
		case DIAMOND_LEGGINGS:
		case GOLD_LEGGINGS:
		case IRON_LEGGINGS:
		case LEATHER_LEGGINGS:
		case CHAINMAIL_LEGGINGS:
			return ArmorType.LEGGINGS;
		case DIAMOND_BOOTS:
		case GOLD_BOOTS:
		case IRON_BOOTS:
		case LEATHER_BOOTS:
		case CHAINMAIL_BOOTS:
			return ArmorType.BOOTS;
		default:
			return ArmorType.OTHER;
		}
	}

	public void saveVoteCounts() {
		int num = voteCounts.size();
		File file = new File(getDataFolder(), "votes.bin");
		file.getParentFile().mkdirs();
		try {
			DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
			out.writeInt(num);
			for (Entry<UUID, Integer> entry : voteCounts.entrySet()) {
				out.writeLong(entry.getKey().getMostSignificantBits());
				out.writeLong(entry.getKey().getLeastSignificantBits());
				out.writeInt(entry.getValue());
			}
			out.close();
		} catch (IOException e) {
			getLogger().warning("Failed to save vote counts:");
			getLogger().warning(e.getMessage());
		}
	}

	public void loadVoteCounts() {
		File file = new File(getDataFolder(), "votes.bin");
		if (!file.exists())
			return;
		try {
			DataInputStream in = new DataInputStream(new FileInputStream(file));
			int num = in.readInt();
			for (int d = 0; d < num; ++d) {
				long most = in.readLong();
				long least = in.readLong();
				UUID id = new UUID(most, least);
				int votes = in.readInt();
				voteCounts.put(id, votes);
			}
			in.close();
		} catch (IOException e) {
			getLogger().warning("Failed to load vote counts:");
			getLogger().warning(e.getMessage());
		}
	}

	public BonusType getConfigBonusType(String path, BonusType def) {
		String at = config.getString(path);
		if (at == null || at == "")
			return def;
		BonusType result = def;
		try {
			result = BonusType.valueOf(at.toUpperCase());
		} catch (IllegalArgumentException e) {
		}
		return result;
	}
	
	public void givePlayerItems(Player player, ItemStack...items){
		for(ItemStack item : player.getInventory().addItem(items).values()){
			player.getWorld().dropItemNaturally(player.getLocation(), item);
		}
	}
	
	public void dropItems(Location loc, ItemStack...items){
		for(ItemStack item : items){
			loc.getWorld().dropItemNaturally(loc, item);
		}
	}
	
	public int getCraftAmount(ItemStack[] matrix){
		int min = 64;
		for(int d = 0; d < matrix.length; ++d){
			if(matrix[d] == null || matrix[d].getType() == Material.AIR || matrix[d].getAmount() < 1) continue;
			if(matrix[d].getAmount() < min) min = matrix[d].getAmount();
		}
		return min;
	}
	
	public Collection<ItemStack> computeDrops(String dropsStr, int mult){
		return computeDrops(dropsStr, mult, 0);
	}
	
	
	public Collection<ItemStack> computeDrops(String dropsStr, int mult, int add){
		ArrayList<ItemStack> result = new ArrayList<>();
		if(dropsStr == null || dropsStr.isEmpty()) return result;
		String dropParts[] = dropsStr.split(",");
		for(String drop : dropParts){
			String sections[] = drop.split("/");
			if(sections.length != 4) continue;
			try{
				Material mat = Material.getMaterial(sections[0].toUpperCase());
				if(mat == null || mat == Material.AIR) continue;
				int metadata = 0;
				if(sections[1] != null && !sections[1].isEmpty()) metadata = Integer.parseInt(sections[1]);
				int maxamount = (Integer.parseInt(sections[2]) + add) * mult;
				double percent = Double.parseDouble(sections[3]) / 100d;
				if(percent > 1) percent = 1;
				else if(percent <= 0) continue;
				int amount = 0;
				for(int d = 0; d < maxamount; ++d){
					if(rand.nextDouble() < percent) ++amount;
				}
				if(amount == 0) continue;
				while(amount > 0){
					result.add(new ItemStack(mat, Math.min(amount, mat.getMaxStackSize()), (short)metadata));
					amount -= mat.getMaxStackSize();
				} 
			} catch(NumberFormatException e){}
		}
		return result;
	}
	
	public int getFittingAmount(ItemStack fit, Inventory inv){
		int toFit = fit.getAmount();
		for(int d = 0; d < 36; ++d){
			if(toFit < 1) return fit.getAmount();
			ItemStack slot = inv.getItem(d);
			if(slot == null || slot.getType() == Material.AIR){
				int amt = Math.min(toFit, fit.getMaxStackSize());
				toFit -= amt;
			} else if(slot.isSimilar(fit)){
				int amt = Math.min(toFit, fit.getMaxStackSize() - slot.getAmount());
				toFit -= amt;
			}
		}
		return fit.getAmount() - toFit;
	}
	
	public void givePermissions(Player player, String perms){
		if(perms == null || perms.isEmpty()) return;
		if(permsApi == null) return;
		String[] parts = perms.split(",");
		for(String p : parts){
			boolean add = true;
			if(p.startsWith("+")){
				p = p.substring(1);
			} else if(p.startsWith("-")){
				add = false;
				p = p.substring(1);
			}
			if(!permsApi.setPermission(player, p, add)){
				getLogger().info("Failed to give perm: " + p);
			}
		}
	}
	
	public void givePermissionsOffline(OfflinePlayer player, String perms){
		if(perms == null || perms.isEmpty()) return;
		if(permsApi == null) return;
		String[] parts = perms.split(",");
		for(String p : parts){
			boolean add = true;
			if(p.startsWith("+")){
				p = p.substring(1);
			} else if(p.startsWith("-")){
				add = false;
				p = p.substring(1);
			}
			if(!permsApi.setPermission(player, p, add)){
				getLogger().info("Failed to give perm: " + p);
			}
		}
	}
	
	public void broadcast(String msg, String player, double money){
		if(!broadcast) return;
		if(msg == null || msg.isEmpty()) return;
		msg = msg.replace("<player>", player).replace("<money>", numberFormat.format(money));
		msg = msg.replace("&", "�");
		Bukkit.broadcastMessage(title + msg);
	}
	
	public void tell(String msg, Player player, double money){
		if(msg == null || msg.isEmpty()) return;
		msg = msg.replace("<player>", player.getName()).replace("<money>", numberFormat.format(money));
		msg = msg.replace("&", "�");
		player.sendMessage(title + msg);
	}
	
	public double getPlayerItemPerk(Player player){
		if(player.hasPermission("rewardstime.perks.fivetimes.items")) return 5d;
		if(player.hasPermission("rewardstime.perks.fourtimes.items")) return 4d;
		if(player.hasPermission("rewardstime.perks.threetimes.items")) return 3d;
		if(player.hasPermission("rewardstime.perks.twopointfivetimes.items")) return 2.5;
		if(player.hasPermission("rewardstime.perks.twotimes.items")) return 2d;
		if(player.hasPermission("rewardstime.perks.onepointfivetimes.items")) return 1.5;
		return 1d;
	}
	
	public double getPlayerExpPerk(Player player){
		if(player.hasPermission("rewardstime.perks.fivetimes.exp")) return 5d;
		if(player.hasPermission("rewardstime.perks.fourtimes.exp")) return 4d;
		if(player.hasPermission("rewardstime.perks.threetimes.exp")) return 3d;
		if(player.hasPermission("rewardstime.perks.twopointfivetimes.exp")) return 2.5;
		if(player.hasPermission("rewardstime.perks.twotimes.exp")) return 2d;
		if(player.hasPermission("rewardstime.perks.onepointfivetimes.exp")) return 1.5;
		return 1d;
	}
	
	public double getPlayerMoneyPerk(Player player){
		if(player.hasPermission("rewardstime.perks.fivetimes.money")) return 5d;
		if(player.hasPermission("rewardstime.perks.fourtimes.money")) return 4d;
		if(player.hasPermission("rewardstime.perks.threetimes.money")) return 3d;
		if(player.hasPermission("rewardstime.perks.twopointfivetimes.money")) return 2.5;
		if(player.hasPermission("rewardstime.perks.twotimes.money")) return 2d;
		if(player.hasPermission("rewardstime.perks.onepointfivetimes.money")) return 1.5;
		return 1d;
	}
	
	public double getOfflinePlayerItemPerk(OfflinePlayer player){
		if(player.getPlayer() != null) return getPlayerItemPerk(player.getPlayer());
		if(!permsApi.supportsOfflinePlayers()) return 1;
		if(permsApi.checkPermission(player, "rewardstime.perks.fivetimes.items")) return 5d;
		if(permsApi.checkPermission(player, "rewardstime.perks.fourtimes.items")) return 4d;
		if(permsApi.checkPermission(player, "rewardstime.perks.threetimes.items")) return 3d;
		if(permsApi.checkPermission(player, "rewardstime.perks.twopointfivetimes.items")) return 2.5;
		if(permsApi.checkPermission(player, "rewardstime.perks.twotimes.items")) return 2d;
		if(permsApi.checkPermission(player, "rewardstime.perks.onepointfivetimes.items")) return 1.5;
		return 1d;
	}
	
	public double getOfflinePlayerExpPerk(OfflinePlayer player){
		if(player.getPlayer() != null) return getPlayerExpPerk(player.getPlayer());
		if(!permsApi.supportsOfflinePlayers()) return 1;
		if(permsApi.checkPermission(player, "rewardstime.perks.fivetimes.exp")) return 5d;
		if(permsApi.checkPermission(player, "rewardstime.perks.fourtimes.exp")) return 4d;
		if(permsApi.checkPermission(player, "rewardstime.perks.threetimes.exp")) return 3d;
		if(permsApi.checkPermission(player, "rewardstime.perks.twopointfivetimes.exp")) return 2.5;
		if(permsApi.checkPermission(player, "rewardstime.perks.twotimes.exp")) return 2d;
		if(permsApi.checkPermission(player, "rewardstime.perks.onepointfivetimes.exp")) return 1.5;
		return 1d;
	}
	
	public double getOfflinePlayerMoneyPerk(OfflinePlayer player){
		if(player.getPlayer() != null) return getPlayerMoneyPerk(player.getPlayer());
		if(!permsApi.supportsOfflinePlayers()) return 1;
		if(permsApi.checkPermission(player, "rewardstime.perks.fivetimes.money")) return 5d;
		if(permsApi.checkPermission(player, "rewardstime.perks.fourtimes.money")) return 4d;
		if(permsApi.checkPermission(player, "rewardstime.perks.threetimes.money")) return 3d;
		if(permsApi.checkPermission(player, "rewardstime.perks.twopointfivetimes.money")) return 2.5;
		if(permsApi.checkPermission(player, "rewardstime.perks.twotimes.money")) return 2d;
		if(permsApi.checkPermission(player, "rewardstime.perks.onepointfivetimes.money")) return 1.5;
		return 1d;
	}
	
	public void addSubGroup(OfflinePlayer player, String group){
		if(!permsApi.supportsSubGroups()) return;
		if(!permsApi.supportsOfflinePlayers()){
			if(player.getPlayer() == null) return;
			permsApi.addSubGroup(player.getPlayer(), group);
		} else permsApi.addSubGroup(player, group);
	}
	
	public void addRank(OfflinePlayer player, String rank){
		if(!permsApi.supportsRanks()) return;
		if(!permsApi.supportsOfflinePlayers()){
			if(player.getPlayer() == null) return;
			permsApi.addRank(player.getPlayer(), rank);
		} else permsApi.addRank(player, rank);
	}
	
	public void removeSubGroup(OfflinePlayer player, String group){
		if(!permsApi.supportsSubGroups()) return;
		if(!permsApi.supportsOfflinePlayers()){
			if(player.getPlayer() == null) return;
			permsApi.removeSubGroup(player.getPlayer(), group);
		} else permsApi.removeSubGroup(player, group);
	}
	
	public boolean rewardPlayer(String base, OfflinePlayer player, int mult){
		if(!player.isOnline()) return rewardOfflinePlayer(base, player, mult);
		if(player.getPlayer() != null) return rewardOnlinePlayer(base, player.getPlayer(), mult);
		return rewardOfflinePlayer(base, player, mult);
	}
	
	public void givePlayerSubGroups(OfflinePlayer player, String str){
		if(permsApi == null) return;
		if(str == null || str.isEmpty()) return;
		String[] gs = str.split(",");
		for(String s : gs){
			if(s.startsWith("-")){
				s = s.substring(1);
				removeSubGroup(player, s);
				continue;
			}
			if(s.startsWith("+")) s = s.substring(1);
			addSubGroup(player, s);
		}
	}
	
	public void givePlayerRank(OfflinePlayer player, String str){
		if(permsApi == null) return;
		if(str == null || str.isEmpty()) return;
		str = str.trim();
		addRank(player, str);
	}
	
	
	public boolean rewardOfflinePlayer(String base, OfflinePlayer player, int mult){
		if(permsApi.supportsOfflinePlayers()) rewardAdvancedOfflinePlayer(player, base, mult);
		else broadcast(config.getString(base + ".broadcast", ""), player.getName(), config.getDouble(base + ".money", 0) * mult);
		if(!config.contains(base)) return false;
		rewardBasicOfflinePlayer(player, base, mult);
		return true;
	}
	
	public void rewardBasicOfflinePlayer(OfflinePlayer player, String base, int mult) {
		double amount = config.getDouble(base + ".money", 0) * mult;
		if(amount > 0) economyDeposit(player, amount);
		else if(amount < 0) economyWithdraw(player, amount);
		
		
	}

	public void rewardAdvancedOfflinePlayer(OfflinePlayer player, String base, int mult){
		double amount = config.getDouble(base + ".money", 0) * mult;
		if(permsApi.checkPermission(player, "rewardstime.rewards.type.money")){
			amount *= getOfflinePlayerMoneyPerk(player);
			if(amount > 0) economyDeposit(player, amount);
			else if(amount < 0) economyWithdraw(player, amount);
		}
		
		if(permsApi.supportsSettingPermissions() && permsApi.checkPermission(player, "rewardstime.rewards.type.perms")){
			String perms = config.getString(base + ".perms", "");
			givePermissionsOffline(player, perms);
		}
		
		if(permsApi.checkPermission(player, "rewardstime.rewards.type.broadcast")){
			broadcast(config.getString(base + ".broadcast", ""), player.getName(), amount);
		}
		
		if(permsApi.supportsSubGroups() && permsApi.checkPermission(player, "rewardstime.rewards.type.subgroups")){
			givePlayerSubGroups(player, config.getString(base + ".subgroups"));
		}
		
		if(permsApi.supportsRanks() && permsApi.checkPermission(player, "rewardstime.rewards.type.ranks")){
			givePlayerRank(player, config.getString(base + ".rank"));
		}
		
	}
	
	public boolean rewardOnlinePlayer(String base, final Player player, int mult){
		if(!config.contains(base)) return false;
		double amount = config.getDouble(base + ".money", 0) * mult;
		if(player.hasPermission("rewardstime.rewards.type.money")){
			amount *= getPlayerMoneyPerk(player);
			if(amount > 0) playerReward(player, amount, 0, 0);
			else if(amount < 0) playerReward(player, 0, 0, amount);
		}
		
		if(permsApi.supportsSettingPermissions() && player.hasPermission( "rewardstime.rewards.type.perms")){
			String perms = config.getString(base + ".perms", "");
			givePermissions(player, perms);
		}
		
		if(player.hasPermission("rewardstime.rewards.type.broadcast")){
			broadcast(config.getString(base + ".broadcast", ""), player.getName(), amount);
		}
		
		if(permsApi.supportsSubGroups() && player.hasPermission("rewardstime.rewards.type.subgroups")){
			givePlayerSubGroups(player, config.getString(base + ".subgroups"));
		}
		
		if(permsApi.supportsRanks() && permsApi.checkPermission(player, "rewardstime.rewards.type.ranks")){
			givePlayerRank(player, config.getString(base + ".rank"));
		}
		
		if(player.hasPermission("rewardstime.rewards.type.items")){
			
			//Cannot give items immediately in craft/smelt events as dupe bug emerges
			final Collection<ItemStack> items = computeDrops(config.getString(base + ".items"), (int)(mult * getPlayerItemPerk(player)));
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
				@Override
				public void run() {
					givePlayerItems(player, items.toArray(new ItemStack[items.size()]));
				}
			}, 1);
			
		}
		
		if(player.hasPermission("rewardstime.rewards.type.exp")){
			int xp = config.getInt(base + ".exp", 0) * mult;
			player.giveExp((int) (xp * getPlayerExpPerk(player)));
		}
		
		if(player.hasPermission("rewardstime.rewards.type.tell")){
			String tell = config.getString(base + ".tell");
			tell(tell, player, amount);
		}
		
		return true;
	}
}
