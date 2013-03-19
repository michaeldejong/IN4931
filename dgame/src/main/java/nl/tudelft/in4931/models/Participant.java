package nl.tudelft.in4931.models;

import nl.tudelft.in4931.network.Message;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Participant implements Message, Comparable<Participant> {
			
	private static final long serialVersionUID = -2286173819285841233L;
	
	private final String name;
	private final int hp;
	private final int ap;
	private final Type type;
	
	public Participant(Type type, String name, int hp, int ap) {
		this.type = type;
		this.name = name;
		this.hp = hp;
		this.ap = ap;
	}
	
	public Type getType() {
		return type;
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
	
	public Participant setHp(int hp) {
		return new Participant(type, name, hp, ap);
	}

	public Participant deltaHp(int delta) {
		return setHp(hp + delta);
	}
	
	public Participant setAp(int ap) {
		return new Participant(type, name, hp, ap);
	}

	public Participant deltaAp(int delta) {
		return setHp(ap + delta);
	}
	
	public Participant copy() {
		return new Participant(type, name, hp, ap);
	}
	
	public enum Type {
		PLAYER {
			public Participant create(String name) {
				return new Participant(Type.PLAYER, name, 20, 1);
			}
		},
		DRAGON {
			public Participant create(String name) {
				return new Participant(Type.DRAGON, name, 100, 5);
			}
		};
		
		public abstract Participant create(String name);
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(name).toHashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Participant) {
			Participant o = (Participant) other;
			return new EqualsBuilder().append(name, o.name).isEquals();
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "[" + type.name() + "(" + name + ") HP:" + hp + " AP:" + ap + "]";
	}
	
	@Override
	public int compareTo(Participant o) {
		return name.compareTo(o.name);
	}
	
}
