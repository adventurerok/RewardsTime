package me.ithinkrok.rewardstime;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class RewardsTime extends JavaPlugin {
	
	public boolean log = true;

	public Economy economy = null;
	
	
	@Override
	public void onEnable() {
		setupEconomy();
		if(log) getLogger().info("Loaded plugin RewardsTime version: " + getDescription().getVersion());
	}
	
	@Override
	public void onDisable() {
		economy = null;
		if(log) getLogger().info("Unloaded RewardsTime");
	}
	
	private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
}
