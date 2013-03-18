package nl.tudelft.in4931.models;

public class Action {
	
	private final long time;
	private final Participant participant; 

	public Action(long time, Participant participant) {
		this.time = time;
		this.participant = participant;
	}
	
	public long getTime() {
		return time;
	}
	
	public Participant getParticipant() {
		return participant;
	}
	
}
