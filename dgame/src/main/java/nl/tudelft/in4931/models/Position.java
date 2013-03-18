package nl.tudelft.in4931.models;

import java.util.Random;

import nl.tudelft.in4931.network.Message;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Position implements Message {

	private static final long serialVersionUID = -2087274577522934039L;

	public static Position randomPosition(int i, int j) {
		Random random = new Random(System.nanoTime());
		return new Position(random.nextInt(i), random.nextInt(j));
	}

	private final int x;
	private final int y;
	
	public Position(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(x).append(y).toHashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Position) {
			Position o = (Position) other;
			return new EqualsBuilder().append(x, o.x).append(y, o.y).isEquals();
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}
	
}
