package nl.tudelft.in4931.dvgs.models;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import nl.tudelft.in4931.dvgs.network.Address;
import nl.tudelft.in4931.dvgs.network.JobState.State;
import nl.tudelft.in4931.dvgs.network.Message;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ClusterState implements Message {
	
	private static final long serialVersionUID = -8574023360942288949L;
	
	private final List<Job> pendingJobs;
	private final Map<Address, Map<Job, State>> clusterState;
	private final Map<Address, Double> clusterUtilizations;
	
	public ClusterState() {
		this.pendingJobs = Lists.newArrayList();
		this.clusterState = Maps.newTreeMap(); 
		this.clusterUtilizations = Maps.newHashMap();
	}
	
	public Address leastUtilizedCluster() {
		synchronized (clusterUtilizations) {
			double least = Double.MAX_VALUE;
			Address resourceManager = null;
			
			for (Entry<Address, Double> entry : clusterUtilizations.entrySet()) {
				if (entry.getValue() < least) {
					least = entry.getValue();
					resourceManager = entry.getKey();
				}
			}
			
			return resourceManager;
		}
	}
	
	public void update(Job job, State state, double utilization, Address resourceManager) {
		synchronized (clusterUtilizations) {
			clusterUtilizations.put(resourceManager, utilization);
		}
		
		if (state == State.RESCHEDULE) {
			synchronized (pendingJobs) {
				pendingJobs.add(job);
			}
		}
		else {
			synchronized (clusterState) {
				Map<Job, State> managerState = clusterState.get(resourceManager);
				if (managerState == null) {
					managerState = Maps.newHashMap();
					clusterState.put(resourceManager, managerState);
				}
				
				if (state == State.FINISHED) {
					managerState.remove(job);
				}
				else {
					managerState.put(job, state);
				}
			}
		}
	}

	public void markClusterDown(Address address) {
		synchronized (clusterUtilizations) {
			clusterUtilizations.remove(address);
		}
		
		Set<Job> failedJobs = Sets.newHashSet();
		synchronized (clusterState) {
			failedJobs = clusterState.remove(address).keySet();
		}
		
		synchronized (pendingJobs) {
			pendingJobs.addAll(failedJobs);
		}
	}

	public void onNodeJoin(Address address) {
		synchronized (clusterUtilizations) {
			clusterUtilizations.put(address, 0.0);
		}
	}
	
	@Override
	public String toString() {
		return  "[ClusterState: [" + Joiner.on(",").join(pendingJobs) + "]]"; 
	}

}
