package nl.tudelft.in4931.models;

public class ParticipantJoinedAction extends Action {

	private static final long serialVersionUID = 2242628222694740111L;

	public ParticipantJoinedAction(Long time, Participant participant) {
		super(time, participant);
	}
	
	@Override
	public String toString() {
		return "[Join time: " + getTime() + " participant: " + getParticipant() + "]";
	}

}
