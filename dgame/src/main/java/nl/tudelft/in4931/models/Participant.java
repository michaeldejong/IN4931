package nl.tudelft.in4931.models;

public abstract class Participant {
			
	private int hp;
	private int ap;
	
	public Participant(int hp, int ap) {
		this.hp = hp;
		this.ap = ap;
	}
	
	public void setHp(int hp) {
		this.hp = hp;
	}
	
	public int getHp() {
		return hp;
	}
	
	public void setAp(int ap) {
		this.ap = ap;
	}
	
	public int getAp() {
		return ap;
	}

	public abstract Participant copy();
	
}
