package nl.tudelft.in4931;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import nl.tudelft.in4931.models.Action;
import nl.tudelft.in4931.models.GameState;
import nl.tudelft.in4931.models.Participant;
import nl.tudelft.in4931.models.ParticipantJoinedAction;
import nl.tudelft.in4931.models.Position;
import nl.tudelft.in4931.network.Address;
import nl.tudelft.in4931.network.Handler;
import nl.tudelft.in4931.network.NodeWithHandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class Client extends NodeWithHandlers {
	
	private static final Logger log = LoggerFactory.getLogger(Client.class);

	private final Participant.Type type;
	private final AtomicReference<GameState> gameState;
	private final List<Listener> listeners = Lists.newArrayList();
	private final String name;
	
	private Address server;
	private Position initialPosition = null;

	public Client(InetAddress address, Participant.Type type, String name) throws IOException {
		super(address);
		log.info("{} - Started client...", getLocalAddress());
		
		this.gameState = new AtomicReference<>();
		this.type = type;
		this.name = name;
		
		registerMessageHandlers();
	}

	public void setInitialPosition(Position initialPosition) {
		this.initialPosition = initialPosition;
	}

	public String getName() {
		return name;
	}
	
	public void registerListener(Listener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}
	
	private void registerMessageHandlers() {
		on(GameState.class, new Handler<GameState>() {
			@Override
			public void onMessage(GameState state, Address origin) {
				log.debug("{} - Received new game state: {}", getLocalAddress(), state.getTime());
				gameState.set(state);
				
				synchronized (listeners) {
					for (Listener listener : listeners) {
						listener.onGameState(state);
					}
				}
			}
		});
	}
	
	public void setServer(Address server) {
		this.server = server;
	}

	protected Address getServer() {
		return server;
	}
	
	public Position getPosition() {
		for (Entry<Participant, Position> entry : getGameState().getParticipants().entrySet()) {
			if (entry.getKey().getName().equals(name)) {
				return entry.getValue();
			}
		}
		return null;
	}
	
	public void join() {
		ParticipantJoinedAction message = new ParticipantJoinedAction(null, type, name);
		message.setPosition(initialPosition);
		
		log.debug("{} - Sending join action: {} to server: {}", getLocalAddress(), message, server);
		send(message, server);
		
		while (gameState.get() == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				log.warn(e.getMessage(), e);
			}
		}
	}
	
	public void doAction(Action action) {
		send(action, server);
	}
	
	protected GameState getGameState() {
		return gameState.get();
	}
	
	@Override
	public String toString() {
		return "[Client: " + name + "]";
	}
	
	public interface Listener {
		void onGameState(GameState state);
	}

	@Override
	protected void onConnectionLost(Address remote) {
		die();
	}

}
