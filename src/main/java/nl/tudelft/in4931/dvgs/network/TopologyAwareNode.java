package nl.tudelft.in4931.dvgs.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nl.tudelft.in4931.dvgs.network.TopologyEvent.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TopologyAwareNode extends Node {
	
	private static final Logger log = LoggerFactory.getLogger(TopologyAwareNode.class);
	
	private final ScheduledThreadPoolExecutor messenger;
	private final ScheduledThreadPoolExecutor poller;
	private final Topology topology;

	public TopologyAwareNode(final InetAddress address, Role role) throws IOException {
		super(address);

		this.messenger = new ScheduledThreadPoolExecutor(1);
		this.poller = new ScheduledThreadPoolExecutor(5);
		this.topology = new Topology(getLocalAddress(), role);

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
		}, 1000, 5000, TimeUnit.MILLISECONDS);
	}
	
	public void die() {
		poller.shutdownNow();
		messenger.shutdownNow();
		super.die();
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
					boolean canConnect = canConnect(address);
					boolean wasAlreadyKnown = topology.knowsRoleOfNode(address);
					
					if (!canConnect && wasAlreadyKnown) {
						log.info("{} - Detected disconnected node: {}", getLocalAddress(), address);
						onDisconnect(address);
					}
					else if (canConnect && !wasAlreadyKnown) {
						log.info("{} - Detected joined node: {}", getLocalAddress(), address);
						onJoined(address);
					}
				}
			});
		}
	}

	protected void broadcast(Message message) {
		multicast(message, topology.getRemoteNodes());
	}
	
	protected void multicast(Message message, Collection<Address> addresses) {
		for (Address address : addresses) {
			send(message, address);
		}
	}

	protected void send(final Message message, final Address address) {
		messenger.submit(new Runnable() {
			@Override
			public void run() {
				sendMessage(message, address, true);
			}
		});
	}
	
	protected Topology getTopology() {
		return topology;
	}

	private void registerMessageHandlers() {
		on(Topology.class, new Handler<Topology>() {
			@Override
			public void onMessage(Topology other, Address origin) {
				log.debug("{} - Received topology: {} from other node: {}", getLocalAddress(), other, origin);
				
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
		for (final Address address : topology.getRemoteNodes()) {
			sendTopology(address);
		}
	}

	private void sendTopology(final Address address) {
		messenger.submit(new Runnable() {
			@Override
			public void run() {
				sendMessage(topology.copy(), address, true);
			}
		});
	}

	private boolean canConnect(Address address) {
		try {
			log.trace("{} - Checking connection to: {}", getLocalAddress(), address);
			RemoteNode.createProxy(address);
			return true;
		} catch (RemoteException e) {
			return false;
		}
	}

	private void onDisconnect(Address address) {
		broadcast(new TopologyEvent(Type.LEFT, address));
		if (topology.removeNode(address)) {
			log.debug("{} - Removed node from topology: {}", getLocalAddress(), address);
		}
	}

	private void onJoined(Address address) {
		topology.joinNode(address);
		sendTopology(address);
	}

	private void sendMessage(Message message, Address address, boolean dropRemoteIfFailed) {
		try {
			log.debug("{} - Sending message: {} to: {}", getLocalAddress(), message, address);
			IRemoteObject proxy = RemoteNode.createProxy(address);
			proxy.onMessage(message, getLocalAddress());
		}
		catch (RemoteException e) {
			if (dropRemoteIfFailed) {
				onDisconnect(address);
			}
		}
	}
	
	public Collection<Address> getSchedulers() {
		return topology.getSchedulers();
	}

	public boolean isMasterScheduler() {
		Collection<Address> schedulers = getSchedulers();
		return schedulers.size() > 0 && schedulers.iterator().next().equals(getLocalAddress());
	}
	
}
