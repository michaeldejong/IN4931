package nl.tudelft.in4931.network;

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * Describes the methods a node can call on remote nodes.
 */
public interface IRemoteObject extends Remote {

	/**
	 * This method will be called upon receiving a {@link Message}.
	 * 
	 * @param message	The received {@link Message}.
	 * @param from		The {@link Address} of the node which sent it.
	 * 
	 * @throws RemoteException	In case a RMI exception occurred.
	 */
	void onMessage(Message message, Address from) throws RemoteException;
	
}
