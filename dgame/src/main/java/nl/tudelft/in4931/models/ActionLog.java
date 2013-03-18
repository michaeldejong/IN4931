package nl.tudelft.in4931.models;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;

public class ActionLog {
	
	private final List<Action> actions = Lists.newArrayList();
	
	public Long insertAction(Action action) {
		synchronized (actions) {
			boolean wasEmpty = actions.isEmpty();
	
			if (wasEmpty) {
				actions.add(action);
				return null;
			}
			
			Action previousAction = actions.get(actions.size() - 1);
			if (previousAction.getTime() < action.getTime()) {
				actions.add(action);
				return null;
			}
			
			int index = actions.size() - 1;
			while (index >= 0 && actions.get(index).getTime() > action.getTime()) {
				index--;
			}
			
			actions.add(index + 1, action);
			return action.getTime() - 1;
		}
	}
	
	public Iterator<Action> iterateOnwardsFrom(long time) {
		synchronized (actions) {
			int index = actions.size() - 1;
			while (index > 0 && actions.get(index).getTime() >= time) {
				index--;
			}
			
			index++;
			return new ActionLogIterator(index);
		}
	}

	public Iterator<Action> iterateAll() {
		synchronized (actions) {
			return new ActionLogIterator(0);
		}
	}
	
	private class ActionLogIterator extends UnmodifiableIterator<Action> {

		private int index;

		public ActionLogIterator(int startingIndex) {
			this.index = startingIndex;
		}
		
		@Override
		public boolean hasNext() {
			synchronized (actions) {
				return index < actions.size();
			}
		}

		@Override
		public Action next() {
			synchronized (actions) {
				return actions.get(index++);
			}
		}
		
	}
	
}
