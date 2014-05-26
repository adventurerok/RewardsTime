package me.ithinkrok.rewardstime;

import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDeathEvent;

public class RewardsListener implements Listener {

	RewardsTime plugin;
	
	public RewardsListener(RewardsTime plugin) {
		super();
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onKill(EntityDeathEvent event){
		Player killer = event.getEntity().getKiller();
		if(killer == null) return;
		plugin.economy.depositPlayer(killer, 3);
	}
}
