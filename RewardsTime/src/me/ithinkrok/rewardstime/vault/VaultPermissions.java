package me.ithinkrok.rewardstime.vault;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultPermissions implements IVaultPermissions {

	private Permission permission;

	public VaultPermissions() {
		setupPermissions();
	}
	
	@Override
	public boolean setPermission(Player player, String perm, boolean set){
		if(set){
			return permission.playerAdd(player, perm);
		} else {
			return permission.playerRemove(player, perm);
		}
	}

	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServer().getServicesManager()
				.getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}
		return (permission != null);
	}

}
