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
		if(!plugin.mobRewards) return;
		Player killer = event.getEntity().getKiller();
		if(killer == null) return;
		String entName = event.getEntity().getType().toString().toLowerCase();
		double amount = plugin.config.getDouble("mob." + entName + ".money", 0);
		if(amount == 0) killer.sendMessage("You get nothing for killing that");
		else if(amount > 0){
			plugin.economy.depositPlayer(killer, amount);
			killer.sendMessage("You recieve $" + amount);
		}
		else if(amount < 0){
			killer.sendMessage("You lose $" + (-amount));
			plugin.economy.withdrawPlayer(killer, -amount);
		}
	}
}
