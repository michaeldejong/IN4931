package nl.tudelft.in4931.dvgs.models;

import java.util.Collection;

import nl.tudelft.in4931.dvgs.network.Message;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

public class Jobs implements Message {
	
	private static final long serialVersionUID = -12302574818732814L;

	public static Jobs of(Job job) {
		return new Jobs(Sets.newHashSet(job));
	}

	public static Jobs of(Collection<Job> jobs) {
		return new Jobs(jobs);
	}
	
	private final Collection<Job> jobs;

	public Jobs(Collection<Job> jobs) {
		this.jobs = jobs;
	}
	
	public Collection<Job> getJobs() {
		return jobs;
	}
	
	@Override
	public int hashCode() {
		return jobs.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		return jobs.equals(other);
	}
	
	@Override
	public String toString() {
		return "[Jobs:" + Joiner.on(",").join(jobs) + "]";
	}
	
}
