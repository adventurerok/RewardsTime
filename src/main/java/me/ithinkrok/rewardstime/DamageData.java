package me.ithinkrok.rewardstime;

import java.util.*;
import java.util.Map.Entry;

public class DamageData {

	public HashMap<UUID, Double> damageAmounts = new HashMap<>();
	public long lastDamage = 0;
	
	public void damageFrom(UUID player, double amount){
		Double previous = damageAmounts.get(player);
		if(previous == null) previous = 0.0;
		previous += amount;
		damageAmounts.put(player, amount);
		lastDamage = System.nanoTime();
	}
	
	public HashMap<UUID, Double> getResult(){
		double total = 0;
		for(Entry<UUID, Double> entry : damageAmounts.entrySet()){
			total += entry.getValue();
		}
		HashMap<UUID, Double> results = new HashMap<>();
		for(Entry<UUID, Double> entry : damageAmounts.entrySet()){
			results.put(entry.getKey(), entry.getValue() / total);
		}
		return results;
	}
}
