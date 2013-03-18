package nl.tudelft.in4931.models;

import java.util.Random;

public class Position {

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
	
}
