package nl.tudelft.in4931;


import java.io.IOException;
import java.net.InetAddress;

import nl.tudelft.in4931.models.Participant.Type;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

public class SimpleConnectionTest {

	private Server server1, server2;
	private Client client1, client2;
	
	@Before
	public void setUp() throws IOException, InterruptedException {
		InetAddress local = InetAddress.getByName("127.0.0.1");
		server1 = new Server(local);
		server2 = new Server(local);
		client1 = new Client(local, Type.PLAYER, "Client #1");
		client2 = new Client(local, Type.PLAYER, "Client #2");
		
		server1.setServers(Sets.newHashSet(server2.getLocalAddress()));
		server2.setServers(Sets.newHashSet(server1.getLocalAddress()));
		
		client1.setServer(server1.getLocalAddress());
		client2.setServer(server2.getLocalAddress());
		
		Thread.sleep(1000);
	}
	
	@Test
	public void testThatClientsCanLogOntoGame() throws InterruptedException {
		client1.join();
		Assert.assertEquals(1, client1.getGameState().getParticipants().size());

		client2.join();
		Thread.sleep(100);
		
		Assert.assertEquals(2, client1.getGameState().getParticipants().size());
		Assert.assertEquals(2, client2.getGameState().getParticipants().size());
	}
	
}
