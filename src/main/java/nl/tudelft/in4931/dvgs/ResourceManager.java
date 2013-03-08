package nl.tudelft.in4931.dvgs;

import java.io.IOException;
import java.net.InetAddress;

import nl.tudelft.in4931.dvgs.network.Role;
import nl.tudelft.in4931.dvgs.network.TopologyAwareNode;

public class ResourceManager extends TopologyAwareNode {

	public ResourceManager(InetAddress address) throws IOException {
		super(address, Role.RESOURCE_MANAGER);
	}

}
