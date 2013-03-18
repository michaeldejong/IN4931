package nl.tudelft.in4931.network;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class NodeWithHandlers extends Node {
	
	private static final Logger log = LoggerFactory.getLogger(NodeWithHandlers.class);
	
	private final ScheduledThreadPoolExecutor messenger = new ScheduledThreadPoolExecutor(1);
	
	private final Map<Class<?>, Handler<?>> handlers = Maps.newHashMap();
	private final RemoteNodes remoteNodes;

	private volatile boolean alive = true;
	
	public NodeWithHandlers(final InetAddress address) throws IOException {
		super(address);
		this.remoteNodes = new RemoteNodes();
	}
	
	public void die() {
		if (alive) {
			log.info("{} - Terminated", getLocalAddress());
			alive = false;
			messenger.shutdownNow();
			super.die();
		}
	}
	
	protected void multicast(final Message message, final Collection<Address> addressees) {
		long start = System.currentTimeMillis();
		List<Future<?>> futures = Lists.newArrayList();
		for (Address address : addressees) {
			futures.add(send(message, address));
		}
		
		while (!futures.isEmpty()) {
			Future<?> future = futures.get(0);
			if (future.isCancelled() || future.isDone()) {
				futures.remove(0);
			}
			
			if (start + 5000 < System.currentTimeMillis()) {
				log.warn("This is taking quite long...");
			}
		}
	}

	protected Future<?> send(final Message message, final Address address) {
		return messenger.submit(new Runnable() {
			@Override
			public void run() {
				sendAndWait(message, address);
			}
		});
	}
	
	protected boolean sendAndWait(Message message, Address address) {
		if (address.equals(getLocalAddress())) {
			return true;
		}
		
		try {
			log.debug("{} - Sending: {} to: {}", getLocalAddress(), message, address);
			IRemoteObject proxy = remoteNodes.createProxy(address, true);
			proxy.onMessage(message, getLocalAddress());
			return true;
		}
		catch (RemoteException e) {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	protected final <M extends Message> void onMessage(M message, Address from) {
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
		if (handlers.containsKey(type)) {
			throw new IllegalArgumentException("Handler for message type; " + type.getSimpleName() + " is already registered!");
		}
		handlers.put(type, handler);
	}

}
