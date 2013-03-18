package nl.tudelft.in4931.dvgs.network;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * This class is a representation of a remote node.
 * 
 * @author michael
 *
 * @param <T>
 */
class RemoteNodes {

	private static final Logger log = LoggerFactory.getLogger(RemoteNodes.class);
	
	private static final Map<Address, IRemoteObject> cache = Maps.newConcurrentMap();
	
	public IRemoteObject createProxy(Address address, boolean allowCached) throws RemoteException {
		if (allowCached) {
			IRemoteObject cachedRemote = cache.get(address);
			if (cachedRemote != null) {
				return cachedRemote;
			}
		}
			
		try {
			log.trace("Doing lookup of proxy object for remote: {}", address);
			String rmiUrl = "rmi://" + address.getHostAddress() + ":" + address.getPort() + "/relay";
			IRemoteObject lookup = (IRemoteObject) Naming.lookup(rmiUrl);
			
			if (allowCached) {
				cache.put(address, lookup);
			}
			
			return lookup;
		} 
		catch (RemoteException | MalformedURLException | NotBoundException e) {
			throw new RemoteException("Could not connect to: " + address);
		}
	}
	
}
