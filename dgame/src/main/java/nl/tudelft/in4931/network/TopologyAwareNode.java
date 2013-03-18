package nl.tudelft.in4931.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nl.tudelft.in4931.network.TopologyEvent.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;


public class TopologyAwareNode extends Node {
	
	private static final Logger log = LoggerFactory.getLogger(TopologyAwareNode.class);
	
	private final ScheduledThreadPoolExecutor poller = new ScheduledThreadPoolExecutor(5);
	private final Map<Class<?>, Handler<?>> handlers = Maps.newHashMap();
	private final ScheduledThreadPoolExecutor messenger;
	private final RemoteNodes remoteNodes;
	private final Topology topology;

	private volatile boolean alive = true;
	
	public TopologyAwareNode(final InetAddress address, Role role) throws IOException {
		super(address);

		this.messenger = new ScheduledThreadPoolExecutor(1);
		this.topology = new Topology(getLocalAddress(), role);
		this.remoteNodes = new RemoteNodes();

		registerMessageHandlers();
		
		poller.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					updateTopology(address);
				} catch (Throwable e) {
					log.error(e.getMessage(), e);
				}
			}
		}, 0, 5000, TimeUnit.MILLISECONDS);
	}
	
	public void addTopologyListener(TopologyListener listener) {
		synchronized (topology) {
			topology.addListener(listener);
		}
	}
	
	public void die() {
		if (alive) {
			log.info("{} - Terminated", getLocalAddress());
			alive = false;
			poller.shutdownNow();
			messenger.shutdownNow();
			super.die();
		}
	}
	
	private void updateTopology(InetAddress localAddress) throws UnknownHostException {
		log.debug("{} - Updating local topology...", getLocalAddress());
		Iterator<Address> iterator = new SubNetworkIterator(localAddress);
		while (iterator.hasNext()) {
			final Address address = iterator.next();
			if (address.equals(getLocalAddress())) {
				continue;
			}
			
			poller.submit(new Runnable() {
				@Override
				public void run() {
					try {
						boolean canConnect = canConnect(address);
						boolean wasAlreadyKnown;
						synchronized (topology) {
							wasAlreadyKnown = topology.knowsRoleOfNode(address);
						}
						
						if (!canConnect && wasAlreadyKnown) {
							log.warn("{} - Detected disconnected node: {}", getLocalAddress(), address);
							onDisconnect(address);
						}
						else if (canConnect && !wasAlreadyKnown) {
							log.debug("{} - Detected joined node: {}", getLocalAddress(), address);
							onJoined(address);
						}
					}
					catch (Throwable e) {
						log.error(e.getMessage(), e);
					}
				}
			});
		}
	}

	protected void broadcast(final Message message) {
		messenger.submit(new Runnable() {
			@Override
			public void run() {
				Set<Address> remotes;
				synchronized (topology) {
					remotes = topology.getRemoteNodes();
				}
				
				for (Address address : remotes) {
					sendAndWait(message, address);
				}
			}
		});
	}

	protected void multicast(final Message message, final Collection<Address> addressees) {
		for (Address address : addressees) {
			send(message, address);
		}
	}

	protected void send(final Message message, final Address address) {
		messenger.submit(new Runnable() {
			@Override
			public void run() {
				sendAndWait(message, address);
			}
		});
	}
	
	private void registerMessageHandlers() {
		on(Topology.class, new Handler<Topology>() {
			@Override
			public void onMessage(Topology other, Address origin) {
				log.trace("{} - Received topology: {} from other node: {}", getLocalAddress(), other, origin);
				
				synchronized (topology) {
					if (!topology.merge(other) && other.knowsRoleOfNode(getLocalAddress())) {
						return;
					}
					if (topology.size() > other.size()) {
						sendTopology(origin);
					}
					else if (topology.size() < other.size()){
						broadcastTopology();
					}
				}
			}
		});
		
		on(TopologyEvent.class, new Handler<TopologyEvent>() {
			@Override
			public void onMessage(TopologyEvent message, Address origin) {
				Address address = message.getAddress();
				switch (message.getType()) {
					case LEFT:		onDisconnect(address); 		break;
					case JOINED: 	onJoined(address); 			break;
					default: throw new IllegalArgumentException("Unsupported TopologyEvent.Type: " + message.getType());
				}
			}
		});
	}

	private void broadcastTopology() {
		Set<Address> remotes;
		synchronized (topology) {
			remotes = topology.getRemoteNodes();
		}
		
		for (final Address address : remotes) {
			sendTopology(address);
		}
	}

	private void sendTopology(final Address address) {
		messenger.submit(new Runnable() {
			@Override
			public void run() {
				Topology copy;
				synchronized (topology) {
					copy = topology.copy();
				}
				sendAndWait(copy, address);
			}
		});
	}

	private boolean canConnect(Address address) {
		try {
			log.trace("{} - Checking connection to: {}", getLocalAddress(), address);
			remoteNodes.createProxy(address, false);
			return true;
		} catch (RemoteException e) {
			return false;
		}
	}

	private void onDisconnect(Address address) {
		log.error("Node: " + address + " dropped from the cluster!");
		if (isScheduler()) {
			multicast(new TopologyEvent(Type.LEFT, address), getServers());
		}
		
		synchronized (topology) {
			if (topology.removeNode(address)) {
				log.trace("{} - Removed node from topology: {}", getLocalAddress(), address);
			}
		}
	}

	private void onJoined(Address address) {
		synchronized (topology) {
			topology.joinNode(address);
		}
		sendTopology(address);
	}

	protected boolean sendAndWait(Message message, Address address) {
		if (address.equals(getLocalAddress())) {
			return true;
		}
		
		try {
			if (!(message instanceof Topology)) {
				log.debug("{} - Sending: {} to: {}", getLocalAddress(), message, address);
			}
			IRemoteObject proxy = remoteNodes.createProxy(address, true);
			proxy.onMessage(message, getLocalAddress());
			return true;
		}
		catch (RemoteException e) {
			onDisconnect(address);
			return false;
		}
	}

	public int getNumberOfRemotes() {
		synchronized (topology) {
			return topology.getRemoteNodes().size();
		}
	}

	public List<Address> getServers() {
		synchronized (topology) {
			return topology.getServers();
		}
	}

	public List<Address> getClients() {
		synchronized (topology) {
			return topology.getClients();
		}
	}

	public boolean isScheduler() {
		List<Address> servers = getServers();
		return servers.contains(getLocalAddress());
	}
		
	@SuppressWarnings("unchecked")
	protected final <M extends Message> void onMessage(M message, Address from) {
		if (!(message instanceof Topology)) {
			log.debug(getLocalAddress() + " - Received message: {} from: {}", message, from);
		}
		
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
