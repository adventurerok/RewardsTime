package me.ithinkrok.rewardstime.permissions;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultPerms implements IPermissions {

	private Permission permission;

	public VaultPerms() {
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
	
	@Override
	public void addSubGroup(Player player, String group){
		permission.playerAddGroup(player, group);
	}

	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServer().getServicesManager()
				.getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}
		return (permission != null);
	}
	
	@Override
	public boolean enabled() {
		return permission != null;
	}

	@Override
	public void disable() {
		permission = null;
	}

	@Override
	public void addSubGroup(OfflinePlayer player, String group) {
		permission.playerAddGroup(Bukkit.getWorlds().get(0).getName(), player, group);
	}

	@Override
	public void removeSubGroup(OfflinePlayer player, String group) {
		permission.playerRemoveGroup(Bukkit.getWorlds().get(0).getName(), player, group);
		
	}

	@Override
	public void removeSubGroup(Player player, String group) {
		permission.playerRemoveGroup(player, group);
		
	}

	@Override
	public boolean checkPermission(OfflinePlayer player, String perm) {
		return permission.playerHas(Bukkit.getWorlds().get(0).getName(), player, perm);
	}

	@Override
	public boolean checkPermission(Player player, String perm) {
		return player.hasPermission(perm);
	}

	@Override
	public boolean supportsSubGroups() {
		return permission.hasGroupSupport();
	}

	@Override
	public boolean supportsOfflinePlayers() {
		return true;
	}

	@Override
	public boolean setPermission(OfflinePlayer player, String perm, boolean set) {
		if(set){
			return permission.playerAdd(Bukkit.getWorlds().get(0).getName(), player, perm);
		} else {
			return permission.playerRemove(Bukkit.getWorlds().get(0).getName(), player, perm);
		}
	}

	@Override
	public boolean supportsSettingPermissions() {
		return true;
	}

}
