package nl.tudelft.in4931;


import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import nl.tudelft.in4931.models.Participant.Type;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class IntermediateConnectionTest {

	private Server server1, server2;
	private Client client1, client2, client3;
	
	private ScheduledThreadPoolExecutor executor;
	
	@Before
	public void setUp() throws IOException, InterruptedException {
		executor = new ScheduledThreadPoolExecutor(10);
		
		InetAddress local = InetAddress.getByName("127.0.0.1");
		server1 = new Server(local);
		server2 = new Server(local);
		client1 = new Client(local, Type.PLAYER, "Client #1");
		client2 = new Client(local, Type.PLAYER, "Client #2");
		client3 = new Client(local, Type.PLAYER, "Client #3");
		
		server1.setServers(Sets.newHashSet(server2.getLocalAddress()));
		server2.setServers(Sets.newHashSet(server1.getLocalAddress()));
		
		client1.setServer(server1.getLocalAddress());
		client2.setServer(server1.getLocalAddress());
		client3.setServer(server2.getLocalAddress());
		
		Thread.sleep(1000);
	}
	
	@Test
	public void testThatClientsCanLogOntoGame() throws InterruptedException {
		join(client1, client2, client3);
		
		Thread.sleep(100);
		
		Assert.assertEquals(3, client1.getGameState().getParticipants().size());
		Assert.assertEquals(3, client2.getGameState().getParticipants().size());
		Assert.assertEquals(3, client3.getGameState().getParticipants().size());
	}
	
	public void join(Client... clients) {
		List<Future<?>> futures = Lists.newArrayList();
		for (final Client client : clients) {
			futures.add(executor.submit(new Runnable() {
				@Override
				public void run() {
					client.join();
				}
			}));
		}
		
		while (!futures.isEmpty()) {
			Future<?> future = futures.get(0);
			if (future.isDone() || future.isCancelled()) {
				futures.remove(0);
			}
		}
	}
	
}
