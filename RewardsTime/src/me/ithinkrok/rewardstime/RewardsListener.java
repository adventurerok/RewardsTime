package me.ithinkrok.rewardstime;

import java.util.HashMap;
import java.util.UUID;
import java.util.Map.Entry;

import me.ithinkrok.rewardstime.RewardsTime.ArmorType;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.EntityEquipment;

public class RewardsListener implements Listener {

	RewardsTime plugin;
	
	public RewardsListener(RewardsTime plugin) {
		super();
		this.plugin = plugin;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onDamage(EntityDamageByEntityEvent event){
		if(!plugin.mobRewards) return;
		if(!(event.getDamager() instanceof Player)) return;
		Player damager = (Player) event.getDamager();
		if(!plugin.enabledGameModes.get(damager.getGameMode())) return;
		plugin.entityDamage(event.getEntity(), damager, event.getDamage());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onKill(EntityDeathEvent event){
		if(!plugin.mobRewards) return;
		DamageData damages = plugin.entityDamageData.get(event.getEntity().getUniqueId());
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
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onCraftItem(CraftItemEvent event){
		if(!plugin.craftRewards) return;
		if(!(event.getWhoClicked() instanceof Player)) return;
		String item = event.getCurrentItem().getType().toString().toLowerCase();
		Player player = (Player) event.getWhoClicked();
		if(!plugin.enabledGameModes.get(player.getGameMode())) return;
		double amount = plugin.config.getDouble("craft." + item + ".money", 0);
		String withMeta = "craft." + item + "/" + event.getCurrentItem().getDurability() +".money";
		if(plugin.config.contains(withMeta)){
			amount = plugin.config.getDouble(withMeta);
		}
		amount *= event.getCurrentItem().getAmount();
		if(amount == 0) return;
		if(amount > 0){
			plugin.playerReward(player, amount, 0, 0);
		} else if(amount < 0){
			plugin.playerReward(player, 0, 0, -amount);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onSmeltItem(FurnaceExtractEvent event){
		if(!plugin.smeltRewards) return;
		if(!plugin.enabledGameModes.get(event.getPlayer().getGameMode())) return;
		String item = event.getItemType().toString().toLowerCase();
		Player player = event.getPlayer();
		double amount = plugin.config.getDouble("smelt." + item + ".money", 0);
//		String withMeta = "smelt." + item + "/" + event.get +".money"; //(cannot get item meta)
//		if(plugin.config.contains(withMeta)){
//			amount = plugin.config.getDouble(withMeta);
//		}
		amount *= event.getItemAmount();
		if(amount == 0) return;
		if(amount > 0){
			plugin.playerReward(player, amount, 0, 0);
		} else if(amount < 0){
			plugin.playerReward(player, 0, 0, -amount);
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.MONITOR)
	public void onMineBlock(BlockBreakEvent event){
		if(!plugin.blockRewards) return;
		if(event.getPlayer() == null) return;
		if(!plugin.enabledGameModes.get(event.getPlayer().getGameMode())) return;
		String item = event.getBlock().getType().toString().toLowerCase();
		int data = event.getBlock().getData();
		double amount = plugin.config.getDouble("block." + item + ".money", 0);
		String withMeta = "block." + item + "/" + data + ".money";
		if(plugin.config.contains(withMeta)){
			amount = plugin.config.getDouble(withMeta);
		}
		if(amount == 0) return;
		if(amount > 0){
			plugin.playerReward(event.getPlayer(), amount, 0, 0);
		} else if(amount < 0){
			plugin.playerReward(event.getPlayer(), 0, 0, -amount);
		}
	}
}
