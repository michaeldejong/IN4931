package nl.tudelft.in4931;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nl.tudelft.in4931.models.AttackAction;
import nl.tudelft.in4931.models.HealAction;
import nl.tudelft.in4931.models.MoveAction;
import nl.tudelft.in4931.models.MoveAction.Direction;
import nl.tudelft.in4931.models.Participant;
import nl.tudelft.in4931.models.Participant.Type;
import nl.tudelft.in4931.models.Position;
import nl.tudelft.in4931.network.Address;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerClient extends Client implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(DragonClient.class);
	
	private static final ScheduledThreadPoolExecutor exector = new ScheduledThreadPoolExecutor(10);

	private ScheduledFuture<?> future;

	public PlayerClient(InetAddress address, String name, Address server) throws IOException {
		super(address, Type.PLAYER, name);
		
		setServer(server);
	}

	public void start() {
		join();
		synchronized (exector) {
			future = exector.scheduleWithFixedDelay(this, 0, 1000, TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public void run() {
		Entry<Participant, Position> byName = getGameState().getByName(getName());
		if (byName == null) {
			return;
		}
		
		if (byName.getKey().getHp() == 0) {
			future.cancel(false);
			return;
		}
		
		Entry<Participant, Position> closestDragon = locateClosestDragon();
		if (closestDragon == null) {
			return;
		}
		
		Entry<Participant, Position> closestWoundedPlayer = locateClosestWoundedPlayer();
		
		Position myPosition = getPosition();
		
		if (closestWoundedPlayer != null && closestWoundedPlayer.getValue().distance(myPosition) <= 5 && Math.random() > 0.5) {
			log.info("{} - Wounded player is within 5 distance -> Heal...", getLocalAddress());
			send(new HealAction(null, closestWoundedPlayer.getKey().getName()), getServer());
		}
		else if (closestDragon.getValue().distance(myPosition) <= 2) {
			log.info("{} - Dragon is within 2 distance -> Attack...", getLocalAddress());
			send(new AttackAction(null, closestDragon.getKey().getName()), getServer());
		}
		else {
			log.info("{} - Moving towards closest dragon... ({} -> {})", getLocalAddress(), myPosition, closestDragon.getValue());
			Direction direction = myPosition.moveTo(closestDragon.getValue());
			if (getGameState().getByPosition(myPosition.moveTo(direction)) == null) {
				send(new MoveAction(null, direction), getServer());
			}
			else {
				send(new MoveAction(null, Direction.random()), getServer());
			}
		}
	}

	private Entry<Participant, Position> locateClosestDragon() {
		Entry<Participant, Position> closest = null;
		double currentDistance = Double.MAX_VALUE;
		
		for (Entry<Participant, Position> entry : getGameState().getParticipants().entrySet()) {
			if (entry.getKey().getType() == Type.DRAGON) {
				double entryDistance = getPosition().distance(entry.getValue());
				if (closest == null || currentDistance > entryDistance) {
					closest = entry;
					currentDistance = entryDistance;
				}
			}
		}
		
		return closest;
	}

	private Entry<Participant, Position> locateClosestWoundedPlayer() {
		Entry<Participant, Position> closest = null;
		int currentDistance = Integer.MAX_VALUE;
		
		Set<Entry<Participant, Position>> entrySet = getGameState().getParticipants().entrySet();
		for (Entry<Participant, Position> entry : entrySet) {
			if (entry.getKey().getName().equals(getName())) {
				continue;
			}
			
			int hp = entry.getKey().getHp();
			if (entry.getKey().getType() == Type.PLAYER && hp < 20 && hp > 0) {
				int entryDistance = getPosition().distance(entry.getValue());
				if (closest == null || currentDistance > entryDistance) {
					closest = entry;
					currentDistance = entryDistance;
				}
			}
		}
		
		return closest;
	}

}
