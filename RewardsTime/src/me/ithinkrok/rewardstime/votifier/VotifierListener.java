package me.ithinkrok.rewardstime.votifier;

import java.util.Collection;

import me.ithinkrok.rewardstime.RewardsTime;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.inventory.ItemStack;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

public class VotifierListener implements Listener {

	
	RewardsTime plugin;
	
	
	
	public VotifierListener(RewardsTime plugin) {
		super();
		this.plugin = plugin;
	}



	@SuppressWarnings("deprecation") //Votifier is the real depreciated one...
	@EventHandler(priority = EventPriority.MONITOR)
	public void onVote(VotifierEvent event){
		if(!plugin.voteRewards) return;
		Vote vote = event.getVote();
		OfflinePlayer voter = Bukkit.getOfflinePlayer(vote.getUsername());
		if(voter == null){
			plugin.getLogger().info(vote.getUsername() + " voted, but they are not a player!");
			return;
		}
		plugin.incrementVoteCount(voter.getUniqueId());
		
		int votes = plugin.getVotes(voter.getUniqueId());
		if(voter.getPlayer() != null){
			voter.getPlayer().sendMessage(plugin.title + "You have voted " + plugin.valColor + votes + plugin.white + " times!");
		}
		
		String str = "votes.achieve." + votes + "";
		if(!rewardPlayer(voter, str)){
			int highest = 0;
			for(int d = 0; d < plugin.voteEveryList.size(); ++d){
				if((votes % plugin.voteEveryList.get(d)) == 0 && plugin.voteEveryList.get(d) > highest){
					highest = plugin.voteEveryList.get(d);
				}
			}
			str = "votes.every." + highest + "";
			rewardPlayer(voter, str);
		}
		
	}
	
	public boolean rewardPlayer(OfflinePlayer voter, String reward){
		double amount = plugin.config.getDouble(reward + ".money", 0);
		if(amount == 0) return false;
		
		plugin.economyDeposit(voter, amount);
		broadcast(plugin.config.getString(reward + ".broadcast", ""), voter, amount);
		Collection<ItemStack> items = plugin.computeDrops(plugin.config.getString(reward + ".items"));
		int xp = plugin.config.getInt(reward + ".exp", 0);
		
		if(voter.getPlayer() == null){
			if(!items.isEmpty() || xp != 0){
				Bukkit.broadcastMessage(plugin.title + ChatColor.RED + voter.getName() + plugin.white + " should have been online to collect an additional reward.");
			}
			return true;
		}
		Player player = voter.getPlayer();
		plugin.givePlayerItems(player, items.toArray(new ItemStack[items.size()]));
		player.giveExp(xp);
		
		
		return true;
	}
	
	public void broadcast(String msg, OfflinePlayer player, double money){
		if(msg == null) return;
		msg = msg.replace("<player>", player.getName()).replace("<money>", Double.toString(money));
		msg = msg.replace("&", "§");
		Bukkit.broadcastMessage(plugin.title + msg);
	}
}
