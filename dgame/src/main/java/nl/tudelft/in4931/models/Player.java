package nl.tudelft.in4931.models;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Player extends Participant {
	
	public Player() {
		this(75, 20);
	}

	Player(int hp, int ap) {
		super(75, 20);
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(getHp()).append(getAp()).toHashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Player) {
			Player o = (Player) other;
			return new EqualsBuilder().append(getHp(), o.getHp()).append(getAp(), o.getAp()).isEquals();
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "[Player HP:" + getHp() + " AP:" + getAp() + "]";
	}

	@Override
	public Player copy() {
		return new Player(getHp(), getAp());
	}
	
}
