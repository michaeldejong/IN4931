package nl.tudelft.in4931.dvgs;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import nl.tudelft.in4931.dvgs.models.Job;
import nl.tudelft.in4931.dvgs.network.JobState;
import nl.tudelft.in4931.dvgs.network.JobState.State;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExtensiveTest {

	private Scheduler s1, s2;
	private ResourceManager r1, r2;
	
	@Before
	public void setUp() throws IOException, InterruptedException {
		InetAddress localHost = InetAddress.getLocalHost();
		s1 = new Scheduler(localHost);
		s2 = new Scheduler(localHost);
		r1 = new ResourceManager(localHost, 1);
		r2 = new ResourceManager(localHost, 1);
		
		Thread.sleep(5000);
	}
	
	@After
	public void tearDown() {
		s1.die();
		s2.die();
		r1.die();
		r2.die();
	}
	
	@Test(timeout = 30000)
	public void testSimpleJobSchedulingAndConfirmation() throws InterruptedException {
		final AtomicBoolean processed = new AtomicBoolean();
		s2.addListener(new JobListener() {
			@Override
			public void onJobState(JobState job) {
				if (job.getState() == State.FINISHED) {
					processed.set(true);
				}
			}
		});
		
		r1.offerJob(new Job(1, 5000));	 		// R1 will execute this job.
		
		while (!processed.get()) { }
	}
	
	@Test(timeout = 30000)
	public void testDoubleJobScheduling() throws InterruptedException {
		final AtomicInteger processed = new AtomicInteger();
		s2.addListener(new JobListener() {
			@Override
			public void onJobState(JobState job) {
				if (job.getState() == State.FINISHED) {
					processed.incrementAndGet();
				}
			}
		});
		
		r1.offerJob(new Job(1, 5000));	 		// R1 will take this job.
		r1.offerJob(new Job(2, 5000));	 		// S1 should receive a JobState object, and assign the job to R2.
		
		while (processed.get() < 2) { }
	}
	
}
