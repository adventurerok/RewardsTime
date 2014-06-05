package me.ithinkrok.rewardstime.permissions;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class DefaultPerms implements IPermissions {

	@Override
	public void addSubGroup(OfflinePlayer player, String group) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addSubGroup(Player player, String group) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeSubGroup(OfflinePlayer player, String group) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeSubGroup(Player player, String group) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean checkPermission(OfflinePlayer player, String permission) {
		if(!player.isOnline() || player.getPlayer() == null) throw new UnsupportedOperationException();
		return checkPermission(player.getPlayer(), permission);
	}

	@Override
	public boolean checkPermission(Player player, String permission) {
		return player.hasPermission(permission);
	}

	@Override
	public void disable() {
		
	}

	@Override
	public boolean supportsSubGroups() {
		return false;
	}

	@Override
	public boolean supportsOfflinePlayers() {
		return false;
	}

	@Override
	public boolean setPermission(OfflinePlayer player, String permission, boolean set) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setPermission(Player player, String permission, boolean set) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean enabled() {
		return true;
	}

	@Override
	public boolean supportsSettingPermissions() {
		return false;
	}

}
