package nl.tudelft.in4931.dvgs.models;

import java.util.NavigableMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import com.google.common.collect.Maps;

public class Cluster implements Runnable {

	private final ScheduledThreadPoolExecutor executor;
	private final NavigableMap<Long, Job> nodes;
	private final int maxCapacity;
	
	public Cluster(int maxCapacity) {
		this.executor = new ScheduledThreadPoolExecutor(1);
		this.maxCapacity = maxCapacity;
		this.nodes = Maps.newTreeMap();
		
		executor.scheduleWithFixedDelay(this, 1000, 1000, TimeUnit.SECONDS);
	}
	
	public boolean addJob(Job job) {
		synchronized (nodes) {
			if (nodes.size() == maxCapacity) {
				return false;
			}
			
			long timestamp = System.currentTimeMillis();
			nodes.put(job.getDuration() + timestamp, job);
			return true;
		}
	}
	
	public int getAvailability() {
		synchronized (nodes) {
			return maxCapacity - nodes.size();
		}
	}

	public void run() {
		synchronized (nodes) {
			Long key;
			long timestamp = System.currentTimeMillis();
			
			while ((key = nodes.floorKey(timestamp)) != null) {
				nodes.remove(key);
			}
		}
	}
	
}
