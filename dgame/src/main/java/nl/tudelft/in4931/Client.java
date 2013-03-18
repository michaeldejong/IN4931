package nl.tudelft.in4931;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicReference;

import nl.tudelft.in4931.models.Action;
import nl.tudelft.in4931.models.GameState;
import nl.tudelft.in4931.models.Participant;
import nl.tudelft.in4931.models.ParticipantJoinedAction;
import nl.tudelft.in4931.network.Address;
import nl.tudelft.in4931.network.Handler;
import nl.tudelft.in4931.network.NodeWithHandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client extends NodeWithHandlers {
	
	private static final Logger log = LoggerFactory.getLogger(Client.class);

	private final Participant.Type type;
	private final AtomicReference<GameState> gameState;
	private final String name;
	
	private Address server;

	public Client(InetAddress address, Participant.Type type, String name) throws IOException {
		super(address);
		log.info("{} - Started client...", getLocalAddress());
		
		this.gameState = new AtomicReference<>();
		this.type = type;
		this.name = name;
		
		registerMessageHandlers();
	}
	
	private void registerMessageHandlers() {
		on(GameState.class, new Handler<GameState>() {
			@Override
			public void onMessage(GameState state, Address origin) {
				log.info("{} - Received new game state: {}", getLocalAddress(), state);
				gameState.set(state);
			}
		});
	}
	
	public void setServer(Address server) {
		this.server = server;
	}
	
	public void join() {
		ParticipantJoinedAction message = new ParticipantJoinedAction(null, type.create(name));
		log.info("{} - Sending join action: {} to server: {}", getLocalAddress(), message, server);
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

}
