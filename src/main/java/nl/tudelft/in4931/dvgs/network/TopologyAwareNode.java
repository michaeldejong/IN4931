package nl.tudelft.in4931.dvgs.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nl.tudelft.in4931.dvgs.network.TopologyEvent.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;


public class TopologyAwareNode extends Node {
	
	private static final Logger log = LoggerFactory.getLogger(TopologyAwareNode.class);

	private final Map<Class<?>, Handler<?>> handlers = Maps.newHashMap();
	private final ScheduledThreadPoolExecutor messenger;
	private final ScheduledThreadPoolExecutor poller;
	private final Topology topology;

	private volatile boolean alive = true;
	
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
		}, 0, 5000, TimeUnit.MILLISECONDS);
	}
	
	public void addTopologyListener(TopologyListener listener) {
		topology.addListener(listener);
	}
	
	public void die() {
		if (alive) {
			alive = false;
			poller.shutdownNow();
			messenger.shutdownNow();
			super.die();
		}
	}
	
	private void updateTopology(InetAddress localAddress) throws UnknownHostException {
		log.trace("{} - Updating local topology...", getLocalAddress());
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

	protected void broadcast(final Message message) {
		messenger.submit(new Runnable() {
			@Override
			public void run() {
				for (Address address : topology.getRemoteNodes()) {
					sendAndWait(message, address, true);
				}
			}
		});
	}

	protected void broadcastToSchedulers(final Message message) {
		messenger.submit(new Runnable() {
			@Override
			public void run() {
				boolean replicated = false;
				Iterator<Address> iterator = getSchedulers().iterator();
				
				while (!replicated && iterator.hasNext()) {
					Address scheduler = iterator.next();
					if (sendAndWait(message, scheduler, true)) {
						replicated = true;
					}
				}
			}
		});
	}

	protected void broadcastToBackupSchedulers(final Message message) {
		messenger.submit(new Runnable() {
			@Override
			public void run() {
				boolean replicated = false;
				Iterator<Address> iterator = getSchedulers().iterator();
				while (!iterator.next().equals(getLocalAddress())) {
					// Forward until iterator pointed to local host.
				}
				
				while (!replicated && iterator.hasNext()) {
					Address scheduler = iterator.next();
					if (sendAndWait(message, scheduler, true)) {
						replicated = true;
					}
				}
			}
		});
	}

	protected void send(final Message message, final Address address) {
		messenger.submit(new Runnable() {
			@Override
			public void run() {
				sendAndWait(message, address, true);
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
				log.trace("{} - Received topology: {} from other node: {}", getLocalAddress(), other, origin);
				
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
				sendAndWait(topology.copy(), address, true);
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
			log.trace("{} - Removed node from topology: {}", getLocalAddress(), address);
		}
	}

	private void onJoined(Address address) {
		topology.joinNode(address);
		sendTopology(address);
	}

	protected boolean sendAndWait(Message message, Address address, boolean dropRemoteIfFailed) {
		try {
			if (!(message instanceof Topology)) {
				log.debug("{} - Sending: {} to: {}", getLocalAddress(), message, address);
			}
			IRemoteObject proxy = RemoteNode.createProxy(address);
			proxy.onMessage(message, getLocalAddress());
			return true;
		}
		catch (RemoteException e) {
			if (dropRemoteIfFailed) {
				onDisconnect(address);
			}
			return false;
		}
	}

	@Override
	protected <M extends Message> void onMessage(M message, Address from) throws RemoteException {
		if (isScheduler() && !(message instanceof Topology)) {
			replicateMessageToBackup(message, from);
		}
		else {
			handleMessage(message, from);
		}
	}

	private <M extends Message> void replicateMessageToBackup(M message, Address from) {
		boolean replicate = true;
		Address backup = getBackupOf(getLocalAddress());
		while (backup != null && replicate) {
			try {
				log.debug("{} - Replicating: {} to: {}", getLocalAddress(), message, backup);
				IRemoteObject proxy = RemoteNode.createProxy(backup);
				proxy.onMessage(message, from);
				replicate = false;
			}
			catch (RemoteException e) {
				topology.removeNode(backup);
				backup = getBackupOf(backup);
			}
		}

		if (backup == null) {
			handleMessage(message, from);
		}
	}

	public List<Address> getSchedulers() {
		return topology.getSchedulers();
	}

	public boolean isMasterScheduler() {
		List<Address> schedulers = getSchedulers();
		return !schedulers.isEmpty() && schedulers.get(0).equals(getLocalAddress());
	}

	public boolean isScheduler() {
		List<Address> schedulers = getSchedulers();
		return schedulers.contains(getLocalAddress());
	}
	
	private Address getBackupOf(Address address) {
		List<Address> schedulers = getSchedulers();
		int index = schedulers.indexOf(address);
		if (index >= 0 && (index + 1 < schedulers.size())) {
			return schedulers.get(index + 1);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private final <M extends Message> void handleMessage(M message, Address from) {
		if (!(message instanceof Topology)) {
			log.debug(getLocalAddress() + " - Received message: {} from: {}", message, from);
		}
		
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
