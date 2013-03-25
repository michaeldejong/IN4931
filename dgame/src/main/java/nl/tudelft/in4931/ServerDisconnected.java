package nl.tudelft.in4931;

import java.util.Collection;
import java.util.Collections;

import nl.tudelft.in4931.models.Action;
import nl.tudelft.in4931.network.Address;

public class ServerDisconnected extends Action {

	private static final long serialVersionUID = -5768645784770894570L;
	
	private final Address address;
	private final Collection<String> clientNames;
	
	public ServerDisconnected(Long time, Address address, Collection<String> clientNames) {
		super(time);
		this.address = address;
		this.clientNames = clientNames;
	}

	public Address getAddress() {
		return address;
	}

	public Collection<String> getClientNames() {
		return Collections.unmodifiableCollection(clientNames);
	}

}
