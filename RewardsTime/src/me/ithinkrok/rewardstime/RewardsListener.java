package me.ithinkrok.rewardstime;

import me.ithinkrok.rewardstime.RewardsTime.ArmorType;

import org.bukkit.Material;
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
		double amountStart = amount;
		if(amount > 0){
			EntityEquipment equip = event.getEntity().getEquipment();
			if(equip.getHelmet().getType() != Material.AIR){
				RewardsBonus type = plugin.armorType.get(ArmorType.HELMET);
				RewardsBonus material = plugin.armorMaterial.get(plugin.getArmorMaterial(equip.getHelmet().getType()));
				amount = RewardsBonus.apply(amount, type.type, material.apply(type.amount));
			}
			if(equip.getChestplate().getType() != Material.AIR){
				RewardsBonus type = plugin.armorType.get(ArmorType.CHESTPLATE);
				RewardsBonus material = plugin.armorMaterial.get(plugin.getArmorMaterial(equip.getChestplate().getType()));
				amount = RewardsBonus.apply(amount, type.type, material.apply(type.amount));
			}
			if(equip.getLeggings().getType() != Material.AIR){
				RewardsBonus type = plugin.armorType.get(ArmorType.LEGGINGS);
				RewardsBonus material = plugin.armorMaterial.get(plugin.getArmorMaterial(equip.getLeggings().getType()));
				amount = RewardsBonus.apply(amount, type.type, material.apply(type.amount));
			}
			if(equip.getBoots().getType() != Material.AIR){
				RewardsBonus type = plugin.armorType.get(ArmorType.BOOTS);
				RewardsBonus material = plugin.armorMaterial.get(plugin.getArmorMaterial(equip.getBoots().getType()));
				amount = RewardsBonus.apply(amount, type.type, material.apply(type.amount));
			}
			plugin.economy.depositPlayer(killer, amount);
			if(amountStart == amount) killer.sendMessage("You recieve $" + amount);
			else killer.sendMessage("You recieve $" + amountStart + " + $" + (amount - amountStart) + " bonus");
		}
		else if(amount < 0){
			killer.sendMessage("You lose $" + (-amount));
			plugin.economy.withdrawPlayer(killer, -amount);
		}
	}
}
