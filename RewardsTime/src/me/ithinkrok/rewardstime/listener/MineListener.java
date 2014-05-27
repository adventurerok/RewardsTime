package me.ithinkrok.rewardstime.listener;

import java.util.Collection;
import java.util.Map.Entry;

import me.ithinkrok.rewardstime.RewardsBonus;
import me.ithinkrok.rewardstime.RewardsTime;
import me.ithinkrok.rewardstime.RewardsBonus.BonusType;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class MineListener implements Listener {

	RewardsTime plugin;
	
	public MineListener(RewardsTime plugin) {
		super();
		this.plugin = plugin;
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.MONITOR)
	public void onMineBlock(BlockBreakEvent event){
		if(event.isCancelled()) return;
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
		Collection<ItemStack> drops =event.getBlock().getDrops(event.getPlayer().getItemInHand());
		for(ItemStack i : drops){
			
			//Prevent infinite rewards for destroying the block
			if(i.getType() == event.getBlock().getType()) return;
		}
		double amountStart = amount;
		if(amount > 0 && plugin.toolBonus && event.getPlayer().getItemInHand().getType() != Material.AIR){
			ItemStack tool = event.getPlayer().getItemInHand();
			for(Entry<Enchantment, Integer> entry : tool.getEnchantments().entrySet()){
				String str = "tool.enchant." + entry.getKey().getName().toLowerCase();
				double bonus = plugin.getConfig().getDouble(str + ".bonus");
				BonusType type = plugin.getConfigBonusType(str + ".type", BonusType.MULTIPLY);
				double lvlBonus = plugin.getConfig().getDouble(str + "/" + entry.getValue() + ".bonus");
				if(lvlBonus != 0){
					bonus = lvlBonus;
					type =  plugin.getConfigBonusType(str + "/" + entry.getValue() + ".type", BonusType.MULTIPLY);
				}
				if(bonus != 0) amount = RewardsBonus.apply(amount, type, bonus);
			}
		}
		if(amount == 0) return;
		if(amount > 0){
			plugin.playerReward(event.getPlayer(), amountStart, amount - amountStart, 0);
		} else if(amount < 0){
			plugin.playerReward(event.getPlayer(), 0, 0, -amount);
		}
	}
}
