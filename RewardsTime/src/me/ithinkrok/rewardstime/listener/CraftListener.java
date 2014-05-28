package me.ithinkrok.rewardstime.listener;

import java.util.Collection;

import me.ithinkrok.rewardstime.RewardsTime;

import org.bukkit.Bukkit;
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
		if(event.getWhoClicked() == null || !(event.getWhoClicked() instanceof Player)) return;
		if(event.getSlotType() != SlotType.RESULT) return;
		final Player player = (Player) event.getWhoClicked();
		if(!plugin.enabledGameModes.get(player.getGameMode())) return;
		if(!player.hasPermission("rewardstime.rewards")) return;
		
		int playerAmount = player.getItemOnCursor().getAmount();
		int slotAmount = event.getCurrentItem().getAmount();
		int itemAmount = 0;
		if(slotAmount == 0) return;
		
		if(event.isShiftClick()){
			int maxCraft = plugin.getCraftAmount(event.getInventory().getMatrix());
			ItemStack toFit = event.getCurrentItem().clone();
			toFit.setAmount(slotAmount * maxCraft);
			itemAmount = plugin.getFittingAmount(toFit, player.getInventory());
			itemAmount = (itemAmount / slotAmount) * slotAmount;
		} else if(event.getHotbarButton() != -1){
			itemAmount = plugin.getFittingAmount(event.getCurrentItem(), player.getInventory());
		} else if(player.getItemOnCursor().getType() == Material.AIR){
			itemAmount = slotAmount;
		} else if(player.getItemOnCursor().isSimilar(event.getCurrentItem())){
			itemAmount = Math.min(slotAmount, event.getCurrentItem().getMaxStackSize() - playerAmount);
		}
		if(itemAmount <= 0) return;
		
		String item = event.getCurrentItem().getType().toString().toLowerCase();
		String base = "craft." + item;
		double amount = plugin.config.getDouble(base + ".money", 0);
		String withMeta = base + "/" + event.getCurrentItem().getDurability();
		if(plugin.config.contains(withMeta)){
			amount = plugin.config.getDouble(withMeta + ".money");
			base = withMeta;
		}
		amount *= itemAmount;
		
		final int itemAmountFinal = itemAmount;
		
		final String dropsStr = plugin.config.getString(base + ".items");
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			
			@Override
			public void run() {
				Collection<ItemStack> result = plugin.computeDrops(dropsStr, itemAmountFinal);
				plugin.givePlayerItems(player, result.toArray(new ItemStack[result.size()]));
			}
		});
		
		
		player.giveExp(itemAmount * plugin.getConfig().getInt(base + ".exp", 0));
		
		String perms = plugin.getConfig().getString(base + ".perms");
		plugin.givePermissions(player, perms);
		
		if(amount == 0) return;
		if(amount > 0){
			plugin.playerReward(player, amount, 0, 0);
		} else if(amount < 0){
			plugin.playerReward(player, 0, 0, -amount);
		}
		
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onInventoryClick(InventoryClickEvent event){
		if(event.isCancelled()) return;
		if(!plugin.smeltRewards) return;
		if(event.getWhoClicked() == null || !(event.getWhoClicked() instanceof Player)) return;
		if(event.getSlotType() != SlotType.RESULT) return;
		final Player player = (Player) event.getWhoClicked();
		if(!plugin.enabledGameModes.get(player.getGameMode())) return;
		if(!player.hasPermission("rewardstime.rewards")) return;
		
		int playerAmount = player.getItemOnCursor().getAmount();
		int slotAmount = event.getCurrentItem().getAmount();
		int itemAmount = 0;
		if(slotAmount == 0) return;
		if(event.isShiftClick() || event.getHotbarButton() != -1){
			itemAmount = plugin.getFittingAmount(event.getCurrentItem(), player.getInventory());
		} else if(event.isRightClick()){
			if(player.getItemOnCursor().getType() == Material.AIR){
				itemAmount = (int) Math.ceil(slotAmount / 2d);
			} else if(player.getItemOnCursor().isSimilar(event.getCurrentItem())){
				itemAmount = Math.min(slotAmount, event.getCurrentItem().getMaxStackSize() - playerAmount);
			} else return;
		} else if(player.getItemOnCursor().getType() == Material.AIR){
			itemAmount = slotAmount;
		} else if(player.getItemOnCursor().isSimilar(event.getCurrentItem())){
			itemAmount = Math.min(slotAmount, event.getCurrentItem().getMaxStackSize() - playerAmount);
		}
		if(itemAmount <= 0) return;
		
		String item = event.getCurrentItem().getType().toString().toLowerCase();
		String base = "smelt." + item;
		double amount = plugin.config.getDouble(base + ".money", 0);
		String withMeta = base + "/" + event.getCurrentItem().getDurability();
		if(plugin.config.contains(withMeta)){
			amount = plugin.config.getDouble(withMeta + ".money");
			base = withMeta;
		}
		amount *= itemAmount;
		
		final int itemAmountFinal = itemAmount;
		
		final String dropsStr = plugin.config.getString(base + ".items");
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			
			@Override
			public void run() {
				Collection<ItemStack> result = plugin.computeDrops(dropsStr, itemAmountFinal);
				plugin.givePlayerItems(player, result.toArray(new ItemStack[result.size()]));
			}
		});
		
		
		player.giveExp(itemAmount * plugin.getConfig().getInt(base + ".exp", 0));
		
		String perms = plugin.getConfig().getString(base + ".perms");
		plugin.givePermissions(player, perms);
		
		if(amount == 0) return;
		if(amount > 0){
			plugin.playerReward(player, amount, 0, 0);
		} else if(amount < 0){
			plugin.playerReward(player, 0, 0, -amount);
		}
	}
	

	
}
