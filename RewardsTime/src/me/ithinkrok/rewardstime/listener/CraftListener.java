package me.ithinkrok.rewardstime.listener;

import java.util.Collection;

import me.ithinkrok.rewardstime.RewardsTime;

import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.ItemStack;

public class CraftListener implements Listener {

	RewardsTime plugin;

	public CraftListener(RewardsTime plugin) {
		super();
		this.plugin = plugin;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onCraftItem(CraftItemEvent event){
		if(event.isCancelled()) return;
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
		
		String dropsStr = plugin.config.getString("craft." + item + ".items");
		Collection<ItemStack> result = plugin.computeDrops(dropsStr);
		plugin.givePlayerItems(player, result.toArray(new ItemStack[result.size()]));
		
		player.giveExp(plugin.getConfig().getInt("craft." + item + ".exp", 0));
		
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
		
		String dropsStr = plugin.config.getString("smelt." + item + ".items");
		Collection<ItemStack> result = plugin.computeDrops(dropsStr);
		plugin.givePlayerItems(player, result.toArray(new ItemStack[result.size()]));
		
		player.giveExp(plugin.getConfig().getInt("smelt." + item + ".exp", 0));
		
		if(amount == 0) return;
		if(amount > 0){
			plugin.playerReward(player, amount, 0, 0);
		} else if(amount < 0){
			plugin.playerReward(player, 0, 0, -amount);
		}
		
	}
	
}
