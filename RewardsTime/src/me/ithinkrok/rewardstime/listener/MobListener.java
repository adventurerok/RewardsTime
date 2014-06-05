package me.ithinkrok.rewardstime.listener;

import java.util.*;
import java.util.Map.Entry;

import me.ithinkrok.rewardstime.*;
import me.ithinkrok.rewardstime.RewardsTime.ArmorType;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
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
		if(!damager.hasPermission("rewardstime.rewards")) return;
		if(!plugin.enabledGameModes.get(damager.getGameMode())) return;
		plugin.entityDamage(event.getEntity(), damager, event.getDamage());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void changeDrops(EntityDeathEvent event){
		if(!plugin.mobRewards) return;
		Player killer = event.getEntity().getKiller();
		String entName = event.getEntity().getType().toString().toLowerCase();
		String base = "mob." + entName;
		String dropsStr = plugin.config.getString(base + ".items");
		
		int add = 0;
		if(plugin.mobLootingBonus && killer != null){
			if(killer.getItemInHand() != null && killer.getItemInHand().getType() != Material.AIR){
				add = killer.getItemInHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
			}
		}
		
		int dMult = 1;
		if(killer != null) dMult = (int) plugin.getPlayerItemPerk(killer);
		for(ItemStack item : plugin.computeDrops(dropsStr, dMult, add)){
			event.getDrops().add(item);
		}
		
		if(plugin.config.contains(base + ".exp")){
			int exp = plugin.config.getInt(base + ".exp", 0);
			if(killer != null) exp *= plugin.getPlayerExpPerk(killer);
			event.setDroppedExp(exp);
		} else {
			int exp = event.getDroppedExp();
			if(killer != null) exp *= plugin.getPlayerExpPerk(killer);
			event.setDroppedExp(exp);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onKill(EntityDeathEvent event){
		if(!plugin.mobRewards) return;
		DamageData damages = plugin.entityDamageData.remove(event.getEntity().getUniqueId());
		if(damages == null) return;
		String entName = event.getEntity().getType().toString().toLowerCase();
		String base = "mob." + entName;
		double amount = plugin.config.getDouble(base + ".money", 0);
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
		
		String perms = plugin.config.getString(base + ".perms");
		
		String tell = plugin.config.getString(base + ".tell");
		
		HashMap<UUID, Double> rewards = damages.getResult();
		ArrayList<String> killers = new ArrayList<>();
		for(Entry<UUID, Double> entry : rewards.entrySet()){
			OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.getKey());
			Player player = Bukkit.getPlayer(entry.getKey());
			double mult = entry.getValue();
			if(player != null) mult *= plugin.getPlayerMoneyPerk(player);
			if(amount != 0) plugin.playerReward(offline, gain * mult, bonus * mult, loss * mult);
			
			if(player == null) continue;
			if(!player.hasPermission("rewardstime.rewards.from.mobs")) continue;
			if(player.hasPermission("rewardstime.rewards.type.tell")) plugin.tell(tell, player, amount * mult);
			if(player.hasPermission("rewardstime.rewards.type.perms")) plugin.givePermissions(player, perms);
			

			if(player.hasPermission("rewardstime.rewards.type.subgroups")){
				plugin.givePlayerSubGroups(player, plugin.config.getString(base + ".subgroups"));
			}
		}
		
		
		StringBuilder killString = new StringBuilder();
		for(int d = 0; d < killers.size(); ++d){
			if(d == 0) killString.append(killers.get(d));
			else if(d == killers.size() - 1) killString.append(" and ").append(killers.get(d));
			else killString.append(", ").append(killers.get(d));
		}
		
		String bc = plugin.config.getString("mob." + entName + ".broadcast");
		plugin.broadcast(bc, killString.toString(), amount);
		
	}
	
}
