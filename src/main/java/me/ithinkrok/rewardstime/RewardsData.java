package me.ithinkrok.rewardstime;

public class RewardsData {

	public double gained = 0;
	public double gainedBonus = 0;
	public double lost = 0;
	
	public double getTotal(){
		return gained + gainedBonus - lost;
	}
	
	public void change(double gain, double bonus, double loss){
		gained += gain;
		gainedBonus += bonus;
		lost += loss;
	}
	
}
