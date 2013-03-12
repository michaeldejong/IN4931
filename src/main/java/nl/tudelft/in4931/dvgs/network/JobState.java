package nl.tudelft.in4931.dvgs.network;

import java.io.Serializable;

import nl.tudelft.in4931.dvgs.models.Job;
import nl.tudelft.in4931.dvgs.models.Jobs;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class JobState implements Message {

	private static final long serialVersionUID = 6172505681748387989L;
	
	private final Jobs jobs;
	private final State state;
	private final Address resourceManager;
	private final double clusterUtilization;
	
	public JobState(Job job, State state, double clusterUtilization, Address resourceManager) {
		this(Jobs.of(job), state, clusterUtilization, resourceManager);
	}
	
	public JobState(Jobs jobs, State state, double clusterUtilization, Address resourceManager) {
		this.jobs = jobs;
		this.state = state;
		this.clusterUtilization = clusterUtilization;
		this.resourceManager = resourceManager;
	}

	public Jobs getJobs() {
		return jobs;
	}
	
	public State getState() {
		return state;
	}
	
	public Address getResourceManager() {
		return resourceManager;
	}
	
	public double getClusterUtilization() {
		return clusterUtilization;
	}

	public int numberOfJobs() {
		return jobs.getJobs().size();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder()
			.append(jobs)
			.append(state)
			.append(resourceManager)
			.append(clusterUtilization)
			.toHashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof JobState) {
			JobState o = (JobState) other;
			return new EqualsBuilder()
				.append(jobs, o.jobs)
				.append(state, o.state)
				.append(resourceManager, o.resourceManager)
				.append(clusterUtilization, o.clusterUtilization)
				.isEquals();
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "[JobState: " + jobs + ", " + state + ", " + resourceManager + ", " + clusterUtilization + "]";
	}
	
	public static enum State implements Serializable {
		RESCHEDULE, ACCEPTED, FINISHED;
	}

}
