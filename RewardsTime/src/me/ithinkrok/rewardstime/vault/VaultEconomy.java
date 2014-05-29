package me.ithinkrok.rewardstime.vault;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomy implements IVaultEconomy{

	public Economy economy = null;
	
	public VaultEconomy() {
		setupEconomy();
	}
	
	@Override
	public void deposit(OfflinePlayer player, double amount) {
		economy.depositPlayer(player, amount);
	}

	@Override
	public void withdraw(OfflinePlayer player, double amount) {
		economy.withdrawPlayer(player, amount);
	}
	
	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(
				net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}

		return (economy != null);
	}

	@Override
	public void disable() {
		economy = null;
		
	}

	@Override
	public boolean enabled() {
		return economy != null;
	}

}
