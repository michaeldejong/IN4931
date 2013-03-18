package nl.tudelft.in4931.models;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import nl.tudelft.in4931.models.GameStates.Listener;

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
			public void onUpdate(GameState state) {
				counter.set(state);
			}
		});
		
		Dragon dragon = new Dragon();
		states.onAction(new ParticipantJoinedAction(100, dragon));
		
		while (counter.get() == null) { /* Busy wait until received updated GameState. */ }

		GameState gameState = counter.get();
		Map<Participant, Position> participants = gameState.getParticipants();
		
		Assert.assertEquals(100, gameState.getTime());
		Assert.assertEquals(1, participants.size());
		Assert.assertTrue(participants.containsKey(dragon));
	}
	
	@Test
	public void testSimpleActionOnNonEmptyStates() throws InterruptedException {
		testSimpleActionOnEmptyStates();
		
		final AtomicReference<GameState> counter = new AtomicReference<>();
		
		states.addListener(new Listener() {
			@Override
			public void onUpdate(GameState state) {
				counter.set(state);
			}
		});
		
		Player player = new Player();
		states.onAction(new ParticipantJoinedAction(300, player));
		
		while (counter.get() == null) { /* Busy wait until received updated GameState. */ }

		GameState gameState = counter.get();
		Map<Participant, Position> participants = gameState.getParticipants();
		
		Assert.assertEquals(300, gameState.getTime());
		Assert.assertEquals(2, participants.size());
		Assert.assertTrue(participants.containsKey(player));
	}
	
	@Test
	public void testRevertActionOnNonEmptyStates() throws InterruptedException {
		testSimpleActionOnNonEmptyStates();
		
		final AtomicReference<GameState> counter = new AtomicReference<>();
		
		states.addListener(new Listener() {
			@Override
			public void onUpdate(GameState state) {
				counter.set(state);
			}
		});
		
		Player player = new Player();
		states.onAction(new ParticipantJoinedAction(200, player));
		
		while (counter.get() == null) { /* Busy wait until received updated GameState. */ }

		GameState gameState = counter.get();
		Map<Participant, Position> participants = gameState.getParticipants();
		
		Assert.assertEquals(300, gameState.getTime());
		Assert.assertEquals(2, participants.size());
		Assert.assertTrue(participants.containsKey(player));
	}
	
	@Test
	public void testRevertActionOnEmptyStates() throws InterruptedException {
		testSimpleActionOnEmptyStates();
		
		final AtomicReference<GameState> counter = new AtomicReference<>();
		
		states.addListener(new Listener() {
			@Override
			public void onUpdate(GameState state) {
				counter.set(state);
			}
		});
		
		Player player = new Player();
		states.onAction(new ParticipantJoinedAction(0, player));
		
		while (counter.get() == null) { /* Busy wait until received updated GameState. */ }

		GameState gameState = counter.get();
		Map<Participant, Position> participants = gameState.getParticipants();
		
		Assert.assertEquals(100, gameState.getTime());
		Assert.assertEquals(2, participants.size());
		Assert.assertTrue(participants.containsKey(player));
	}

}
