package me.ithinkrok.rewardstime.task;

import me.ithinkrok.rewardstime.RewardsTime;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

public class TaskDailyBonus extends BukkitRunnable {
	
	TaskDaily main;
	RewardsTime plugin;
	OfflinePlayer[] plrs;
	int index;
	
	public TaskDailyBonus(TaskDaily main) {
		this.main = main;
		this.plugin = main.plugin;
	}

	@Override
	public void run() {
		if(main.stopped) return;
		try {
			if (plrs == null) plrs = Bukkit.getOfflinePlayers();
			int left = 10;
			while (index < plrs.length && left > 0) {
				doPlayer(plrs[index]);

				++index;
				--left;
			}
			if (index >= plrs.length) {
				plugin.broadcast(plugin.config.getString("daily.broadcast", ""), "all", plugin.config.getDouble("daily.money", 0));
				cancel();
			}
		} catch (Exception e){
			cancel();
			plugin.getLogger().warning("Exception while resetting daily bonuses:");
			e.printStackTrace();
		}
	}
	
	public void doPlayer(OfflinePlayer player){
		String base = "daily";
		plugin.broadcast = false;
		plugin.rewardPlayer(base, player, 1);
		plugin.broadcast = true;
	}

}
