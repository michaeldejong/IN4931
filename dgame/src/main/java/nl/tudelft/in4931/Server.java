package nl.tudelft.in4931;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

import nl.tudelft.in4931.models.Action;
import nl.tudelft.in4931.models.GameState;
import nl.tudelft.in4931.models.GameStates;
import nl.tudelft.in4931.models.GameStates.Listener;
import nl.tudelft.in4931.models.ParticipantJoinedAction;
import nl.tudelft.in4931.network.Address;
import nl.tudelft.in4931.network.Handler;
import nl.tudelft.in4931.network.Role;
import nl.tudelft.in4931.network.TopologyAwareNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public class Server extends TopologyAwareNode implements Listener {
	
	private static final Logger log = LoggerFactory.getLogger(Server.class);

	private final Map<Address, String> participants = Maps.newHashMap();
	
	private final GameStates gameStates;
	
	public Server(InetAddress address) throws IOException {
		super(address, Role.SERVER);
		log.info("{} - Started server...", getLocalAddress());
		
		gameStates = new GameStates();
		gameStates.addListener(this);
		
		registerMessageHandlers();
	}
	
	private void registerMessageHandlers() {
		on(Action.class, new Handler<Action>() {
			@Override
			public void onMessage(Action action, Address origin) {
				log.info("{} - Received action: {} from: {}", getLocalAddress(), action, origin);
				
				boolean fromServer = true;
				if (action.getTime() == null) {
					action.setTime(System.currentTimeMillis());
					fromServer = false;
				}
				
				if (!fromServer && action instanceof ParticipantJoinedAction) {
					log.info("{} - Added client: {}", getLocalAddress(), origin);
					participants.put(origin, action.getParticipant().getName());
				}
				
				gameStates.onAction(action);
				if (!fromServer) {
					multicast(action, getServers());
				}
			}
		});
	}

	@Override
	public void onUpdate(GameState state) {
		log.info("{} - Game state changed: multicasting to clients...", getLocalAddress());
		multicast(state, participants.keySet());
	}

}
