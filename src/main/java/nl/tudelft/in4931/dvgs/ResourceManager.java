package nl.tudelft.in4931.dvgs;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import nl.tudelft.in4931.dvgs.models.Cluster;
import nl.tudelft.in4931.dvgs.models.Job;
import nl.tudelft.in4931.dvgs.models.Jobs;
import nl.tudelft.in4931.dvgs.network.Address;
import nl.tudelft.in4931.dvgs.network.Handler;
import nl.tudelft.in4931.dvgs.network.JobState;
import nl.tudelft.in4931.dvgs.network.JobState.State;
import nl.tudelft.in4931.dvgs.network.Role;
import nl.tudelft.in4931.dvgs.network.TopologyAwareNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

public class ResourceManager extends TopologyAwareNode {
	
	private static final Logger log = LoggerFactory.getLogger(ResourceManager.class);

	private final Cluster cluster;
	private final Queue<Job> jobQueue = Queues.newArrayDeque();
	private final Object lock = new Object();
	
	public ResourceManager(InetAddress address, int maxCapacity) throws IOException {
		super(address, Role.RESOURCE_MANAGER);
		this.cluster = new Cluster(getLocalAddress(), maxCapacity) {
			@Override
			public void onJobsCompleted(Set<Job> completed) {
				toScheduler(new JobState(Jobs.of(completed), State.FINISHED, calculateLoad(), getLocalAddress()), true);
				drainQueuedJobsToCluster();
			}
		};
		
		registerMessageHandlers();
	}

	private void registerMessageHandlers() {
		on(Jobs.class, new Handler<Jobs>() {
			@Override
			public void onMessage(Jobs jobs, Address origin) {
				synchronized (lock) {
					jobQueue.addAll(jobs.getJobs());
				}
				toScheduler(new JobState(jobs, State.ACCEPTED, calculateLoad(), getLocalAddress()), true);
			}
		});
	}
	
	public void offerJob(Job job, boolean fireAndForget) {
		offerJob(Jobs.of(job), fireAndForget);
	}
	
	public void offerJob(Jobs jobs, boolean fireAndForget) {
		boolean queueEmpty = drainQueuedJobsToCluster();
		if (!queueEmpty) {
			log.debug("{} - Cluster is very busy ({} utilization), moving job to scheduler.", getLocalAddress(), cluster.getUtilization());
			toScheduler(new JobState(jobs, State.RESCHEDULE, calculateLoad(), getLocalAddress()), fireAndForget);
			return;
		}

		List<Job> accepted = Lists.newArrayList();
		List<Job> reschedule = Lists.newArrayList();
		boolean atCapacity = false;
		
		for (Job job : jobs.getJobs()) {
			if (!atCapacity && cluster.offer(job)) {
				accepted.add(job);
			}
			else {
				atCapacity = true;
				reschedule.add(job);
			}
		}
		
		toScheduler(new JobState(Jobs.of(accepted), State.ACCEPTED, calculateLoad(), getLocalAddress()), fireAndForget);
		toScheduler(new JobState(Jobs.of(reschedule), State.RESCHEDULE, calculateLoad(), getLocalAddress()), fireAndForget);
	}
	
	private double calculateLoad() {
		double queuedLoad;
		synchronized (lock) {
			queuedLoad = ((double) jobQueue.size()) / ((double) cluster.getMaxCapacity());
		}
		double currentLoad = cluster.getUtilization();
		return queuedLoad + currentLoad;
	}

	private boolean drainQueuedJobsToCluster() {
		synchronized (lock) {
			while (!jobQueue.isEmpty()) {
				if (cluster.offer(jobQueue.peek())) {
					jobQueue.poll();
				}
				else {
					break;
				}
			}
			return jobQueue.isEmpty();
		}
	}

}
