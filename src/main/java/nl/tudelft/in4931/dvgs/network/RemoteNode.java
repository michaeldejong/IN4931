package nl.tudelft.in4931.dvgs.network;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a representation of a remote node.
 * 
 * @author michael
 *
 * @param <T>
 */
class RemoteNode {

	private static final Logger log = LoggerFactory.getLogger(RemoteNode.class);
	
	private RemoteNode() {
		// Prevent instantiation.
	}
	
	public static IRemoteObject createProxy(Address address) throws RemoteException {
		try {
			log.trace("Doing lookup of proxy object for remote: {}", address);
			String rmiUrl = "rmi://" + address.getHostAddress() + ":" + address.getPort() + "/relay";
			return (IRemoteObject) Naming.lookup(rmiUrl);
		} 
		catch (RemoteException | MalformedURLException | NotBoundException e) {
			throw new RemoteException("Could not connect to: " + address);
		}
	}
	
}
