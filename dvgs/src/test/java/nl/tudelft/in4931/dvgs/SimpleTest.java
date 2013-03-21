package nl.tudelft.in4931.dvgs;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Iterator;

import nl.tudelft.in4931.dvgs.network.Address;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Joiner;

public class SimpleTest {

	private static final int NODE_BOOT_TIME = 1000;
	private static final int DISCOVERY_TIME = 10000;

	@Test
	public void testSimpleNetworkConnection() throws IOException, InterruptedException {
		InetAddress localHost = InetAddress.getByName("127.0.0.1");
		Scheduler scheduler = new Scheduler(localHost);
		ResourceManager resourceManager = new ResourceManager(localHost, 1);
		
		Thread.sleep(NODE_BOOT_TIME);
		
		scheduler.die();
		resourceManager.die();
	}

	@Test
	public void testSchedulersAgePrecedence() throws IOException, InterruptedException {
		InetAddress localHost = InetAddress.getByName("127.0.0.1");
		Scheduler s1 = new Scheduler(localHost);
		Scheduler s2 = new Scheduler(localHost);
		Scheduler s3 = new Scheduler(localHost);
		Scheduler s4 = new Scheduler(localHost);
		
		Thread.sleep(DISCOVERY_TIME);
		
		Collection<Address> schedulers1 = s1.getSchedulers();
		Collection<Address> schedulers2 = s2.getSchedulers();
		Collection<Address> schedulers3 = s3.getSchedulers();
		Collection<Address> schedulers4 = s4.getSchedulers();

		assertSchedulers(schedulers1, s1.getLocalAddress(), s2.getLocalAddress(), s3.getLocalAddress(), s4.getLocalAddress());
		assertSchedulers(schedulers2, s1.getLocalAddress(), s2.getLocalAddress(), s3.getLocalAddress(), s4.getLocalAddress());
		assertSchedulers(schedulers3, s1.getLocalAddress(), s2.getLocalAddress(), s3.getLocalAddress(), s4.getLocalAddress());
		assertSchedulers(schedulers4, s1.getLocalAddress(), s2.getLocalAddress(), s3.getLocalAddress(), s4.getLocalAddress());
		
		s1.die();
		s2.die();
		s3.die();
		s4.die();
	}

	@Test(timeout = 30000)
	public void testSchedulersAgePrecedenceWithSlowStart() throws IOException, InterruptedException {
		InetAddress localHost = InetAddress.getByName("127.0.0.1");
		Scheduler s1 = new Scheduler(localHost);
		Thread.sleep(NODE_BOOT_TIME);

		Scheduler s2 = new Scheduler(localHost);
		Thread.sleep(NODE_BOOT_TIME);
		
		Scheduler s3 = new Scheduler(localHost);
		Thread.sleep(NODE_BOOT_TIME);
		
		Scheduler s4 = new Scheduler(localHost);
		Thread.sleep(DISCOVERY_TIME);
		
		Collection<Address> schedulers1 = s1.getSchedulers();
		Collection<Address> schedulers2 = s2.getSchedulers();
		Collection<Address> schedulers3 = s3.getSchedulers();
		Collection<Address> schedulers4 = s4.getSchedulers();

		assertSchedulers(schedulers1, s1.getLocalAddress(), s2.getLocalAddress(), s3.getLocalAddress(), s4.getLocalAddress());
		assertSchedulers(schedulers2, s1.getLocalAddress(), s2.getLocalAddress(), s3.getLocalAddress(), s4.getLocalAddress());
		assertSchedulers(schedulers3, s1.getLocalAddress(), s2.getLocalAddress(), s3.getLocalAddress(), s4.getLocalAddress());
		assertSchedulers(schedulers4, s1.getLocalAddress(), s2.getLocalAddress(), s3.getLocalAddress(), s4.getLocalAddress());
		
		s1.die();
		s2.die();
		s3.die();
		s4.die();
	}

	@Test
	public void testMasterCheck() throws IOException, InterruptedException {
		InetAddress localHost = InetAddress.getByName("127.0.0.1");
		Scheduler s1 = new Scheduler(localHost);
		Scheduler s2 = new Scheduler(localHost);
		
		Thread.sleep(NODE_BOOT_TIME);
		
		Assert.assertTrue(s1.isMasterScheduler());
		Assert.assertFalse(s2.isMasterScheduler());
		
		Thread.sleep(NODE_BOOT_TIME);
		
		s1.die();
		
		Thread.sleep(DISCOVERY_TIME);
		Assert.assertTrue(s2.isMasterScheduler());
		
		s2.die();
	}
	
	private void assertSchedulers(Collection<Address> schedulers, Address... expected) {
		if (schedulers.size() != expected.length) {
			throw new AssertionError("Unequal sizes: " + Joiner.on(",").join(schedulers) + " and " + Joiner.on(",").join(expected));
		}
		
		int i = 0;
		Iterator<Address> iter = schedulers.iterator();
		while (iter.hasNext()) {
			Address next = iter.next();
			Address expect = expected[i];
			Assert.assertEquals(expect, next);
			i++;
		}
	}
	
}
