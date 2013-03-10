package nl.tudelft.in4931.dvgs.models;

import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class Cluster implements Runnable {

	private final ScheduledThreadPoolExecutor executor;
	private final NavigableMap<Long, Job> nodes;
	private final int maxCapacity;
	private volatile int remainingCapacity;
	
	public Cluster(int maxCapacity) {
		this.executor = new ScheduledThreadPoolExecutor(1);
		this.maxCapacity = maxCapacity;
		this.nodes = Maps.newTreeMap();
		this.remainingCapacity = maxCapacity;
		
		executor.scheduleWithFixedDelay(this, 1000, 1000, TimeUnit.MILLISECONDS);
	}
	
	public boolean offer(Job job) {
		synchronized (nodes) {
			if (nodes.size() == maxCapacity) {
				return false;
			}
			
			long timestamp = System.currentTimeMillis();
			nodes.put(job.getDuration() + timestamp, job);
			remainingCapacity--;
			return true;
		}
	}
	
	public int getRemainingCapacity() {
		synchronized (nodes) {
			return remainingCapacity;
		}
	}

	public double getUtilization() {
		double available = (double) getRemainingCapacity();
		double total = (double) maxCapacity;
		return Math.min(Math.max(0.0, (total - available) / Math.max(1.0, total)), 1.0);
	}
	
	public void run() {
		synchronized (nodes) {
			Long key;
			long timestamp = System.currentTimeMillis();
			
			Set<Job> completed = Sets.newHashSet();
			
			while ((key = nodes.floorKey(timestamp)) != null) {
				Job job = nodes.remove(key);
				if (job != null) {
					remainingCapacity++;
					completed.add(job);
				}
			}
			
			if (!completed.isEmpty()) {
				onJobsCompleted(completed);
			}
		}
	}
	
	public abstract void onJobsCompleted(Set<Job> completed);
	
}
