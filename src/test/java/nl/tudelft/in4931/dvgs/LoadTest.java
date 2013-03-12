package nl.tudelft.in4931.dvgs;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import nl.tudelft.in4931.dvgs.models.Job;
import nl.tudelft.in4931.dvgs.models.Jobs;
import nl.tudelft.in4931.dvgs.network.JobState;
import nl.tudelft.in4931.dvgs.network.JobState.State;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class LoadTest extends LocalTest {
	
	private static final Logger log = LoggerFactory.getLogger(LoadTest.class);
	
	public LoadTest() throws IOException {
		super(defineCluster()
				.schedulers(1)
				.resourceManagers(4, 100));
	}

	@Test
	public void performLoadTest() throws InterruptedException {
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
		for (int i = 0; i < 4; i++) {
			List<Job> jobs = Lists.newArrayList();
			for (int j = 0; j < 500; j++) {
				jobs.add(new Job(x++, 5000));
			}
			
			getResourceManager(i%4).offerJob(Jobs.of(jobs), false);
		}
		
		getResourceManager(0).die();
		getResourceManager(1).die();
		getResourceManager(2).die();
		
		while (counter.get() != 2000) {
			Thread.sleep(100);
		}
	}
	
}
