package nl.tudelft.in4931.network;

public interface TopologyListener {

	void onNodeLeft(Address address, Role role);
	
	void onNodeJoin(Address address, Role role);
	
}
