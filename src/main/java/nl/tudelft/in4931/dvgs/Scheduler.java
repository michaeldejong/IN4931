package nl.tudelft.in4931.dvgs;

import java.io.IOException;
import java.net.InetAddress;

import nl.tudelft.in4931.dvgs.network.Role;
import nl.tudelft.in4931.dvgs.network.TopologyAwareNode;

public class Scheduler extends TopologyAwareNode {

	public Scheduler(InetAddress address) throws IOException {
		super(address, Role.SCHEDULER);
	}
	
}
