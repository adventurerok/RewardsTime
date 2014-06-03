package me.ithinkrok.rewardstime.groupmanager;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class GroupManagerImpl implements IGroupManager {

	GroupManager gm;
	
	public GroupManagerImpl() {
		super();
		gm = (GroupManager) Bukkit.getPluginManager().getPlugin("GroupManager");
		
	}
	
	@Override
	public void addSubGroup(Player player, String group) {
		OverloadedWorldHolder world = gm.getWorldsHolder().getWorldData(player);
		User user = world.getUser(player.getName());
		Group add = world.getGroup(group);
		if(add == null){
			Bukkit.getLogger().info("[RewardsTime] No group called \"" + group + "\" found");
			return;
		}
		user.addSubGroup(add);

	}

	@Override
	public void disable() {
		gm = null;
	}

}
