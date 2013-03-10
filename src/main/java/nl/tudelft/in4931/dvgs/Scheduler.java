package nl.tudelft.in4931.dvgs;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import nl.tudelft.in4931.dvgs.models.ClusterState;
import nl.tudelft.in4931.dvgs.models.Job;
import nl.tudelft.in4931.dvgs.network.Address;
import nl.tudelft.in4931.dvgs.network.Handler;
import nl.tudelft.in4931.dvgs.network.JobState;
import nl.tudelft.in4931.dvgs.network.JobState.State;
import nl.tudelft.in4931.dvgs.network.Role;
import nl.tudelft.in4931.dvgs.network.TopologyAwareNode;
import nl.tudelft.in4931.dvgs.network.TopologyListener;

import com.google.common.collect.Lists;

public class Scheduler extends TopologyAwareNode implements TopologyListener {

	private volatile ClusterState clusterState;
	private final List<JobListener> jobListeners;
	
	public Scheduler(InetAddress address) throws IOException {
		super(address, Role.SCHEDULER);
		this.clusterState = new ClusterState();
		this.jobListeners = Lists.newArrayList();
		
		registerMessageHandlers();
	}
	
	private void registerMessageHandlers() {
		on(JobState.class, new Handler<JobState>() {
			@Override
			public void onMessage(JobState message, Address origin) {
				Job job = message.getJob();
				State state = message.getState();
				double utilization = message.getClusterUtilization();
				Address resourceManager = message.getResourceManager();
				
				synchronized (clusterState) {
					clusterState.update(job, state, utilization, resourceManager);
					broadcastToBackupSchedulers(clusterState);
					
					if (state == State.RESCHEDULE) {
						Address redirectTo = clusterState.leastUtilizedCluster();
						boolean success = false;
						while (!success && redirectTo != null) {
							success = sendAndWait(job, redirectTo, true);
							if (!success) {
								clusterState.markClusterDown(redirectTo);
								broadcastToBackupSchedulers(clusterState);
								redirectTo = clusterState.leastUtilizedCluster();
							}
						}
					}
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
				synchronized (clusterState) {
					clusterState = message;
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
			synchronized (clusterState) {
				clusterState.markClusterDown(address);
				broadcastToBackupSchedulers(clusterState);
			}
		}
	}

	@Override
	public void onNodeJoin(Address address, Role role) {
		if (role == Role.RESOURCE_MANAGER) {
			synchronized (clusterState) {
				clusterState.onNodeJoin(address);
			}
		}
	}
	
}
