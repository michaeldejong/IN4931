package nl.tudelft.in4931.models;

import nl.tudelft.in4931.network.Message;

public class Action implements Message {
	
	private static final long serialVersionUID = 2577229307016464915L;
	
	private final Participant participant;
	
	private Long time;

	public Action(Long time, Participant participant) {
		this.time = time;
		this.participant = participant;
	}
	
	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}
	
	public Participant getParticipant() {
		return participant;
	}
	
}
