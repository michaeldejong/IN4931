package nl.tudelft.in4931;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import nl.tudelft.in4931.Client.Listener;
import nl.tudelft.in4931.models.GameState;
import nl.tudelft.in4931.network.Address;
import nl.tudelft.in4931.network.SubNetworkIterator;
import nl.tudelft.in4931.ui.Board;
import nl.tudelft.in4931.ui.Board.BoardListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class Main {

	private static final Logger log = LoggerFactory.getLogger(Main.class);
	
	private static Server server;
	private static DragonClient dragon;
	private static PlayerClient player;

	private static Board board;

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length == 0) {
			System.out.println("{IP|network} --{player|dragon} {serverIP}");
			System.out.println("{IP|network} --{server} {serverPeerIPs}...");
			return;
		}

		InetAddress address = configureAddress(args);
		if (args[1].equals("--dragon")) {
			Address serverAddress = getServerAddress(args);
			dragon = new DragonClient(address, "Dragon #" + System.nanoTime(), serverAddress);
			startGui(null, dragon);
			dragon.start();
		}
		else if (args[1].equals("--player")) {
			Address server = getServerAddress(args);
			player = new PlayerClient(address, "Player #" + System.nanoTime(), server);
			startGui(null, player);
			player.start();
		}
		else if (args[1].equals("--server")) {
			server = new Server(address);
			server.setServers(parseServers(args));
		}
	}
	
	private static void startGui(BoardListener listener, Client client) {
		board = new Board(listener);

		final AtomicLong last = new AtomicLong();
		client.registerListener(new Listener() {
			@Override
			public void onGameState(GameState state) {
				if (last.get() + 100 < System.currentTimeMillis()) {
					board.update(state);
					last.set(System.currentTimeMillis());
				}
			}
		});
	}

	private static Set<Address> parseServers(String[] args) {
		Set<Address> servers = Sets.newHashSet();
		for (int i = 2; i < args.length; i++) {
			servers.add(Address.parse(args[i]));
		}
		return servers;
	}

	private static Address getServerAddress(String[] args) throws IOException {
		if (args.length >= 3) {
			return Address.parse(args[2]);
		}
		return new Address("127.0.0.1", SubNetworkIterator.PORTS.lowerEndpoint());
	}

	private static InetAddress configureAddress(String[] args) throws IOException {
		if (args.length >= 1) {
			String networkInterfaceName = args[0];
			if (networkInterfaceName.contains(".")) {
				InetAddress element = InetAddress.getByName(networkInterfaceName);
				log.info("Selected IP address: {}", element.getHostAddress());
				System.setProperty("java.rmi.server.hostname", element.getHostAddress());
				return element;
			}
			else {
				NetworkInterface networkInterface = NetworkInterface.getByName(networkInterfaceName);
				Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
				
				while (inetAddresses.hasMoreElements()) {
					InetAddress element = inetAddresses.nextElement();
					if (element instanceof Inet4Address) {
						log.info("Selected IP address: {}", element.getHostAddress());
						System.setProperty("java.rmi.server.hostname", element.getHostAddress());
						return element;
					}
				}
			}
		}
		
		InetAddress localAddress = InetAddress.getByName("127.0.0.1");
		log.info("Selected IP address: {}", localAddress.getHostAddress());
		return localAddress;
	}

}
