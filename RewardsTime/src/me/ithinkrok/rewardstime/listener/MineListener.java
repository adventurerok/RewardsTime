package me.ithinkrok.rewardstime.listener;

import java.util.Collection;
import java.util.Map.Entry;

import me.ithinkrok.rewardstime.RewardsBonus;
import me.ithinkrok.rewardstime.RewardsTime;
import me.ithinkrok.rewardstime.RewardsBonus.BonusType;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
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
		if(!event.getPlayer().hasPermission("rewardstime.rewards.from.block")) return;
		if(!plugin.enabledGameModes.get(event.getPlayer().getGameMode())) return;
		String item = event.getBlock().getType().toString().toLowerCase();
		int data = event.getBlock().getData();
		String base = "block." + item;
		double amount = plugin.config.getDouble(base + ".money", 0);
		String withMeta = base + "/" + data;
		if(plugin.config.contains(withMeta)){
			amount = plugin.config.getDouble(withMeta + ".money");
			base = withMeta;
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
		
		if(event.getPlayer().hasPermission("rewardstime.rewards.type.items")){
			Collection<ItemStack> items = plugin.computeDrops(plugin.config.getString(base + ".items"), (int)plugin.getPlayerItemPerk(event.getPlayer()));
			plugin.dropItems(event.getBlock().getLocation(), items.toArray(new ItemStack[items.size()]));
		}
		
		if(event.getPlayer().hasPermission("rewardstime.rewards.type.exp")){
			int exp = (int) (plugin.config.getInt(base + ".exp", 0) * plugin.getPlayerExpPerk(event.getPlayer()));
			if(exp > 0){
				ExperienceOrb xp = (ExperienceOrb) event.getBlock().getLocation().getWorld().spawnEntity(event.getBlock().getLocation(), EntityType.EXPERIENCE_ORB);
				xp.setExperience(exp);
			}
		}
		
		if(event.getPlayer().hasPermission("rewardstime.rewards.type.perms")){
			String perms = plugin.config.getString(base + ".perms");
			plugin.givePermissions(event.getPlayer(), perms);
		}
		
		if(event.getPlayer().hasPermission("rewardstime.rewards.type.broadcast")){
			String bc = plugin.config.getString(base + ".broadcast");
			plugin.broadcast(bc, event.getPlayer().getName(), amount);
		}
		
		if(event.getPlayer().hasPermission("rewardstime.rewards.type.tell")){
			String tell = plugin.config.getString(base + ".tell");
			plugin.tell(tell, event.getPlayer(), amount);
		}
		
		if(amount == 0 || !event.getPlayer().hasPermission("rewardstime.rewards.type.money")) return;
		amount *= plugin.getPlayerMoneyPerk(event.getPlayer());
		if(amount > 0){
			plugin.playerReward(event.getPlayer(), amountStart, amount - amountStart, 0);
		} else if(amount < 0){
			plugin.playerReward(event.getPlayer(), 0, 0, -amount);
		}
	}
}
