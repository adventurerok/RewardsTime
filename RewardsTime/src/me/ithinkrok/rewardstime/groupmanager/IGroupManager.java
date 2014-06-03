package me.ithinkrok.rewardstime.groupmanager;

import org.bukkit.entity.Player;

public interface IGroupManager {

	public void addSubGroup(Player player, String group);
	public void disable();
	
}
