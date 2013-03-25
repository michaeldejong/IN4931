package nl.tudelft.in4931.models;

import java.util.List;

import com.google.common.collect.Lists;

public class GameStates {
	
	private final ActionLog actionLog = new ActionLog();
	private final List<GameState> states = Lists.newArrayList();
	private final List<Listener> listeners = Lists.newArrayList();
	
	private final Object lock = new Object();
	
	public void addListener(Listener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}
	
	public void onAction(Action action) {
		synchronized (lock) {
			Long revertToTime = actionLog.insertAction(action);
			if (revertToTime == null) {
				ensureSnapshot(action.getTime());
				return;
			}
			
			revert(revertToTime);
		}
	}
	
	private void revert(Long toTime) {
		synchronized (lock) {
			if (states.isEmpty()) {
				return;
			}
			
			int index = states.size() - 1;
			while (index >= 0 && states.get(index).getTime() > toTime) {
				states.remove(index);
				index--;
			}
			
			if (states.isEmpty()) {
				ensureSnapshot(Long.MAX_VALUE);
			}
			else {
				ensureSnapshot(toTime);
			}
		}
	}

	private void ensureSnapshot(long time) {
		synchronized (lock) {
			if (states.isEmpty()) {
				GameState newState = GameState.from(actionLog.iterateAll());
				states.add(newState);
				notifyListeners(newState);
				return;
			}
			
			GameState previousState = getCurrentState();
			if (time >= previousState.getTime()) {
				GameState newState = GameState.from(previousState, actionLog.iterateOnwardsFrom(previousState.getTime() + 1));
				states.add(newState);
				notifyListeners(newState);
			}
		}
	}
	
	private void notifyListeners(GameState state) {
		synchronized (listeners) {
			for (Listener listener : listeners) {
				listener.onGameState(state);
			}
		}
	}
	
	public GameState getCurrentState() {
		synchronized (lock) {
			return states.get(states.size() - 1);
		}
	}

	public interface Listener {
		void onGameState(GameState state);
	}
	
}
