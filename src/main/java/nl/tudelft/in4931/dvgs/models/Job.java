package nl.tudelft.in4931.dvgs.models;

import nl.tudelft.in4931.dvgs.network.Message;

public class Job implements Message {
	
	private static final long serialVersionUID = -5550514054455665498L;
	
	private final long id;
	private final int duration;
	private final State state;

	public Job(long id, int duration, State state) {
		this.id = id;
		this.duration = duration;
		this.state = state;
	}
	
	public long getId() {
		return id;
	}

	public long getDuration() {
		return duration;
	}
	
	public State getState() {
		return state;
	}
	
	public static enum State {
		IDLE, RUNNING, FINISHED;
	}
	
}
