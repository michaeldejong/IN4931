package nl.tudelft.in4931.models;

import java.util.Map;

import com.google.common.collect.Maps;

public class GameState {

	private final Map<Participant, Position> participants;
	
	public GameState() {
		this.participants = Maps.newHashMap();
	}
	
	public void addParticipant(Participant partipant) {
		participants.put(partipant, Position.randomPosition(25, 25));
	}
	
}
