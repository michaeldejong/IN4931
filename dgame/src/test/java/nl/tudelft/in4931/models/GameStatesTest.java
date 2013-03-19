package nl.tudelft.in4931.models;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import nl.tudelft.in4931.models.GameStates.Listener;
import nl.tudelft.in4931.models.Participant.Type;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GameStatesTest {
	
	private GameStates states;
	
	@Before
	public void setUp() {
		states = new GameStates();
	}
	
	@Test
	public void testSimpleActionOnEmptyStates() {
		final AtomicReference<GameState> counter = new AtomicReference<>();
		
		states.addListener(new Listener() {
			@Override
			public void onGameState(GameState state) {
				counter.set(state);
			}
		});
		
		states.onAction(new ParticipantJoinedAction(100L, Type.DRAGON, "Player #1"));
		
		while (counter.get() == null) { /* Busy wait until received updated GameState. */ }

		GameState gameState = counter.get();
		Map<Participant, Position> participants = gameState.getParticipants();
		
		Assert.assertEquals(100, gameState.getTime());
		Assert.assertEquals(1, participants.size());
	}
	
	@Test
	public void testSimpleActionOnNonEmptyStates() throws InterruptedException {
		testSimpleActionOnEmptyStates();
		
		final AtomicReference<GameState> counter = new AtomicReference<>();
		
		states.addListener(new Listener() {
			@Override
			public void onGameState(GameState state) {
				counter.set(state);
			}
		});
		
		states.onAction(new ParticipantJoinedAction(300L, Type.PLAYER, "Player #2"));
		
		while (counter.get() == null) { /* Busy wait until received updated GameState. */ }

		GameState gameState = counter.get();
		Map<Participant, Position> participants = gameState.getParticipants();
		
		Assert.assertEquals(300, gameState.getTime());
		Assert.assertEquals(2, participants.size());
	}
	
	@Test
	public void testRevertActionOnNonEmptyStates() throws InterruptedException {
		testSimpleActionOnNonEmptyStates();
		
		final AtomicReference<GameState> counter = new AtomicReference<>();
		
		states.addListener(new Listener() {
			@Override
			public void onGameState(GameState state) {
				counter.set(state);
			}
		});
		
		states.onAction(new ParticipantJoinedAction(200L, Type.PLAYER, "Player #3"));
		
		while (counter.get() == null) { /* Busy wait until received updated GameState. */ }

		GameState gameState = counter.get();
		Map<Participant, Position> participants = gameState.getParticipants();
		
		Assert.assertEquals(300, gameState.getTime());
		Assert.assertEquals(3, participants.size());
	}
	
	@Test
	public void testRevertActionOnEmptyStates() throws InterruptedException {
		testSimpleActionOnEmptyStates();
		
		final AtomicReference<GameState> counter = new AtomicReference<>();
		
		states.addListener(new Listener() {
			@Override
			public void onGameState(GameState state) {
				counter.set(state);
			}
		});
		
		states.onAction(new ParticipantJoinedAction(0L, Type.PLAYER, "Player #4"));
		
		while (counter.get() == null) { /* Busy wait until received updated GameState. */ }

		GameState gameState = counter.get();
		Map<Participant, Position> participants = gameState.getParticipants();
		
		Assert.assertEquals(100, gameState.getTime());
		Assert.assertEquals(2, participants.size());
	}

}
