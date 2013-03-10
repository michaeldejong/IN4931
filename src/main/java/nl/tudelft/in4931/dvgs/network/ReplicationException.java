package nl.tudelft.in4931.dvgs.network;

public class ReplicationException extends Exception {

	private static final long serialVersionUID = -7427558973133991320L;
	
	private final Address address;
	
	public ReplicationException(Address address) {
		this.address = address;
	}
	
	public Address getAddress() {
		return address;
	}

}
