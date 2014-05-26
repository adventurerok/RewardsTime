package me.ithinkrok.rewardstime;

public class RewardsBonus {

	public static enum BonusType {
		MULTIPLY,
		ADD,
		NONE
	}
	
	public BonusType type;
	public double amount;
	
	public RewardsBonus(BonusType type, double amount) {
		super();
		this.type = type;
		this.amount = amount;
	}
	
	public double apply(double in){
		return apply(in, type, in);
	}
	
	public static double apply(double in, BonusType type, double amount){
		switch(type){
		case MULTIPLY:
			return in * amount;
		case ADD:
			return in + amount;
		default:
			return in;
		}
	}
	
}
