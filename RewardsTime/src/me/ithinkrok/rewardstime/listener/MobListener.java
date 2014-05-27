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
		String dropsStr = plugin.config.getString(str + ".drops");
		if(dropsStr == null || dropsStr.isEmpty()) return;
		String dropParts[] = dropsStr.split(",");
		for(String drop : dropParts){
			String sections[] = drop.split("/");
			if(sections.length != 4) continue;
			try{
				Material mat = Material.getMaterial(sections[0].toUpperCase());
				if(mat == null || mat == Material.AIR) continue;
				int metadata = 0;
				if(sections[1] != null && !sections[1].isEmpty()) metadata = Integer.parseInt(sections[1]);
				int maxamount = Integer.parseInt(sections[2]);
				double percent = Double.parseDouble(sections[3]) / 100d;
				if(percent > 1) percent = 1;
				else if(percent <= 0) continue;
				int amount = 0;
				for(int d = 0; d < maxamount; ++d){
					if(rand.nextDouble() < percent) ++amount;
				}
				if(amount == 0) continue;
				while(amount > 0){
					event.getDrops().add(new ItemStack(mat, Math.min(amount, mat.getMaxStackSize()), (short)metadata));
					amount -= mat.getMaxStackSize();
				} 
			} catch(NumberFormatException e){}
		}
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
