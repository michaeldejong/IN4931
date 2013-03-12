package nl.tudelft.in4931.dvgs;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class LocalTest {
	
	public static ClusterBuilder defineCluster() throws IOException {
		return new ClusterBuilder(InetAddress.getLocalHost());
	}

	private ClusterBuilder clusterDefinition;
	
	public LocalTest(ClusterBuilder clusterDefinition) {
		this.clusterDefinition = clusterDefinition;
	}
	
	@Before
	public void setUp() throws IOException, InterruptedException {
		clusterDefinition.setUp();
	}
	
	@After
	public void tearDown() {
		clusterDefinition.tearDown();
	}
	
	protected Scheduler getScheduler(int i) {
		return clusterDefinition.getScheduler(i);
	}

	protected ResourceManager getResourceManager(int i) {
		return clusterDefinition.getResourceManager(i);
	}
	
	public static class ClusterBuilder {
		
		private static final Logger log = LoggerFactory.getLogger(ClusterBuilder.class);
		
		private final List<Integer> capacities = Lists.newArrayList(100);
		private final InetAddress host;

		private Scheduler[] schedulers = new Scheduler[1];
		private ResourceManager[] resourceManagers = new ResourceManager[1];
		
		private ClusterBuilder(InetAddress host) {
			this.host = host;
		}

		public ClusterBuilder schedulers(int amount) {
			schedulers = new Scheduler[amount];
			return this;
		}
		
		public ClusterBuilder resourceManagers(int amount, int capacity) {
			capacities.clear();
			for (int i = 0; i < amount; i++) {
				capacities.add(capacity);
			}
			return this;
		}
		
		public ClusterBuilder resourceManager(int capacity) {
			capacities.add(capacity);
			return this;
		}
		
		private void setUp() throws IOException, InterruptedException {
			log.info("Preparing cluster...");
			for (int i = 0; i < schedulers.length; i++) {
				schedulers[i] = new Scheduler(host);
			}
			
			resourceManagers = new ResourceManager[capacities.size()];
			for (int i = 0; i < capacities.size(); i++) {
				resourceManagers[i] = new ResourceManager(host, capacities.get(i));
			}
			
			// Wait for cluster to get fully connected...
			int clusterSize = schedulers.length + resourceManagers.length;
			for (Scheduler scheduler : schedulers) {
				while (scheduler.getNumberOfRemotes() != clusterSize - 1) {
					Thread.sleep(1000);
				}
			}
			for (ResourceManager resourceManager : resourceManagers) {
				while (resourceManager.getNumberOfRemotes() != clusterSize - 1) {
					Thread.sleep(1000);
				}
			}
			log.info("Cluster initialized of size: " + clusterSize + " and fully connected!");
		}
		
		private void tearDown() {
			for (Scheduler scheduler : schedulers) {
				scheduler.die();
			}
			for (ResourceManager resourceManager : resourceManagers) {
				resourceManager.die();
			}
		}

		private Scheduler getScheduler(int i) {
			return schedulers[i];
		}

		private ResourceManager getResourceManager(int i) {
			return resourceManagers[i];
		}
		
	}
	
}
