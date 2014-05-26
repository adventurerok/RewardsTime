package me.ithinkrok.rewardstime;

import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;

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
			EntityEquipment equip = event.getEntity().getEquipment();
			if(equip.getHelmet() != null){
				double amt = plugin.config.getDouble("mobarmor.type.helmet.bonus", 0);
				String hName = equip.getHelmet().getType().toString();
				hName = hName.replace("_HELMET", "").toLowerCase();
				double mat = plugin.config.getDouble("mobarmor.material." + hName + ".bonus", 0);
				String type = plugin.config.getString("mobarmor.material." + hName + ".type", "multiply");
				if(type.equals("multiply")) amt *= mat;
				else if(type.equals("add")) amt += mat;
				type = plugin.config.getString("mobarmor.type.helmet.type", "add");
				if(type.equals("multiply")) amount *= amt;
				else if(type.equals("add")) amount += amt;
			}
			plugin.economy.depositPlayer(killer, amount);
			killer.sendMessage("You recieve $" + amount);
		}
		else if(amount < 0){
			killer.sendMessage("You lose $" + (-amount));
			plugin.economy.withdrawPlayer(killer, -amount);
		}
	}
}
