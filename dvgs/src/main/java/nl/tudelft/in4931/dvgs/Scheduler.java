package nl.tudelft.in4931.dvgs;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import nl.tudelft.in4931.dvgs.models.ClusterState;
import nl.tudelft.in4931.dvgs.models.Job;
import nl.tudelft.in4931.dvgs.models.Jobs;
import nl.tudelft.in4931.dvgs.network.Address;
import nl.tudelft.in4931.dvgs.network.Handler;
import nl.tudelft.in4931.dvgs.network.JobState;
import nl.tudelft.in4931.dvgs.network.JobState.State;
import nl.tudelft.in4931.dvgs.network.Role;
import nl.tudelft.in4931.dvgs.network.TopologyAwareNode;
import nl.tudelft.in4931.dvgs.network.TopologyListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class Scheduler extends TopologyAwareNode implements TopologyListener, Runnable {
	
	private static final Logger log = LoggerFactory.getLogger(Scheduler.class);

	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
	private final AtomicReference<ClusterState> clusterStateReference = new AtomicReference<>(new ClusterState());
	private final List<JobListener> jobListeners = Lists.newArrayList();
	
	public Scheduler(InetAddress address) throws IOException {
		super(address, Role.SCHEDULER);
		
		registerMessageHandlers();
		executor.scheduleWithFixedDelay(this, 1000, 1000, TimeUnit.MILLISECONDS);
		
		log.info("{} - Started scheduler...", getLocalAddress());
	}
	
	private void registerMessageHandlers() {
		on(JobState.class, new Handler<JobState>() {
			@Override
			public void onMessage(JobState message, Address origin) {
				Jobs jobs = message.getJobs();
				State state = message.getState();
				double utilization = message.getClusterUtilization();
				Address resourceManager = message.getResourceManager();
				
				synchronized (clusterStateReference) {
					ClusterState clusterState = clusterStateReference.get();
					clusterState.update(jobs, state, utilization, resourceManager);
					toBackup(clusterState, true);
				}
				
				synchronized (jobListeners) {
					for (JobListener listener : jobListeners) {
						listener.onJobState(message);
					}
				}
			}
		});
		
		on(ClusterState.class, new Handler<ClusterState>() {
			@Override
			public void onMessage(ClusterState message, Address origin) {
				synchronized (clusterStateReference) {
					clusterStateReference.set(message);
				}
			}
		});
		
		addTopologyListener(this);
	}

	public void addListener(JobListener listener) {
		synchronized (jobListeners) {
			jobListeners.add(listener);
		}
	}

	@Override
	public void onNodeLeft(Address address, Role role) {
		if (role == Role.RESOURCE_MANAGER) {
			synchronized (clusterStateReference) {
				ClusterState clusterState = clusterStateReference.get();
				clusterState.markClusterDown(address);
				toBackup(clusterState, true);
			}
		}
	}

	@Override
	public void onNodeJoin(Address address, Role role) {
		if (role == Role.RESOURCE_MANAGER) {
			synchronized (clusterStateReference) {
				ClusterState clusterState = clusterStateReference.get();
				clusterState.onNodeJoin(address);
			}
		}
	}

	@Override
	public void run() {
		log.debug("{} - Draining pending jobs to resource managers...", getLocalAddress());
		
		synchronized (clusterStateReference) {
			ClusterState clusterState = clusterStateReference.get();
			List<Job> pendingJobs = clusterState.getPendingJobs();
			if (pendingJobs.isEmpty()) {
				return;
			}
			
			List<Job> jobsToSchedule = Lists.newArrayList();
			int take = Math.min(10, pendingJobs.size());
			for (int i = 0; i < take; i++) {
				jobsToSchedule.add(pendingJobs.remove(0));
			}
			
			Jobs jobs = Jobs.of(jobsToSchedule);
			
			boolean success = false;
			Address redirectTo = clusterState.leastUtilizedCluster();
			while (!success && redirectTo != null) {
				success = sendAndWait(jobs, redirectTo);
				if (!success) {
					clusterState.markClusterDown(redirectTo);
					toBackup(clusterState, true);
					redirectTo = clusterState.leastUtilizedCluster();
					
					if (redirectTo == null) {
						pendingJobs.addAll(0, jobsToSchedule);
					}
				}
			}
		}
	}
	
}
