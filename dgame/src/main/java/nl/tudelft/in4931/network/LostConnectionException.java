package nl.tudelft.in4931.network;

public class LostConnectionException extends RuntimeException {

	private static final long serialVersionUID = -677415919868173455L;
	
	private final Address remote;
	
	public LostConnectionException(Address remote) {
		this.remote = remote;
	}
	
	public Address getRemote() {
		return remote;
	}

}
