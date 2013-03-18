package nl.tudelft.in4931.models;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Dragon extends Participant {
	
	private static final long serialVersionUID = -5514869993497642721L;

	public Dragon(String name) {
		this(name, 75, 20);
	}

	Dragon(String name, int hp, int ap) {
		super(name, 75, 20);
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
		return new Dragon(getName(), getHp(), getAp());
	}
	
}
