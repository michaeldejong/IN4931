package nl.tudelft.in4931.dvgs.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


/**
 * This class is responsible for managing incoming connections.
 * 
 * @author michael
 *
 * @param <I>
 */
class LocalNode {

	private final Address address;
	private Registry registry = null;

	/**
	 * This constructor creates a new {@link LocalNode} object.
	 * 
	 * @param host					The local node's {@link InetAddress}.
	 * @throws UnknownHostException	In case the {@link LocalNode} is unable to create the RMI registry.
	 */
	public LocalNode(InetAddress host, IRemoteObject remote) throws IOException {
		int port = SubNetworkIterator.PORTS.lowerEndpoint();
		while (registry == null) {
			try {
				registry = LocateRegistry.createRegistry(port);
			}
			catch (RemoteException e) {
				port++;
				if (port > SubNetworkIterator.PORTS.upperEndpoint()) {
					throw new RuntimeException("Cannot find a free port in the range: " + SubNetworkIterator.PORTS);
				}
			}
		}
		
		this.address = new Address(host.getHostAddress(), port);
		
		registry.rebind("relay", remote);
	}

	public void die() {
		try {
			registry.unbind("relay");
		} catch (RemoteException | NotBoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @return	The local node's {@link Address}.
	 */
	public Address getLocalAddress() {
		return address;
	}
	
}
