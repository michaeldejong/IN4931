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
		
		long time = 0;
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
			participants.put(joinAction.getParticipant(), Position.randomPosition(WIDTH, HEIGHT));
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

	public Map<Participant, Position> getParticipants() {
		return Collections.unmodifiableMap(participants);
	}
	
}
