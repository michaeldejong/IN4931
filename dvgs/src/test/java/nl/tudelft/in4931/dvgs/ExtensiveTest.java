package nl.tudelft.in4931.dvgs;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import nl.tudelft.in4931.dvgs.models.Job;
import nl.tudelft.in4931.dvgs.network.JobState;
import nl.tudelft.in4931.dvgs.network.JobState.State;

import org.junit.Test;

public class ExtensiveTest extends LocalTest {

	public ExtensiveTest() throws IOException {
		super(defineCluster()
				.schedulers(2)
				.resourceManager(1)
				.resourceManager(1));
	}

	@Test(timeout = 30000)
	public void testSimpleJobSchedulingAndConfirmation() throws InterruptedException {
		final AtomicBoolean processed = new AtomicBoolean();
		getScheduler(1).addListener(new JobListener() {
			@Override
			public void onJobState(JobState job) {
				if (job.getState() == State.FINISHED) {
					processed.set(true);
				}
			}
		});
		
		getResourceManager(0).offerJob(new Job(1, 5000), false);	 		// R1 will execute this job.
		
		while (!processed.get()) { }
	}

	@Test(timeout = 300000000)
	public void testDoubleJobScheduling() throws InterruptedException {
		final AtomicInteger processed = new AtomicInteger();
		getScheduler(1).addListener(new JobListener() {
			@Override
			public void onJobState(JobState job) {
				if (job.getState() == State.FINISHED) {
					processed.addAndGet(job.numberOfJobs());
				}
			}
		});
		
		getResourceManager(0).offerJob(new Job(1, 5000), false);	 		// R1 will take this job.
		getResourceManager(0).offerJob(new Job(2, 5000), false);	 		// S1 should receive a JobState object, and assign the job to R2.
		
		while (processed.get() < 2) { }
	}
	
}
