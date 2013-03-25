package nl.tudelft.in4931;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;

import nl.tudelft.in4931.Client.Listener;
import nl.tudelft.in4931.models.GameState;
import nl.tudelft.in4931.models.Position;
import nl.tudelft.in4931.ui.Board;
import nl.tudelft.in4931.ui.Board.BoardListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class DisconnectionTest {
	
	private static final Logger log = LoggerFactory.getLogger(DisconnectionTest.class);
	
	private Board board;
	private Set<String> killedDragons;

	private DragonClient dragon1;
	private DragonClient dragon2;
	private PlayerClient player1;
	private PlayerClient player2;

	private Server server1;
	private Server server2;
	
	@Before
	public void setUp() throws IOException, InterruptedException {
		InetAddress local = InetAddress.getByName("127.0.0.1");

		killedDragons = Sets.newHashSet();
		
		server1 = new Server(local);
		server2 = new Server(local);
		
		server1.setServers(Sets.newHashSet(server2.getLocalAddress()));
		server2.setServers(Sets.newHashSet(server1.getLocalAddress()));

		dragon1 = new DragonClient(local, "D1", server1.getLocalAddress()); 
		dragon2 = new DragonClient(local, "D2", server2.getLocalAddress()); 
		player1 = new PlayerClient(local, "P1", server1.getLocalAddress());
		player2 = new PlayerClient(local, "P2", server2.getLocalAddress());
		
		player1.setInitialPosition(new Position(0, 0));
		
		board = new Board(new BoardListener() {
			@Override
			public void spawnDragon(final int x, final int y) { }
		});
		
		dragon1.registerListener(new Listener() {
			@Override
			public void onGameState(GameState state) {
				board.update(state);
			}
		});
		
		Thread.sleep(1000);
	}
	
	@After
	public void tearDown() {
		board.dispose();
	}
	
	@Test
	public void testThatDragonWillKillPlayer() throws InterruptedException {
		dragon1.start();
		dragon2.start();
		
		player1.start();
		player2.start();
		
		Thread.sleep(2000);
		server2.die();

		Thread.sleep(10000);
	}
	
}
