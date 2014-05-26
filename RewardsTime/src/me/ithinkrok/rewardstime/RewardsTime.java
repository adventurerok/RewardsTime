package me.ithinkrok.rewardstime;

import java.util.EnumMap;

import me.ithinkrok.rewardstime.RewardsBonus.BonusType;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
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
	
	public boolean log = true;
	public boolean mobRewards = true;
	public FileConfiguration config;

	public Economy economy = null;
	
	public EnumMap<ArmorMaterial, RewardsBonus> armorMaterial = new EnumMap<>(ArmorMaterial.class);
	public EnumMap<ArmorType, RewardsBonus> armorType = new EnumMap<>(ArmorType.class);
	
	
	@Override
	public void onEnable() {
		config = getConfig();
		config.options().copyDefaults(true);
		saveConfig();
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
	
	public void loadConfigValues() {
		config = getConfig();
		mobRewards = config.getBoolean("mobrewards", true);
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
				return true;
			} else if("reload".equalsIgnoreCase(args[0])) {
				reloadConfig();
				loadConfigValues();
				sender.sendMessage("Config reloaded successfully!");
				return true;
			}
			return false;
		}
		return false;
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
