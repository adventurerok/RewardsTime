package me.ithinkrok.rewardstime.permissions;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class GroupManagerPerms implements IPermissions {

	GroupManager gm;
	
	public GroupManagerPerms() {
		super();
		gm = (GroupManager) Bukkit.getPluginManager().getPlugin("GroupManager");
		
	}
	
	@Override
	public void addSubGroup(OfflinePlayer player, String group) {
		OverloadedWorldHolder world = gm.getWorldsHolder().getDefaultWorld();
		User user = world.getUser(player.getName());
		Group add = world.getGroup(group);
		if(add == null){
			Bukkit.getLogger().info("[RewardsTime] No group called \"" + group + "\" found");
		}
		user.addSubGroup(add);

	}

	@Override
	public void disable() {
		gm = null;
	}

	@Override
	public void removeSubGroup(OfflinePlayer player, String group) {
		OverloadedWorldHolder world = gm.getWorldsHolder().getDefaultWorld();
		User user = world.getUser(player.getName());
		Group add = world.getGroup(group);
		if(add == null){
			Bukkit.getLogger().info("[RewardsTime] No group called \"" + group + "\" found");
		}
		user.removeSubGroup(add);
	}

	@Override
	public boolean checkPermission(OfflinePlayer player, String permission) {
		OverloadedWorldHolder world = gm.getWorldsHolder().getDefaultWorld();
		User user = world.getUser(player.getName());
		return gm.getWorldsHolder().getWorldPermissions(world.getName()).checkUserPermission(user, permission);
	}

	@Override
	public void addSubGroup(Player player, String group) {
		OverloadedWorldHolder world = gm.getWorldsHolder().getDefaultWorld();
		User user = world.getUser(player.getName());
		Group add = world.getGroup(group);
		if(add == null){
			Bukkit.getLogger().info("[RewardsTime] No group called \"" + group + "\" found");
		}
		user.addSubGroup(add);
	}

	@Override
	public void removeSubGroup(Player player, String group) {
		OverloadedWorldHolder world = gm.getWorldsHolder().getDefaultWorld();
		User user = world.getUser(player.getName());
		Group add = world.getGroup(group);
		if(add == null){
			Bukkit.getLogger().info("[RewardsTime] No group called \"" + group + "\" found");
		}
		user.removeSubGroup(add);
	}

	@Override
	public boolean checkPermission(Player player, String permission) {
		return player.hasPermission(permission);
	}

	@Override
	public boolean supportsSubGroups() {
		return true;
	}

	@Override
	public boolean supportsOfflinePlayers() {
		return true;
	}

	@Override
	public boolean setPermission(OfflinePlayer player, String permission, boolean set) {
		OverloadedWorldHolder world = gm.getWorldsHolder().getDefaultWorld();
		User user = world.getUser(player.getName());
		user.addPermission(permission);
		return true;
	}

	@Override
	public boolean setPermission(Player player, String permission, boolean set) {
		OverloadedWorldHolder world = gm.getWorldsHolder().getDefaultWorld();
		User user = world.getUser(player.getName());
		user.addPermission(permission);
		return true;
	}

	@Override
	public boolean enabled() {
		return gm != null && gm.isEnabled();
	}

	@Override
	public boolean supportsSettingPermissions() {
		return true;
	}

}
