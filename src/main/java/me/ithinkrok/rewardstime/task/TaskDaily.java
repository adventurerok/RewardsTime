package me.ithinkrok.rewardstime.task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import me.ithinkrok.rewardstime.RewardsTime;

import org.bukkit.scheduler.BukkitRunnable;

public class TaskDaily extends BukkitRunnable {
	
	public static final long DAY_MS = 86_400_000;
	public static long MAX_SKIP_TICKS = 12_000;
	
	volatile boolean stopped = false;
	
	RewardsTime plugin;
	
	long targetTime;
	
	public TaskDaily(RewardsTime plugin, String targetTime){
		this.plugin = plugin;
		if(targetTime == null){
			stopped = true;
			return;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		try {
			Date tar = sdf.parse(targetTime);
			Calendar input = Calendar.getInstance();
			Calendar target = Calendar.getInstance();
			input.setTime(tar);
			target.set(Calendar.SECOND, 0);
			target.set(Calendar.MINUTE, input.get(Calendar.MINUTE));
			target.set(Calendar.HOUR_OF_DAY, input.get(Calendar.HOUR_OF_DAY));
			if(!target.after(Calendar.getInstance())) target.add(Calendar.DAY_OF_YEAR, 1);
			this.targetTime = target.getTimeInMillis();
		} catch (ParseException e) {
			plugin.getLogger().warning("Daily date " + targetTime + " is invalid");
			stopped = true;
		}
	}
	

	public TaskDaily(RewardsTime plugin, long targetTime) {
		super();
		this.plugin = plugin;
		this.targetTime = targetTime;
	}
	
	public void start(){
		if(stopped) return;
		long time = System.currentTimeMillis();
		long diff = targetTime - time;
		if(diff <= 0) {
			runEvent();
			targetTime += DAY_MS;
			diff += DAY_MS;
		}
		int ticks = (int) (diff / 50);
		
		runTaskLaterAsynchronously(plugin, Math.min(ticks, MAX_SKIP_TICKS));
	}

	@Override
	public void run() {
		if(stopped) return;
		long time = System.currentTimeMillis();
		long diff = targetTime - time;
		if(diff <= 0) {
			runEvent();
			targetTime += DAY_MS;
			diff += DAY_MS;
		}
		int ticks = (int) (diff / 50);
		
		TaskDaily taskDaily = new TaskDaily(plugin, targetTime);
		plugin.dailyThread = taskDaily;
		taskDaily.runTaskLaterAsynchronously(plugin, Math.min(ticks, MAX_SKIP_TICKS));
	}
	
	public void stop(){
		stopped = true;
		try{
			cancel();
		} catch(IllegalStateException e){
			//Already stopped
		}
	}
	
	public void runEvent(){
		TaskDailyBonus task = new TaskDailyBonus(this);
		
		task.runTaskTimer(plugin, 10, 1);
	}

}
