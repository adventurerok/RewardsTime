package me.ithinkrok.rewardstime;

import me.ithinkrok.rewardstime.RewardsTime.ArmorType;

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
				RewardsBonus type = plugin.armorType.get(ArmorType.HELMET);
				RewardsBonus material = plugin.armorMaterial.get(plugin.getArmorMaterial(equip.getHelmet().getType()));
				amount = RewardsBonus.apply(amount, type.type, material.apply(type.amount));
			}
			if(equip.getChestplate() != null){
				RewardsBonus type = plugin.armorType.get(ArmorType.CHESTPLATE);
				RewardsBonus material = plugin.armorMaterial.get(plugin.getArmorMaterial(equip.getChestplate().getType()));
				amount = RewardsBonus.apply(amount, type.type, material.apply(type.amount));
			}
			if(equip.getLeggings() != null){
				RewardsBonus type = plugin.armorType.get(ArmorType.LEGGINGS);
				RewardsBonus material = plugin.armorMaterial.get(plugin.getArmorMaterial(equip.getLeggings().getType()));
				amount = RewardsBonus.apply(amount, type.type, material.apply(type.amount));
			}
			if(equip.getBoots() != null){
				RewardsBonus type = plugin.armorType.get(ArmorType.BOOTS);
				RewardsBonus material = plugin.armorMaterial.get(plugin.getArmorMaterial(equip.getBoots().getType()));
				amount = RewardsBonus.apply(amount, type.type, material.apply(type.amount));
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
