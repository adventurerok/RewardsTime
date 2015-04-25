package me.ithinkrok.rewardstime.vault;

import org.bukkit.OfflinePlayer;

public interface IVaultEconomy {

	public void deposit(OfflinePlayer player, double amount);
	public void withdraw(OfflinePlayer player, double amount);
	
	public void disable();
	
	public boolean enabled();
	
}
