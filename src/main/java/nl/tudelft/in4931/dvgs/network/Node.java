package nl.tudelft.in4931.dvgs.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;


public abstract class Node {
	
	private static final Logger log = LoggerFactory.getLogger(Node.class);
	
	private final LocalNode receiver;
	private final Map<Class<?>, Handler<?>> handlers = Maps.newHashMap();
	
	/**
	 * This constructs a new {@link Node} object.
	 * 
	 * @param address	The {@link Address} of the local node.
	 * @param localOnly	True if the cluster will run on a single machine, or false otherwise.
	 * 
	 * @throws UnknownHostException	In case of network issues.
	 */
	public Node(InetAddress address) throws IOException {
		if (!address.equals(InetAddress.getLocalHost()) && System.getSecurityManager() == null) {
			log.info("Setting RMI SecurityManager...");
			System.setSecurityManager(new RMISecurityManager());
		}
		this.receiver = new LocalNode(address, new RemoteObject(this));
	}
	
	protected void die() {
		receiver.die();
	}
	
	public Address getLocalAddress() {
		return receiver.getLocalAddress();
	}

	@SuppressWarnings("unchecked")
	final <M extends Message> void onMessage(M message, Address from) throws RemoteException {
		log.trace(getLocalAddress() + " - Received message: {} from: {}", message, from);
		Handler<?> handler = handlers.get(message.getClass());
		
		if (handler != null) {
			((Handler<M>) handler).onMessage(message, from);
		}
		else {
			log.warn(getLocalAddress() + " - No message handler registered from messages of type: {}", message.getClass());
		}
	}
	
	public <M extends Message> void on(Class<M> type, Handler<M> handler) {
		if (handlers.containsKey(type)) {
			throw new IllegalArgumentException("Handler for message type; " + type.getSimpleName() + " is already registered!");
		}
		handlers.put(type, handler);
	}
	
}
