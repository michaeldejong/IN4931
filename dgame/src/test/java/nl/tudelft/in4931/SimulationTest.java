package nl.tudelft.in4931;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import nl.tudelft.in4931.Client.Listener;
import nl.tudelft.in4931.models.GameState;
import nl.tudelft.in4931.models.Position;
import nl.tudelft.in4931.network.Address;
import nl.tudelft.in4931.ui.Board;
import nl.tudelft.in4931.ui.Board.BoardListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class SimulationTest {
	
	private static final Logger log = LoggerFactory.getLogger(SimulationTest.class);
	
	private static final int PLAYERS = 20;
	private static final int DRAGONS = 2;
	private static final int SERVERS = 3;

	private List<Server> servers;
	private List<DragonClient> dragons;
	private List<PlayerClient> clients;

	private Board board;
	
	@Before
	public void setUp() throws IOException, InterruptedException {
		InetAddress local = InetAddress.getLocalHost();
		
		final Set<Address> serverAddresses = Sets.newHashSet();
		servers = Lists.newArrayList();
		for (int i = 0; i < SERVERS; i++) {
			Server server = new Server(local);
			serverAddresses.add(server.getLocalAddress());
			servers.add(server);
		}
		
		for (Server server : servers) {
			Set<Address> otherServers = Sets.newHashSet();
			otherServers.addAll(serverAddresses);
			otherServers.remove(server.getLocalAddress());
			server.setServers(otherServers);
		}
		
		dragons = Lists.newArrayList();
		for (int i = 0; i < DRAGONS; i++) {
			Address server = servers.get(i%SERVERS).getLocalAddress();
			dragons.add(new DragonClient(local, "Dragon #" + i, server));
		}
		
		clients = Lists.newArrayList();
		for (int i = 0; i < PLAYERS; i++) {
			Address server = servers.get(i%SERVERS).getLocalAddress();
			clients.add(new PlayerClient(local, "Player #" + i, server));
		}
		
		board = new Board(new BoardListener() {
			@Override
			public void spawnDragon(final int x, final int y) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Address server = servers.get(dragons.size()%SERVERS).getLocalAddress();
							DragonClient client = new DragonClient(InetAddress.getLocalHost(), "Dragon #" + dragons.size(), server);
							client.setInitialPosition(new Position(x, y));
							dragons.add(client);
							client.start();
						}
						catch (IOException e) {
							log.warn("Could not spawn dragon", e);
						}
					}
				}).start();
			}
		});
		
		final AtomicLong last = new AtomicLong();
		dragons.get(0).registerListener(new Listener() {
			@Override
			public void onGameState(GameState state) {
				if (last.get() + 100 < System.currentTimeMillis()) {
					board.update(state);
					last.set(System.currentTimeMillis());
				}
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
		final Set<String> killedDragons = Sets.newHashSet();
		
		for (DragonClient dragon : dragons) {
			dragon.start();
			awaitDragonDeath(dragon, killedDragons);
		}

		for (PlayerClient client : clients) {
			client.start();
		}
		
		while (true) {
			int killed = 0;
			synchronized (killedDragons) {
				killed = killedDragons.size();
			}
			
			Thread.sleep(100);
			if (killed == dragons.size()) {
				break;
			}
		}
	}

	private void awaitDragonDeath(final DragonClient client, final Set<String> killedDragons) {
		client.registerListener(new Listener() {
			@Override
			public void onGameState(GameState state) {
				synchronized (killedDragons) {
					if (killedDragons.contains(client.getName())) {
						return;
					}
					
					int myHp = state.getByName(client.getName()).getKey().getHp();
					if (myHp == 0) {
						log.info("{} - Dragon died!", client.getLocalAddress());
						killedDragons.add(client.getName());
					}
					else {
						log.info("{} - Dragon HP is: {}", client.getLocalAddress(), myHp);
					}
				}
			}
		});
	}
	
}
