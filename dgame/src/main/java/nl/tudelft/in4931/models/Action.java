package nl.tudelft.in4931.models;

import nl.tudelft.in4931.network.Message;

public class Action implements Message {
	
	private static final long serialVersionUID = 2577229307016464915L;
	
	private Long time;
	private String participant;

	public Action(Long time) {
		this.time = time;
	}
	
	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}
	
	public String getParticipant() {
		return participant;
	}

	public void setParticipant(String participant) {
		this.participant = participant;
	}
	
}
