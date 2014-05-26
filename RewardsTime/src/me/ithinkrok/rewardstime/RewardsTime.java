package me.ithinkrok.rewardstime;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import me.ithinkrok.rewardstime.RewardsBonus.BonusType;
import me.ithinkrok.rewardstime.vault.IVaultEconomy;
import me.ithinkrok.rewardstime.vault.VaultEconomy;
import me.ithinkrok.rewardstime.votifier.VotifierApi;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
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
	
	public IVaultEconomy vaultApi = null;

	public String title = ChatColor.DARK_PURPLE + "[RewardsTime]" + ChatColor.WHITE + " ";
	public String fieldColor = ChatColor.RED.toString();
	public String valColor = ChatColor.BLUE.toString();
	public String nameColor = ChatColor.GREEN.toString();
	public String typeColor = ChatColor.GOLD.toString();
	public String white = ChatColor.WHITE.toString();

	public boolean scheduled = false;
	public int scheduleTime = 300;
	public int damageTimeout = 60;
	public int voteSaveMinutes = 15;

	public HashMap<UUID, RewardsData> fiveChange = new HashMap<>();
	
	public HashMap<UUID, DamageData> entityDamageData = new HashMap<>();
	
	public HashMap<UUID, Integer> voteCounts = new HashMap<>();
	
	public ArrayList<Integer> voteEveryList = new ArrayList<>();

	public boolean log = true;
	public boolean mobRewards = true;
	public boolean craftRewards = true;
	public boolean smeltRewards = true;
	public boolean mobArmorBonus = true;
	public boolean blockRewards = true;
	public boolean voteRewards = true;
	public boolean rewardCreative = false;
	public FileConfiguration config;

	public EnumMap<GameMode, Boolean> enabledGameModes = new EnumMap<>(GameMode.class);

	

	public EnumMap<ArmorMaterial, RewardsBonus> armorMaterial = new EnumMap<>(ArmorMaterial.class);
	public EnumMap<ArmorType, RewardsBonus> armorType = new EnumMap<>(ArmorType.class);

	public HashMap<String, FieldType> fieldTypes = new HashMap<>();

	@Override
	public void onEnable() {
		fieldTypes.put("money", FieldType.DOUBLE);
		fieldTypes.put("bonus", FieldType.DOUBLE);
		fieldTypes.put("type", FieldType.BONUSTYPE);
		File conFile = new File(getDataFolder(), "config.yml");
		if (!conFile.exists()) { // Cannot use bukkit default config feature as
									// causes unintended side effects
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
		if(Bukkit.getPluginManager().isPluginEnabled("Vault")){
			vaultApi = new VaultEconomy();
		}
		if(Bukkit.getPluginManager().isPluginEnabled("Votifier")){
			new VotifierApi().createListener(this);
		}
		loadConfigValues();
		getServer().getPluginManager().registerEvents(new RewardsListener(this), this);
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
	}
	
	public void checkDamageTimeout(){
		long time = System.nanoTime();
		HashMap<UUID, DamageData> result = new HashMap<>();
		for(Entry<UUID, DamageData> entry : entityDamageData.entrySet()){
			double diff = time - entry.getValue().lastDamage;
			diff /= 1_000_000_000d;
			if(diff <= damageTimeout){
				result.put(entry.getKey(), entry.getValue());
			}
		}
		entityDamageData.clear();
		entityDamageData = result;
	}

	@Override
	public void onDisable() {
		if(vaultApi != null) vaultApi.disable();
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
		rewardCreative = config.getBoolean("rewardcreative", false);
		armorMaterial.put(ArmorMaterial.DIAMOND, loadBonus("mobarmor.material.diamond"));
		armorMaterial.put(ArmorMaterial.IRON, loadBonus("mobarmor.material.iron"));
		armorMaterial.put(ArmorMaterial.GOLD, loadBonus("mobarmor.material.gold"));
		armorMaterial.put(ArmorMaterial.LEATHER, loadBonus("mobarmor.material.leather"));
		armorMaterial.put(ArmorMaterial.CHAINMAIL, loadBonus("mobarmor.material.chainmail"));
		armorMaterial.put(ArmorMaterial.OTHER, loadBonus("mobarmor.material.other"));
		armorType.put(ArmorType.HELMET, loadBonus("mobarmor.type.helmet"));
		armorType.put(ArmorType.CHESTPLATE, loadBonus("mobarmor.type.chestplate"));
		armorType.put(ArmorType.LEGGINGS, loadBonus("mobarmor.type.leggings"));
		armorType.put(ArmorType.BOOTS, loadBonus("mobarmor.type.boots"));
		armorType.put(ArmorType.OTHER, loadBonus("mobarmor.type.other"));

		enabledGameModes.put(GameMode.SURVIVAL, config.getBoolean("gamemodes.survival", true));
		enabledGameModes.put(GameMode.CREATIVE, config.getBoolean("gamemodes.creative", false));
		enabledGameModes.put(GameMode.ADVENTURE, config.getBoolean("gamemodes.adventure", true));

		scheduleTime = config.getInt("rewardtime", 500);
		damageTimeout = config.getInt("damagetimeout", 60);
		voteSaveMinutes = config.getInt("votesaveminutes", 15);
		
		for(int d = 0; d < 250; ++d){
			if(config.contains("votes.every." + d + ".money")){
				voteEveryList.add(d);
			}
		}
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
		if(vaultApi == null) return;
		double total = gain + bonus - loss;
		if (total > 0)
			economyDeposit(player, total);
		else if (total < 0)
			economyWithdraw(player, -total);
		if(!player.isOnline()) return;
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
	
	public void entityDamage(Entity entity, Player player, double amount){
		DamageData data = entityDamageData.get(entity.getUniqueId());
		if(data == null){
			data = new DamageData();
			entityDamageData.put(entity.getUniqueId(), data);
		}
		data.damageFrom(player.getUniqueId(), amount);
	}
	
	public boolean hasEntity(Entity entity){
		return entityDamageData.get(entity.getUniqueId()) != null;
	}

	public void alertRewards() {
		for (Entry<UUID, RewardsData> entry : fiveChange.entrySet()) {
			Player player = Bukkit.getPlayer(entry.getKey());
			if (player == null)
				continue;
			RewardsData data = entry.getValue();
			String message = title + "Over the last " + timeString(scheduleTime) + ", you gained " + valColor + "$"
					+ data.gained + white + " (+" + valColor + "$" + data.gainedBonus + white + " bonus) and lost "
					+ valColor + "$" + data.lost + white + " (Total: " + valColor + "$" + data.getTotal() + white + ")";
			player.sendMessage(message);
		}
		fiveChange.clear();
		scheduled = false;
	}

	public String timeString(int seconds) {
		if (seconds % 60 == 0) {
			return ChatColor.RED + "" + (seconds / 60) + white + " minutes";
		} else
			return ChatColor.RED + "" + (seconds / 60) + white + "m" + ChatColor.RED + "" + (seconds % 60) + white
					+ "s";
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("rewardstime")) {
			if (args.length < 1) {
				sender.sendMessage("RewardsTime commands: ");
				sender.sendMessage("- /rewardstime reload : Reloads the config");
				sender.sendMessage("- /rewardstime field <type> <item/mob name> <field> [newvalue]");
				sender.sendMessage("- /rewardstime config <field> [newvalue]");
				return true;
			} else if ("reload".equalsIgnoreCase(args[0])) {
				reloadConfig();
				loadConfigValues();
				sender.sendMessage(title + "Config reloaded successfully!");
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
				if (!check(sender, type, name, field))
					return true;
				String str = type + "." + name.toLowerCase() + "." + field;
				String strChat = typeColor + type + white + "." + nameColor + name.toLowerCase() + white + "."
						+ fieldColor + field;
				if (args.length < 5) {
					if (!config.contains(str)) {
						sender.sendMessage(title + "No value is set for " + strChat);
					} else {
						sender.sendMessage(title + strChat + white + " is set to " + valColor + config.get(str));
					}
				} else {
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
					if (!config.contains(field)) {
						sender.sendMessage(title + "No value is set for " + fieldColor + field);
					} else {
						sender.sendMessage(title + fieldColor + field + white + " is set to " + valColor
								+ config.get(field));
					}
				} else {
					config.set(field, parseObject(args[2]));
					sender.sendMessage(title + fieldColor + field + white + " set to " + valColor + args[2]);
					saveConfig();
					loadConfigValues();
				}
				return true;
			}
			return false;
		}
		return false;
	}

	public boolean check(CommandSender sender, String type, String name, String field) {
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
			if (!field.equals("money")) {
				sender.sendMessage(title + "Unknown field: " + fieldColor + field + white + ", fields are: [money]");
				return false;
			}
		}
		return true;
	}
	
	public void economyWithdraw(OfflinePlayer player, double amount){
		if(vaultApi != null) vaultApi.withdraw(player, amount);
	}
	
	public void economyDeposit(OfflinePlayer player, double amount){
		if(vaultApi != null) vaultApi.deposit(player, amount);
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
	
	public void incrementVoteCount(UUID voter){
		Integer count = voteCounts.get(voter);
		if(count == null) count = 0;
		voteCounts.put(voter, count++);
	}
	
	public int getVotes(UUID voter){
		Integer count = voteCounts.get(voter);
		if(count == null) count = 0;
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
	
	public void saveVoteCounts(){
		int num = voteCounts.size();
		File file = new File(getDataFolder(), "votes.bin");
		try{
			DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
			out.writeInt(num);
			for(Entry<UUID, Integer> entry : voteCounts.entrySet()){
				out.writeLong(entry.getKey().getMostSignificantBits());
				out.writeLong(entry.getKey().getLeastSignificantBits());
				out.writeInt(entry.getValue());
			}
			out.close();
		} catch(IOException e){
			getLogger().warning("Failed to save vote counts:");
			getLogger().warning(e.getMessage());
		}
	}
	
	public void loadVoteCounts(){
		File file = new File(getDataFolder(), "votes.bin");
		if(!file.exists()) return;
		try{
			DataInputStream in = new DataInputStream(new FileInputStream(file));
			int num = in.readInt();
			for(int d = 0; d < num; ++d){
				long most = in.readLong();
				long least = in.readLong();
				UUID id = new UUID(most, least);
				int votes = in.readInt();
				voteCounts.put(id, votes);
			}
			in.close();
		} catch(IOException e){
			getLogger().warning("Failed to load vote counts:");
			getLogger().warning(e.getMessage());
		}
	}
}
