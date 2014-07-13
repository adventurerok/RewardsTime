package me.ithinkrok.rewardstime.permissions;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public interface IPermissions {

	public void addRank(OfflinePlayer player, String rank);
	public void addRank(Player player, String rank);
	public void addSubGroup(OfflinePlayer player, String group);
	public void addSubGroup(Player player, String group);
	public void removeSubGroup(OfflinePlayer player, String group);
	public void removeSubGroup(Player player, String group);
	public boolean checkPermission(OfflinePlayer player, String permission);
	public boolean checkPermission(Player player, String permission);
	public boolean setPermission(OfflinePlayer player, String permission, boolean set);
	public boolean setPermission(Player player, String permission, boolean set);
	public boolean enabled();
	public void disable();
	public boolean supportsRanks();
	public boolean supportsSubGroups();
	public boolean supportsOfflinePlayers();
	public boolean supportsSettingPermissions();
}
