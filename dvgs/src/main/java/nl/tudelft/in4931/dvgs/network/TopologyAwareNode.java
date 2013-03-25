package nl.tudelft.in4931.dvgs.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nl.tudelft.in4931.dvgs.network.TopologyEvent.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;


public class TopologyAwareNode extends Node {

	private static final Logger log = LoggerFactory.getLogger(TopologyAwareNode.class);
	
	private final ScheduledThreadPoolExecutor poller = new ScheduledThreadPoolExecutor(5);
	private final ScheduledThreadPoolExecutor messenger = new ScheduledThreadPoolExecutor(2);
	private final Map<Class<?>, Handler<?>> handlers = Maps.newHashMap();
	private final RemoteNodes remoteNodes;
	private final Topology topology;

	private volatile boolean alive = true;
	
	public TopologyAwareNode(final InetAddress address, Role role) throws IOException {
		super(address);

		this.topology = new Topology(getLocalAddress(), role);
		this.remoteNodes = new RemoteNodes();

		messenger.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				log.warn("{} - Failed to schedule runnable...", getLocalAddress());
			}
		});
		
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
			log.debug("{} - Terminated", getLocalAddress());
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
			
			poller.submit(new Poller(address));
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

	protected void toScheduler(final Message message, boolean fireAndForget) {
		Future<?> future = messenger.submit(new Runnable() {
			@Override
			public void run() {
				boolean replicated = false;
				Iterator<Address> iterator = getSchedulers().iterator();
				
				while (!replicated && iterator.hasNext()) {
					Address scheduler = iterator.next();
					if (sendAndWait(message, scheduler)) {
						log.debug("{} - Delivered message: {}", getLocalAddress(), message);
						replicated = true;
					}
					else {
						log.warn("{} - Could not replicate message to: {}", getLocalAddress(), scheduler);
					}
				}
				
				if (replicated) {
					log.debug("{} - Successfully replicated message: {}", getLocalAddress(), message);
				}
				else {
					log.error("{} - Message could not be delivered to the schedulers!", getLocalAddress());
				}
			}
		});
		
		if (!fireAndForget) {
			blockOnFuture(future);
		}
	}

	protected void toBackup(final Message message, boolean fireAndForget) {
		Future<?> future = messenger.submit(new Runnable() {
			@Override
			public void run() {
				boolean replicated = false;
				List<Address> schedulers = getSchedulers();
				int index = schedulers.indexOf(getLocalAddress()) + 1;
				
				while (!replicated && index < schedulers.size()) {
					Address scheduler = schedulers.get(index);
					if (sendAndWait(message, scheduler)) {
						replicated = true;
					}
				}
			}
		});
		
		if (!fireAndForget) {
			blockOnFuture(future);
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
			toBackup(new TopologyEvent(Type.LEFT, address), true);
		}
		
		synchronized (topology) {
			if (topology.removeNode(address)) {
				log.trace("{} - Removed node from topology: {}", getLocalAddress(), address);
			}
		}
	}

	private void onJoined(Address address) {
		log.info("{} - Detected new node: {}", getLocalAddress(), address);
		synchronized (topology) {
			topology.joinNode(address);
		}
		sendTopology(address);
	}

	protected boolean sendAndWait(Message message, Address address) {
		try {
			if (!(message instanceof Topology) && !(message instanceof TopologyEvent)) {
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

	@Override
	protected <M extends Message> void onMessage(M message, Address from) throws RemoteException {
		if (isScheduler() && !(message instanceof Topology)) {
			replicateMessageToBackup(message, from);
		}
		handleMessage(message, from);
	}

	private <M extends Message> void replicateMessageToBackup(M message, Address from) {
		boolean replicated = false;
		Address backup = getBackupOf(getLocalAddress());
		while (backup != null && !replicated) {
			try {
				log.debug("{} - Replicating: {} to: {}", getLocalAddress(), message, backup);
				IRemoteObject proxy = remoteNodes.createProxy(backup, true);
				proxy.onMessage(message, from);
				replicated = true;
			}
			catch (RemoteException e) {
				topology.removeNode(backup);
				backup = getBackupOf(backup);
			}
		}

		if (backup == null || !replicated) {
			handleMessage(message, from);
		}
	}

	public int getNumberOfRemotes() {
		synchronized (topology) {
			return topology.getRemoteNodes().size();
		}
	}

	public List<Address> getSchedulers() {
		synchronized (topology) {
			return topology.getSchedulers();
		}
	}

	public List<Address> getResourceManagers() {
		synchronized (topology) {
			return topology.getResourceManagers();
		}
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

	private void blockOnFuture(Future<?> future) {
		while (!future.isDone() && !future.isCancelled()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				log.warn(e.getMessage(), e);
			}
		}
	}
	
	private class Poller implements Runnable {
		private final Address address;

		private Poller(Address address) {
			this.address = address;
		}

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
	}

}
