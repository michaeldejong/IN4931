package nl.tudelft.in4931.dvgs.models;

import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nl.tudelft.in4931.dvgs.network.Address;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class Cluster implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(Cluster.class);
	private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
	
	private final NavigableMap<Long, Job> nodes;
	private final int maxCapacity;
	private final Address local;
	
	private volatile int remainingCapacity;
	
	public Cluster(Address local, int maxCapacity) {
		this.local = local;
		this.maxCapacity = maxCapacity;
		this.nodes = Maps.newTreeMap();
		this.remainingCapacity = maxCapacity;
		
		executor.scheduleWithFixedDelay(this, 1000, 500, TimeUnit.MILLISECONDS);
	}
	
	public boolean offer(Job job) {
		synchronized (nodes) {
			if (nodes.size() == maxCapacity) {
				return false;
			}
			long timestamp = System.currentTimeMillis();
			long key = job.getDuration() + timestamp;
			
			while (nodes.containsKey(key)) {
				key++;
			}
			
			nodes.put(key, job);
			remainingCapacity--;
			return true;
		}
	}
	
	public int getRemainingCapacity() {
		synchronized (nodes) {
			return remainingCapacity;
		}
	}

	public double getMaxCapacity() {
		return maxCapacity;
	}

	public double getUtilization() {
		double available = (double) getRemainingCapacity();
		double total = (double) maxCapacity;
		return Math.min(Math.max(0.0, (total - available) / Math.max(1.0, total)), 1.0);
	}
	
	public void run() {
		try {
			synchronized (nodes) {
				long timestamp = System.currentTimeMillis();
				
				Set<Job> completed = Sets.newHashSet();
				
				Long key;
				while ((key = nodes.floorKey(timestamp)) != null) {
					Job job = nodes.remove(key);
					if (job != null) {
						remainingCapacity++;
						completed.add(job);
					}
				}
				
				if (!completed.isEmpty()) {
					log.info("{} - Cluster: {} finished jobs, {} remaining jobs", local, completed.size(), nodes.size());
					onJobsCompleted(completed);
				}
			}
		}
		catch (Throwable e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public abstract void onJobsCompleted(Set<Job> completed);
	
}
