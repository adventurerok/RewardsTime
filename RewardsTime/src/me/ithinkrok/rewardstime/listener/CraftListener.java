package me.ithinkrok.rewardstime.listener;

import java.util.Collection;

import me.ithinkrok.rewardstime.RewardsTime;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.inventory.InventoryType.SlotType;
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
		
		//event.getCurrentItem().getAmount() is broken as it returns number of items shown in results slot
		amount *= event.getCurrentItem().getAmount();
		
		event.getCurrentItem();
		
		String dropsStr = plugin.config.getString("craft." + item + ".items");
		for(int d = 0; d < event.getCurrentItem().getAmount(); ++d){
			Collection<ItemStack> result = plugin.computeDrops(dropsStr);
			plugin.givePlayerItems(player, result.toArray(new ItemStack[result.size()]));
		}
		
		player.giveExp(event.getCurrentItem().getAmount() * plugin.getConfig().getInt("craft." + item + ".exp", 0));
		
		if(amount == 0) return;
		if(amount > 0){
			plugin.playerReward(player, amount, 0, 0);
		} else if(amount < 0){
			plugin.playerReward(player, 0, 0, -amount);
		}
		
		
		
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onInventoryClick(InventoryClickEvent event){
		if(!plugin.smeltRewards) return;
		if(event.getInventory().getType() != InventoryType.FURNACE) return;
		if(event.getWhoClicked() == null || !(event.getWhoClicked() instanceof Player)) return;
		if(event.getSlotType() != SlotType.RESULT) return;
		Player player = (Player) event.getWhoClicked();
		if(!plugin.enabledGameModes.get(player.getGameMode())) return;
		
		int playerAmount = player.getItemOnCursor().getAmount();
		int slotAmount = event.getCurrentItem().getAmount();
		int itemAmount = 0;
		if(slotAmount == 0) return;
		
		if(event.isShiftClick()){
			itemAmount = plugin.getFittingAmount(event.getCurrentItem(), player.getInventory());
		} else if(event.isRightClick()){
			if(player.getItemOnCursor().getType() == Material.AIR){
				itemAmount = (int) Math.ceil(slotAmount / 2d);
			} else if(player.getItemOnCursor().isSimilar(event.getCurrentItem())){
				itemAmount = Math.min(slotAmount, event.getCurrentItem().getMaxStackSize() - playerAmount);
			} else return;
		}
		if(player.getItemOnCursor().getType() == Material.AIR){
			itemAmount = slotAmount;
		} else if(player.getItemOnCursor().isSimilar(event.getCurrentItem())){
			itemAmount = Math.min(slotAmount, event.getCurrentItem().getMaxStackSize() - playerAmount);
		}
		
		String item = event.getCurrentItem().getType().toString().toLowerCase();
		double amount = plugin.config.getDouble("smelt." + item + ".money", 0);
		String withMeta = "smelt." + item + "/" + event.getCurrentItem().getDurability() +".money";
		if(plugin.config.contains(withMeta)){
			amount = plugin.config.getDouble(withMeta);
		}
		amount *= itemAmount;
		
		String dropsStr = plugin.config.getString("smelt." + item + ".items");
		for(int d = 0; d < itemAmount; ++d){
			Collection<ItemStack> result = plugin.computeDrops(dropsStr);
			plugin.givePlayerItems(player, result.toArray(new ItemStack[result.size()]));
		}
		
		
		player.giveExp(itemAmount * plugin.getConfig().getInt("smelt." + item + ".exp", 0));
		
		if(amount == 0) return;
		if(amount > 0){
			plugin.playerReward(player, amount, 0, 0);
		} else if(amount < 0){
			plugin.playerReward(player, 0, 0, -amount);
		}
	}
	

	
}
