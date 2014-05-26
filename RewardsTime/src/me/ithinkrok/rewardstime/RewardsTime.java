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
}
