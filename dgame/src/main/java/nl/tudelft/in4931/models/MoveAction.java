package nl.tudelft.in4931.models;

import java.util.Random;

public class MoveAction extends Action {

	private static final long serialVersionUID = -7511113938977214661L;
	
	private final Direction direction;

	public MoveAction(Long time, Direction direction) {
		super(time);
		this.direction = direction;
	}
	
	public Direction getDirection() {
		return direction;
	}
	
	public enum Direction {
		LEFT 	(-1,  0), 
		RIGHT 	( 1,  0), 
		UP 		( 0,  1), 
		DOWN 	( 0, -1);
		
		private final int dX;
		private final int dY;

		private Direction(int dX, int dY) {
			this.dX = dX;
			this.dY = dY;
		}
		
		public int getDx() {
			return dX;
		}
		
		public int getDy() {
			return dY;
		}

		public static Direction random() {
			return values()[new Random().nextInt(4)];
		}

	}
	
}
