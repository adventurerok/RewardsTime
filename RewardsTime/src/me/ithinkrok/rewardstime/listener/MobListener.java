package me.ithinkrok.rewardstime.listener;

import java.util.*;
import java.util.Map.Entry;

import me.ithinkrok.rewardstime.*;
import me.ithinkrok.rewardstime.RewardsTime.ArmorType;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class MobListener implements Listener {

	RewardsTime plugin;
	Random rand = new Random();

	public MobListener(RewardsTime plugin) {
		super();
		this.plugin = plugin;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onDamage(EntityDamageByEntityEvent event){
		if(event.isCancelled()) return;
		if(!plugin.mobRewards) return;
		if(!(event.getDamager() instanceof Player)) return;
		Player damager = (Player) event.getDamager();
		if(!plugin.enabledGameModes.get(damager.getGameMode())) return;
		plugin.entityDamage(event.getEntity(), damager, event.getDamage());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void changeDrops(EntityDeathEvent event){
		if(!plugin.mobRewards) return;
		String entName = event.getEntity().getType().toString().toLowerCase();
		String str = "mob." + entName;
		String dropsStr = plugin.config.getString(str + ".items");
		for(ItemStack item : plugin.computeDrops(dropsStr)){
			event.getDrops().add(item);
		}
		
		if(event.getDroppedExp() == 0) return;
		int exp = plugin.config.getInt(str + ".exp", 0);
		event.setDroppedExp(event.getDroppedExp() + exp);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onKill(EntityDeathEvent event){
		if(!plugin.mobRewards) return;
		DamageData damages = plugin.entityDamageData.remove(event.getEntity().getUniqueId());
		if(damages == null) return;
		String entName = event.getEntity().getType().toString().toLowerCase();
		double amount = plugin.config.getDouble("mob." + entName + ".money", 0);
		if(amount == 0) return;
		double amountStart = amount;
		if(amount > 0){
			if(plugin.mobArmorBonus){
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
			}
		}
		double gain = 0;
		double bonus = 0;
		double loss = 0;
		if(amountStart > 0){
			gain = amountStart;
			bonus = amount - amountStart;
		} else if(amountStart < 0){
			loss = - amountStart;
		}
		HashMap<UUID, Double> rewards = damages.getResult();
		for(Entry<UUID, Double> entry : rewards.entrySet()){
			double mult = entry.getValue();
			plugin.playerReward(Bukkit.getOfflinePlayer(entry.getKey()), gain * mult, bonus * mult, loss * mult);
		}
		
	}
	
}
