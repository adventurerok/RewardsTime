package me.ithinkrok.rewardstime;

import java.io.*;
import java.util.EnumMap;
import java.util.HashMap;

import me.ithinkrok.rewardstime.RewardsBonus.BonusType;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class RewardsTime extends JavaPlugin {
	
	public static enum ArmorMaterial {
		DIAMOND,
		GOLD,
		CHAINMAIL,
		IRON,
		LEATHER,
		OTHER
	}
	
	public static enum ArmorType {
		HELMET,
		CHESTPLATE,
		LEGGINGS,
		BOOTS,
		OTHER
	}
	
	public static enum FieldType {
		BOOLEAN,
		INTEGER,
		DOUBLE,
		STRING,
		BONUSTYPE
	}
	
	public boolean log = true;
	public boolean mobRewards = true;
	public boolean craftRewards = true;
	public boolean smeltRewards = true;
	public boolean mobArmorBonus = true;
	public boolean blockRewards = true;
	public boolean voteRewards = true;
	public boolean rewardCreative = false;
	public FileConfiguration config;

	public Economy economy = null;
	
	public EnumMap<ArmorMaterial, RewardsBonus> armorMaterial = new EnumMap<>(ArmorMaterial.class);
	public EnumMap<ArmorType, RewardsBonus> armorType = new EnumMap<>(ArmorType.class);
	
	
	public HashMap<String, FieldType> fieldTypes = new HashMap<>();
	
	@Override
	public void onEnable() {
		fieldTypes.put("money", FieldType.DOUBLE);
		fieldTypes.put("bonus", FieldType.DOUBLE);
		fieldTypes.put("type", FieldType.BONUSTYPE);
		File conFile = new File(getDataFolder(), "config.yml");
		if(!conFile.exists()){ //Cannot use bukkit default config feature as causes unintended side effects
			try{
				InputStream in = RewardsTime.class.getClassLoader().getResourceAsStream("configdefault.yml");
				FileOutputStream out = new FileOutputStream(conFile);
				int b = -1;
				while((b = in.read()) != -1){
					out.write(b);
				}
				in.close();
				out.close();
				getLogger().info("Created default config");
			} catch(IOException e){
				getLogger().warning("Failed to copy default config");
				getLogger().warning(e.getMessage());
			}
			reloadConfig();
		}
		setupEconomy();
		loadConfigValues();
		getServer().getPluginManager().registerEvents(new RewardsListener(this), this);
	}
	
	@Override
	public void onDisable() {
		economy = null;
	}
	
	private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
	
	public Object getFieldValue(CommandSender sender, FieldType type, String parse){
		switch(type){
		case DOUBLE:
			try {
				return Double.parseDouble(parse);
			} catch(NumberFormatException e){
				sender.sendMessage("Field must be a number");
				return null;
			}
		case INTEGER:
			try{
				return Integer.parseInt(parse);
			} catch(NumberFormatException e){
				sender.sendMessage("Field must be an integer");
			}
		case STRING:
			return parse;
		case BOOLEAN:
			if(!parse.equalsIgnoreCase("true") && !parse.equalsIgnoreCase("false")){
				sender.sendMessage("Field must be a boolean (true/false)");
				return null;
			} else return Boolean.parseBoolean(parse);
		case BONUSTYPE:
			BonusType bonus = null;
			try{
				bonus = BonusType.valueOf(parse.toUpperCase());
			} catch(IllegalArgumentException e){}
			if(bonus == null){
				sender.sendMessage("Field must be a BonusType (multiply/add)");
			}
			return bonus;
		default:
			return null;
		}
	}
	
	public void loadConfigValues() {
		config = getConfig();
		mobRewards = config.getBoolean("mobrewards", true);
		craftRewards = config.getBoolean("craftrewards", true);
		smeltRewards = config.getBoolean("smeltrewards", true);
		blockRewards = config.getBoolean("blockrewards", true);
		voteRewards = config.getBoolean("voterewards", true);
		mobArmorBonus = config.getBoolean("mobarmorbonus", true);
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
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if(command.getName().equalsIgnoreCase("rewardstime")){
			if(args.length < 1){
				sender.sendMessage("RewardsTime commands: ");
				sender.sendMessage("- /rewardstime reload : Reloads the config");
				sender.sendMessage("- /rewardstime field <type> <item/mob name> <field> [newvalue]");
				sender.sendMessage("- /rewardstime config <field> [newvalue]");
				return true;
			} else if("reload".equalsIgnoreCase(args[0])) {
				reloadConfig();
				loadConfigValues();
				sender.sendMessage("Config reloaded successfully!");
				return true;
			} else if("field".equalsIgnoreCase(args[0])){
				if(args.length < 4) {
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
				if(!check(sender, type, name, field)) return true;
				String str = type + "." + name.toLowerCase() + "." + field;
				if(args.length < 5){
					if(!config.contains(str)){
						sender.sendMessage("No value is set for " + str);
					} else {
						sender.sendMessage(str + " is set to " + config.get(str));
					}
				} else {
					Object val = getFieldValue(sender, fieldTypes.get(field), args[4]);
					if(val == null) return true;
					config.set(str, val);
					sender.sendMessage(str + " set to " + val);
					saveConfig();
					loadConfigValues();
				}
				return true;
			} else if("config".equalsIgnoreCase(args[0])){
				if(args.length < 2){
					sender.sendMessage("Usage: /rewardstime config <field> [newvalue]");
					sender.sendMessage(" - <field>: The field to get/set");
					sender.sendMessage(" - [newvalue]: The value to set the field to");
					return true;
				}
				String field = args[1];
				if(args.length < 3){
					if(!config.contains(field)){
						sender.sendMessage("No value is set for " + field);
					} else {
						sender.sendMessage(field + " is set to " + config.get(field));
					}
				} else {
					config.set(field, args[2]);
					sender.sendMessage(field + " set to " + args[2]);
					saveConfig();
					loadConfigValues();
				}
				return true;
			}
			return false;
		}
		return false;
	}
	
	public boolean check(CommandSender sender, String type, String name, String field){
		String[] nameParts = name.split("/");
		if(nameParts.length > 2){
			sender.sendMessage("Only one metadata slash is allowed");
			return true;
		}
		
		boolean isBonus = false;
		
		switch(type) {
		case "craft":
		case "smelt":
		case "block":
			if(Material.getMaterial(nameParts[0].toUpperCase()) == null){
				sender.sendMessage("Unknown item: " + nameParts[0]);
				return false;
			}
			break;
		case "mob":
			EntityType t = null;
			try{
				t = EntityType.valueOf(nameParts[0].toUpperCase());
			} catch(IllegalArgumentException e){}
			if(t == null){
				sender.sendMessage("Unknown entity: " + nameParts[0]);
				return false;
			}
			break;
		case "mobarmor.material":
			ArmorMaterial mat = null;
			try{
				mat = ArmorMaterial.valueOf(name.toUpperCase());
			} catch(IllegalArgumentException e){}
			if(mat == null){
				sender.sendMessage("Unknown armor material: " + name);
				return false;
			}
			isBonus = true;
			break;
		case "mobarmor.type":
			ArmorType part = null;
			try{
				part = ArmorType.valueOf(name.toUpperCase());
			} catch(IllegalArgumentException e){}
			if(part == null){
				sender.sendMessage("Unknown armor type: " + name);
				return false;
			}
			isBonus = true;
			break;
		default:
			sender.sendMessage("Unknown type: " + type + ", types are: [craft, smelt, block, mob]");
			return false;
		}
		if(isBonus){
			if(!field.equals("type") && !field.equals("bonus")){
				sender.sendMessage("Unknown bonus field: " + field + ", fields are [type, bonus]");
				return false;
			}
		} else {
			if(!field.equals("money")){
				sender.sendMessage("Unknown field: " + field + ", fields are: [money]");
				return false;
			}
		}
		return true;
	}
	
	public RewardsBonus loadBonus(String base){
		double amount = config.getDouble(base + ".bonus", 0);
		String ty = config.getString(base + ".type", "none").toUpperCase();
		BonusType type = BonusType.NONE;
		try{
			type = BonusType.valueOf(ty);
		} catch(IllegalArgumentException e){}
		return new RewardsBonus(type, amount);
	}
	
	public ArmorMaterial getArmorMaterial(Material mat){
		switch(mat){
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
	
	public ArmorType getArmorType(Material mat){
		switch(mat){
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
}
