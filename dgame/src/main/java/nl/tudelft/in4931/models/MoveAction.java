package nl.tudelft.in4931.models;

public class MoveAction extends Action {

	private static final long serialVersionUID = -7511113938977214661L;
	
	private final Position position;

	public MoveAction(long time, Participant participant, Position position) {
		super(time, participant);
		this.position = position;
	}
	
	public Position getPosition() {
		return position;
	}
	
}
