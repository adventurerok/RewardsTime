package me.ithinkrok.rewardstime.vault;

import org.bukkit.entity.Player;

public interface IVaultPermissions {

	public boolean setPermission(Player player, String perm, boolean set);
	
	public boolean enabled();
	
	public void addSubGroup(Player player, String group);
	
	public void disable();
	
}
