package nl.tudelft.in4931;


import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import nl.tudelft.in4931.models.Participant.Type;
import nl.tudelft.in4931.network.Address;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ScalingTest {
	
	private static final int SERVERS = 5;
	private static final int CLIENTS = 100;
	private static final Logger log = LoggerFactory.getLogger(ScalingTest.class);
	
	private ScheduledThreadPoolExecutor executor;
	
	@Before
	public void setUp() throws InterruptedException {
		executor = new ScheduledThreadPoolExecutor(10);
	}

	@Test
	public void testThatClientsCanLogOntoGame() throws InterruptedException, IOException {
		long start = System.currentTimeMillis();
		
		Set<Address> serverAddresses = Sets.newHashSet();
		List<Server> servers = Lists.newArrayList();
		for (int i = 0; i < SERVERS; i++) {
			Server server = new Server(InetAddress.getByName("127.0.0.1"));
			serverAddresses.add(server.getLocalAddress());
			servers.add(server);
		}
		
		for (Server server : servers) {
			Set<Address> addresses = Sets.newHashSet();
			addresses.addAll(serverAddresses);
			addresses.remove(server.getLocalAddress());
			server.setServers(addresses);
		}
		
		List<Client> clients = Lists.newArrayList();
		for (int i = 0; i < CLIENTS; i++) {
			Client client = new Client(InetAddress.getByName("127.0.0.1"), Type.PLAYER, "Client #" + i);
			client.setServer(servers.get(i%SERVERS).getLocalAddress());
			clients.add(client);
		}
		
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
		
		Thread.sleep(2000);
		
		for (Client client : clients) {
			Assert.assertEquals(CLIENTS, client.getGameState().getParticipants().size());
		}
		
		long end = System.currentTimeMillis();
		log.info("Took " + (end - start) + "ms to complete network...");
	}
	
}
