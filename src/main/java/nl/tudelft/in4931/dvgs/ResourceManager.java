package nl.tudelft.in4931.dvgs;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Queue;
import java.util.Set;

import nl.tudelft.in4931.dvgs.models.Cluster;
import nl.tudelft.in4931.dvgs.models.Job;
import nl.tudelft.in4931.dvgs.network.Address;
import nl.tudelft.in4931.dvgs.network.Handler;
import nl.tudelft.in4931.dvgs.network.JobState;
import nl.tudelft.in4931.dvgs.network.JobState.State;
import nl.tudelft.in4931.dvgs.network.Role;
import nl.tudelft.in4931.dvgs.network.TopologyAwareNode;

import com.google.common.collect.Queues;

public class ResourceManager extends TopologyAwareNode {

	private final Cluster cluster;
	private final Queue<Job> jobQueue;
	private final Object lock = new Object();
	
	public ResourceManager(InetAddress address, int maxCapacity) throws IOException {
		super(address, Role.RESOURCE_MANAGER);
		this.jobQueue = Queues.newArrayDeque();
		this.cluster = new Cluster(maxCapacity) {
			@Override
			public void onJobsCompleted(Set<Job> completed) {
				synchronized (lock) {
					double utilization = cluster.getUtilization();
					for (Job job : completed) {
						broadcastToSchedulers(new JobState(job, State.FINISHED, utilization, getLocalAddress()));
					}
					
					while (!jobQueue.isEmpty()) {
						if (cluster.offer(jobQueue.peek())) {
							Job job = jobQueue.poll();
							broadcastToSchedulers(new JobState(job, State.IN_PROGRESS, utilization, getLocalAddress()));
						}
						else {
							break;
						}
					}
				}
			}
		};
		
		registerMessageHandlers();
	}

	private void registerMessageHandlers() {
		on(Job.class, new Handler<Job>() {
			@Override
			public void onMessage(Job job, Address origin) {
				synchronized (lock) {
					if (!jobQueue.isEmpty() || !cluster.offer(job)) {
						jobQueue.add(job);
					}
				}
				broadcastToSchedulers(new JobState(job, State.ACCEPTED, cluster.getUtilization(), getLocalAddress()));
			}
		});
	}
	
	public void offerJob(Job job) {
		boolean accepted = false;
		synchronized (lock) {
			accepted = jobQueue.isEmpty() && cluster.offer(job);
		}
		
		if (accepted) {
			broadcastToSchedulers(new JobState(job, State.IN_PROGRESS, cluster.getUtilization(), getLocalAddress()));
		}
		else {
			broadcastToSchedulers(new JobState(job, State.RESCHEDULE, cluster.getUtilization(), getLocalAddress()));
		}
	}

}
