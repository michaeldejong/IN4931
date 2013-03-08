package nl.tudelft.in4931.dvgs.network;

public interface Handler<T> {
	
	void onMessage(T message, Address origin);

}
