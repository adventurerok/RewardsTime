package me.ithinkrok.rewardstime.votifier;

import me.ithinkrok.rewardstime.RewardsTime;

import org.bukkit.*;
import org.bukkit.event.*;

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
		plugin.getLogger().info(voter.getName() + " voted!");
		plugin.incrementVoteCount(voter.getUniqueId());
		
		int votes = plugin.getVotes(voter.getUniqueId());
		if(voter.getPlayer() != null){
			voter.getPlayer().sendMessage(plugin.title + "You have voted " + plugin.valColor + votes + plugin.white + " times!");
		}
		
		String str;
		double amount;
		String bc;
		
		
		str = "votes.achieve." + votes + "";
		amount = plugin.config.getDouble(str + ".money", 0);
		if(amount != 0){
			plugin.economyDeposit(voter, amount);
			bc = plugin.config.getString(str + ".broadcast", "");
			broadcast(bc, voter, amount);
		} else {
			int highest = 0;
			for(int d = 0; d < plugin.voteEveryList.size(); ++d){
				if((votes % plugin.voteEveryList.get(d)) == 0 && plugin.voteEveryList.get(d) > highest){
					highest = plugin.voteEveryList.get(d);
				}
			}
			str = "votes.every." + highest + "";
			amount = plugin.config.getDouble(str + ".money", 0);
			if(amount != 0){
				plugin.economyDeposit(voter, amount);
				bc = plugin.config.getString(str + ".broadcast", "");
				broadcast(bc, voter, amount);
			}
		}
		
	}
	
	public void broadcast(String msg, OfflinePlayer player, double money){
		if(msg == null) return;
		msg = msg.replace("<player>", player.getName()).replace("<money>", Double.toString(money));
		msg = msg.replace("&", "§");
		Bukkit.broadcastMessage(plugin.title + msg);
	}
}
