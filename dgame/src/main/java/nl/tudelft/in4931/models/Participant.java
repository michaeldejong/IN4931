package nl.tudelft.in4931.models;

import nl.tudelft.in4931.network.Message;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public abstract class Participant implements Message, Comparable<Participant> {
			
	private static final long serialVersionUID = -2286173819285841233L;
	
	private final String name;
	private final int hp;
	private final int ap;
	
	public Participant(String name, int hp, int ap) {
		this.name = name;
		this.hp = hp;
		this.ap = ap;
	}
	
	public int getHp() {
		return hp;
	}
	
	public int getAp() {
		return ap;
	}
	
	public String getName() {
		return name;
	}

	public abstract Participant copy();
	
	public enum Type {
		PLAYER {
			public Participant create(String name) {
				return new Player(name);
			}
		},
		DRAGON {
			public Participant create(String name) {
				return new Dragon(name);
			}
		};
		
		public abstract Participant create(String name);
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(name).append(hp).append(ap).toHashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Participant) {
			Participant o = (Participant) other;
			return new EqualsBuilder().append(name, o.name).append(hp, o.hp).append(ap, o.ap).isEquals();
		}
		return false;
	}
	
	@Override
	public int compareTo(Participant o) {
		return name.compareTo(o.name);
	}
	
}
