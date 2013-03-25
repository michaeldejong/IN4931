package nl.tudelft.in4931.network;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public abstract class NodeWithHandlers extends Node {
	
	private static final Logger log = LoggerFactory.getLogger(NodeWithHandlers.class);
	
	private final ScheduledThreadPoolExecutor messenger = new ScheduledThreadPoolExecutor(1);
	
	private final Map<Class<?>, Handler<?>> handlers = Maps.newHashMap();
	private final RemoteNodes remoteNodes;
	private final AtomicBoolean alive = new AtomicBoolean(true);
	
	public NodeWithHandlers(final InetAddress address) throws IOException {
		super(address);
		this.remoteNodes = new RemoteNodes();
	}
	
	public void die() {
		synchronized (alive) {
			if (alive.get()) {
				log.info("{} - Terminated", getLocalAddress());
				alive.set(false);
				messenger.shutdownNow();
				super.die();
			}
		}
	}
	
	protected void multicast(final Message message, final Collection<Address> addressees) {
		for (Address address : addressees) {
			send(message, address);
		}
	}
	
	protected void multicastAndWait(final Message message, final Collection<Address> addressees) {
		ensureAlive();
		
		List<Future<?>> futures = Lists.newArrayList();
		for (Address address : addressees) {
			futures.add(send(message, address));
		}
		
		while (!futures.isEmpty()) {
			Future<?> future = futures.get(0);
			if (future.isCancelled() || future.isDone()) {
				futures.remove(0);
			}
		}
	}

	protected Future<?> send(final Message message, final Address address) {
		ensureAlive();
		
		return messenger.submit(new Runnable() {
			@Override
			public void run() {
				sendAndWait(message, address);
			}
		});
	}
	
	protected void sendAndWait(Message message, Address address) {
		ensureAlive();
		if (address.equals(getLocalAddress())) {
			return;
		}
		
		try {
			log.debug("{} - Sending: {} to: {}", getLocalAddress(), message, address);
			IRemoteObject proxy = remoteNodes.createProxy(address, true);
			proxy.onMessage(message, getLocalAddress());
		}
		catch (RemoteException e) {
			onConnectionLost(address);
		}
	}

	@SuppressWarnings("unchecked")
	protected final <M extends Message> void onMessage(M message, Address from) {
		ensureAlive();
		log.debug(getLocalAddress() + " - Received message: {} from: {}", message, from);
		
		Handler<?> handler = handlers.get(message.getClass());
		if (handler != null) {
			((Handler<M>) handler).onMessage(message, from);
			return;
		}
		
		for (Class<?> clazz : handlers.keySet()) {
			if (clazz.isAssignableFrom(message.getClass())) {
				((Handler<M>) handlers.get(clazz)).onMessage(message, from);
				return;
			}
		}
		
		log.warn(getLocalAddress() + " - No message handler registered from messages of type: {}", message.getClass());
	}
	
	public <M extends Message> void on(Class<M> type, Handler<M> handler) {
		ensureAlive();
		if (handlers.containsKey(type)) {
			throw new IllegalArgumentException("Handler for message type; " + type.getSimpleName() + " is already registered!");
		}
		handlers.put(type, handler);
	}

	private void ensureAlive() {
		synchronized (alive) {
			if (!alive.get()) {
				throw new RuntimeException("This node has died!");
			}
		}
	}

	protected abstract void onConnectionLost(Address remote);

}
