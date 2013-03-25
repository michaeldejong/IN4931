package nl.tudelft.in4931;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nl.tudelft.in4931.models.AttackAction;
import nl.tudelft.in4931.models.MoveAction;
import nl.tudelft.in4931.models.Participant;
import nl.tudelft.in4931.models.Participant.Type;
import nl.tudelft.in4931.models.Position;
import nl.tudelft.in4931.network.Address;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DragonClient extends Client implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(DragonClient.class);
	
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

	private ScheduledFuture<?> future;
	
	public DragonClient(InetAddress address, String name, Address server) throws IOException {
		super(address, Type.DRAGON, name);
		
		setServer(server);
	}
	
	@Override
	public void die() {
		executor.shutdownNow();
		super.die();
	}

	public void start() {
		join();
		future = executor.scheduleWithFixedDelay(this, 0, 1000, TimeUnit.MILLISECONDS);
	}

	@Override
	public void run() {
		if (getGameState().getByName(getName()).getKey().getHp() == 0) {
			// I died!
			future.cancel(false);
			return;
		}
		
		Entry<Participant, Position> closestPlayer = locateClosestPlayer();
		if (closestPlayer == null) {
			// No players left.
			return;
		}
		
		Position myPosition = getPosition();
		if (closestPlayer.getValue().distance(myPosition) <= 2) {
			log.info("{} - Player is within 2 distance -> Attack...", getLocalAddress());
			send(new AttackAction(null, closestPlayer.getKey().getName()), getServer());
		}
		else {
			log.info("{} - Moving towards closest player... ({} -> {})", getLocalAddress(), myPosition, closestPlayer.getValue());
			send(new MoveAction(null, myPosition.moveTo(closestPlayer.getValue())), getServer());
		}
	}

	private Entry<Participant, Position> locateClosestPlayer() {
		Entry<Participant, Position> closest = null;
		double currentDistance = Double.MAX_VALUE;
		
		for (Entry<Participant, Position> entry : getGameState().getParticipants().entrySet()) {
			if (entry.getKey().getType() == Type.PLAYER && entry.getKey().getHp() > 0) {
				double entryDistance = getPosition().distance(entry.getValue());
				if (closest == null || currentDistance > entryDistance) {
					closest = entry;
					currentDistance = entryDistance;
				}
			}
		}
		
		return closest;
	}

}
