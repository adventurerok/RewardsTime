package me.ithinkrok.rewardstime;

public class RewardsBonus {

	public static enum BonusType {
		MULTIPLY,
		ADD
	}
	
	public BonusType type;
	public double amount;
	
	public RewardsBonus(BonusType type, double amount) {
		super();
		this.type = type;
		this.amount = amount;
	}
	
	public double apply(double in){
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
