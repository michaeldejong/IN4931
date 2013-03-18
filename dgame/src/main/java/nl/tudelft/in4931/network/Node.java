package nl.tudelft.in4931.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class Node {
	
	private static final Logger log = LoggerFactory.getLogger(Node.class);
	
	private final LocalNode receiver;
	
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

	protected abstract <M extends Message> void onMessage(M message, Address from) throws RemoteException;
	
}
