package me.ithinkrok.rewardstime;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class RewardsTime extends JavaPlugin {
	
	public boolean log = true;
	public boolean mobRewards = true;
	public FileConfiguration config;

	public Economy economy = null;
	
	
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
}
