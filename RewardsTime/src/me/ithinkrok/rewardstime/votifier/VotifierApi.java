package me.ithinkrok.rewardstime.votifier;

import org.bukkit.Bukkit;

import me.ithinkrok.rewardstime.RewardsTime;

public class VotifierApi {

	public void createListener(RewardsTime plugin){
		Bukkit.getServer().getPluginManager().registerEvents(new VotifierListener(plugin), plugin);
	}
}
