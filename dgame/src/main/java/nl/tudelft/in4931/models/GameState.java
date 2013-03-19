package nl.tudelft.in4931.models;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import nl.tudelft.in4931.network.Message;

import com.google.common.collect.Maps;

public class GameState implements Message {
	
	private static final long serialVersionUID = 765554451275918821L;
	
	public static final int WIDTH = 25;
	public static final int HEIGHT = 25;

	public static GameState from(Iterator<Action> iterator) {
		return from(null, iterator);
	}

	public static GameState from(GameState previousState, Iterator<Action> iterator) {
		GameState state = previousState == null ? new GameState() : previousState.copy();
		
		long time = state.getTime();
		while (iterator.hasNext()) {
			Action action = iterator.next();
			time = action.getTime();
			state.process(action);
		}
		
		state.setTime(time);
		return state;
		
	}
	
	private final Map<Participant, Position> participants;
	private long time = 0;
	
	private GameState() {
		this.participants = Maps.newHashMap();
	}
	
	public long getTime() {
		return time;
	}
	
	private void setTime(long time) {
		this.time = time;
	}
	
	private void process(Action action) {
		if (action instanceof ParticipantJoinedAction) {
			ParticipantJoinedAction joinAction = (ParticipantJoinedAction) action;
			Participant participant = joinAction.getType().create(joinAction.getName());
			participants.put(participant, joinAction.getPosition());
		}
		else if (action instanceof HealAction) {
			HealAction healAction = (HealAction) action;
			Entry<Participant, Position> entry = getByName(healAction.getTarget());
			Entry<Participant, Position> self = getByName(action.getParticipant());
			if (!entry.getKey().getName().equals(self.getKey().getName()) && entry.getValue().distance(self.getValue()) <= 5) {
				Participant newParticipant = entry.getKey().deltaHp(self.getKey().getAp());
				if (newParticipant.getHp() > 20) {
					newParticipant = newParticipant.setHp(20);
				}
				participants.remove(entry.getKey());
				participants.put(newParticipant, entry.getValue());
			}
		}
		else if (action instanceof AttackAction) {
			AttackAction attackAction = (AttackAction) action;
			Entry<Participant, Position> entry = getByName(attackAction.getTarget());
			Entry<Participant, Position> self = getByName(action.getParticipant());
			if (!entry.getKey().getName().equals(self.getKey().getName()) && entry.getValue().distance(self.getValue()) <= 2) {
				Participant newParticipant = entry.getKey().deltaHp(-1 * self.getKey().getAp());
				if (newParticipant.getHp() < 0) {
					newParticipant = newParticipant.setHp(0);
				}
				participants.remove(entry.getKey());
				
				if (newParticipant.getHp() > 0) {
					participants.put(newParticipant, entry.getValue());
				}
			}
		}
		else if (action instanceof MoveAction) {
			MoveAction moveAction = (MoveAction) action;
			Entry<Participant, Position> self = getByName(action.getParticipant());
			Position newPosition = self.getValue().moveTo(moveAction.getDirection());
			if (!participants.containsValue(newPosition)) {
				participants.put(self.getKey(), newPosition);
			}
		}
	}

	private GameState copy() {
		GameState state = new GameState();
		state.setTime(time);
		
		for (Entry<Participant, Position> participant : participants.entrySet()) {
			Participant entity = participant.getKey().copy();
			state.participants.put(entity, participant.getValue());
		}
		
		return state;
	}
	
	public Entry<Participant, Position> getByName(String name) {
		for (Entry<Participant, Position> entry : participants.entrySet()) {
			if (entry.getKey().getName().equals(name)) {
				return entry;
			}
		}
		return null;
	}

	public Entry<Participant, Position> getByPosition(Position position) {
		for (Entry<Participant, Position> entry : participants.entrySet()) {
			if (entry.getValue().equals(position)) {
				return entry;
			}
		}
		return null;
	}

	public Map<Participant, Position> getParticipants() {
		return Collections.unmodifiableMap(participants);
	}
	
	@Override
	public String toString() {
		return "[GameState time: " + time + " participants(" + participants.size() + "): " + toString(participants) + "]";
	}

	private String toString(Map<Participant, Position> participants) {
		StringBuilder builder = new StringBuilder();
		for (Entry<Participant, Position> entry : participants.entrySet()) {
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(entry.getKey() + "=" + entry.getValue());
		}
		return builder.toString();
	}
	
}
