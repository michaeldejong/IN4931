package nl.tudelft.in4931.dvgs.network;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is called by remote nodes, and relays the calls to the specified {@link NodeLogic}.
 * 
 * @author michael
 *
 * @param <M>
 */
@SuppressWarnings("serial")
public class RemoteObject extends UnicastRemoteObject implements IRemoteObject {

	private static final Logger log = LoggerFactory.getLogger(RemoteObject.class);
	
	private final Node node;

	/**
	 * This constructs a new {@link RemoteObject}.
	 * 
	 * @throws RemoteException	In case the {@link RemoteObject} could not be registered with RMI.
	 */
	public RemoteObject(Node node) throws RemoteException {
		this.node = node;
	}
	
	/**
	 * This method is called when receiving a method. It then relays 
	 * the message to the specified {@link NodeLogic}.
	 */
	@Override
	public void onMessage(Message message, Address from) throws RemoteException {
		try {
			if (!(message instanceof Topology) && !(message instanceof TopologyEvent)) {
				log.debug("{} - Received message: {}", node.getLocalAddress(), message);
			}
			node.onMessage(message, from);
		}
		catch (Throwable e) {
			log.error("Failure while processing message: {} - {}", message, e);
		}
	}
	
}
