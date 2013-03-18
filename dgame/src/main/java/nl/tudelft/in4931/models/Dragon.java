package nl.tudelft.in4931.models;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Dragon extends Participant {
	
	public Dragon() {
		this(75, 20);
	}

	Dragon(int hp, int ap) {
		super(75, 20);
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(getHp()).append(getAp()).toHashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Dragon) {
			Dragon o = (Dragon) other;
			return new EqualsBuilder().append(getHp(), o.getHp()).append(getAp(), o.getAp()).isEquals();
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "[Dragon HP:" + getHp() + " AP:" + getAp() + "]";
	}

	@Override
	public Dragon copy() {
		return new Dragon(getHp(), getAp());
	}
	
}
