package nl.tudelft.in4931;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

import nl.tudelft.in4931.models.Action;
import nl.tudelft.in4931.models.GameState;
import nl.tudelft.in4931.models.GameStates;
import nl.tudelft.in4931.models.GameStates.Listener;
import nl.tudelft.in4931.models.ParticipantJoinedAction;
import nl.tudelft.in4931.models.Position;
import nl.tudelft.in4931.network.Address;
import nl.tudelft.in4931.network.Handler;
import nl.tudelft.in4931.network.NodeWithHandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Server extends NodeWithHandlers implements Listener {
	
	private static final Logger log = LoggerFactory.getLogger(Server.class);
	
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

	private final Set<Address> servers = Sets.newHashSet();
	private final Map<Address, String> participants = Maps.newHashMap();
	private final AtomicLong time = new AtomicLong(0);
	private final GameStates gameStates;
	
	private final Object lock = new Object();
	
	public Server(InetAddress address) throws IOException {
		super(address);
		log.info("{} - Started server...", getLocalAddress());
		
		gameStates = new GameStates();
		gameStates.addListener(this);
		
		registerMessageHandlers();
	}
	
	public void registerListener(Listener listener) {
		synchronized (lock) {
			gameStates.addListener(listener);
		}
	}
	
	public void setServers(Set<Address> addresses) {
		synchronized (servers) {
			servers.clear();
			servers.addAll(addresses);
		}
	}
	
	private void registerMessageHandlers() {
		on(Action.class, new Handler<Action>() {
			@Override
			public void onMessage(final Action action, final Address origin) {
				log.debug("{} - Received action: {} from: {}", getLocalAddress(), action, origin);
				synchronized (lock) {
					executor.submit(new Runnable() {
						@Override
						public void run() {
							boolean fromServer = true;
							if (action.getTime() == null) {
								action.setTime(time.incrementAndGet());
								action.setParticipant(participants.get(origin));
								fromServer = false;
							}
							else {
								synchronized (time) {
									time.set(Math.max(time.get(), action.getTime() + 1));
								}
							}
							
							if (!fromServer && action instanceof ParticipantJoinedAction) {
								ParticipantJoinedAction joinAction = (ParticipantJoinedAction) action;
								if (joinAction.getPosition() == null) {
									joinAction.setPosition(Position.randomPosition(GameState.WIDTH, GameState.HEIGHT));
								}
								
								log.debug("{} - Added client: {}", getLocalAddress(), origin);
								participants.put(origin, joinAction.getName());
							}
							
							gameStates.onAction(action);
							if (!fromServer) {
								multicast(action, servers);
							}
						}
					});
				}
			}
		});
	}

	@Override
	public void onGameState(GameState state) {
		Set<Address> clients = participants.keySet();
		log.debug("{} - Sending state: {} to clients: {}", getLocalAddress(), state, Joiner.on(", ").join(clients));
		multicast(state, clients);
	}

}
