package me.ithinkrok.rewardstime.votifier;

import me.ithinkrok.rewardstime.RewardsTime;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
		plugin.incrementVoteCount(voter.getUniqueId());
		
		int votes = plugin.getVotes(voter.getUniqueId());
		if(voter.getPlayer() != null){
			voter.getPlayer().sendMessage(plugin.title + "You have voted " + plugin.valColor + votes + plugin.white + " times!");
		}
		
		String str = "votes.achieve." + votes + "";
		if(!plugin.rewardPlayer(str, voter, 1)){
			int highest = 0;
			for(int d = 0; d < plugin.voteEveryList.size(); ++d){
				if((votes % plugin.voteEveryList.get(d)) == 0 && plugin.voteEveryList.get(d) > highest){
					highest = plugin.voteEveryList.get(d);
				}
			}
			str = "votes.every." + highest + "";
			plugin.rewardPlayer(str, voter, 1);
		}
		
	}
}
