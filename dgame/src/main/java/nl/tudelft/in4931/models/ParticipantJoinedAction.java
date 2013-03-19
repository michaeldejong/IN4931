package nl.tudelft.in4931.models;

import nl.tudelft.in4931.models.Participant.Type;

public class ParticipantJoinedAction extends Action {

	private static final long serialVersionUID = 2242628222694740111L;

	private final Type type;
	private final String name;
	private Position position;

	public ParticipantJoinedAction(Long time, Type type, String name) {
		super(time);
		this.type = type;
		this.name = name;
	}
	
	public void setPosition(Position position) {
		this.position = position;
	}
	
	public Position getPosition() {
		return position;
	}
	
	public Type getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return "[Join time: " + getTime() + " name: " + getName() + " type: " + getType() + "]";
	}

}
