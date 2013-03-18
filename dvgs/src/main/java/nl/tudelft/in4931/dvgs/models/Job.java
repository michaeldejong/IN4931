package nl.tudelft.in4931.dvgs.models;

import java.io.Serializable;

public class Job implements Serializable {
	
	private static final long serialVersionUID = -5550514054455665498L;
	
	private final long id;
	private final int duration;

	public Job(long id, int duration) {
		this.id = id;
		this.duration = duration;
	}
	
	public long getId() {
		return id;
	}

	public long getDuration() {
		return duration;
	}
	
	@Override
	public int hashCode() {
		return (int) (id % Integer.MAX_VALUE);
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof Job && ((Job) other).id == id;
	}
	
	@Override
	public String toString() {
		return "[Job:" + id + ", " + duration + "]";
	}
	
}
