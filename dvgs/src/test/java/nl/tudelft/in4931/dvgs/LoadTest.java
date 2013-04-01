package nl.tudelft.in4931.dvgs;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import nl.tudelft.in4931.dvgs.models.Job;
import nl.tudelft.in4931.dvgs.models.Jobs;
import nl.tudelft.in4931.dvgs.network.JobState;
import nl.tudelft.in4931.dvgs.network.JobState.State;

import org.junit.Test;

import com.google.common.collect.Lists;

public class LoadTest extends LocalTest {
	
	public LoadTest() throws IOException {
		super(defineCluster()
				.schedulers(5)
				.resourceManagers(1, 10));
	}

	@Test
	public void performLoadTest() throws InterruptedException, IOException {
		final AtomicInteger counter = new AtomicInteger();
		getScheduler(0).addListener(new JobListener() {
			@Override
			public void onJobState(JobState jobState) {
				if (jobState.getState() == State.FINISHED) {
					counter.addAndGet(jobState.numberOfJobs());
				}
			}
		});
		
		Thread.sleep(100);
		
		int x = 0;
		for (int i = 0; i < 20; i++) {
			List<Job> jobs = Lists.newArrayList();
			for (int j = 0; j < 500; j++) {
				jobs.add(new Job(x++, 10000));
			}
			
			getResourceManager(0).offerJob(Jobs.of(jobs), false);
		}
		
		Thread.sleep(5000);
		
		ResourceManager resourceManager = new ResourceManager(InetAddress.getByName("127.0.0.1"), 1000);

		while (counter.get() != 10000) {
			Thread.sleep(100);
		}
	}
	
}
