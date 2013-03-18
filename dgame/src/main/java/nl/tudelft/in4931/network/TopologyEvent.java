package nl.tudelft.in4931.network;

public class TopologyEvent implements Message {
	
	private static final long serialVersionUID = 5040223548298806841L;
	
	private final Type type;
	private final Address address;
	
	public TopologyEvent(Type type, Address address) {
		this.type = type;
		this.address = address;
	}
	
	public Type getType() {
		return type;
	}
	
	public Address getAddress() {
		return address;
	}
	
	public static enum Type {
		JOINED, LEFT;
	}

}
