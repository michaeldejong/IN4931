package nl.tudelft.in4931.network;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


public class Topology implements Message {

	private static final long serialVersionUID = -991147078192111108L;
	
	private final Map<Address, TopologyParticipant> nodes;
	private final transient List<TopologyListener> listeners;
	
	private transient final Address address;

	private Topology() {
		this.address = null;
		this.nodes = Maps.newTreeMap();
		this.listeners = Lists.newArrayList();
	}
	
	public Topology(Address address, Role role) {
		this.address = address;
		this.nodes = Maps.newTreeMap();
		this.listeners = Lists.newArrayList();
		
		nodes.put(address, new TopologyParticipant(address, 0, role));
	}

	public boolean knowsRoleOfNode(Address address) {
		synchronized (nodes) {
			return nodes.containsKey(address);
		}
	}
	
	boolean removeNode(Address address) {
		synchronized (nodes) {
			TopologyParticipant removed = nodes.remove(address);
			if (removed != null) {
				notifyListenersOfLeft(removed.getAddress(), removed.getRole());
				return true;
			}
			return false;
		}
	}

	public Set<Address> getAllNodes() {
		synchronized (nodes) {
			return Collections.unmodifiableSet(nodes.keySet());
		}
	}

	public Set<Address> getRemoteNodes() {
		synchronized (nodes) {
			return Collections.unmodifiableSet(Sets.difference(nodes.keySet(), Sets.newHashSet(address)));
		}
	}

	boolean merge(Topology other) {
		synchronized (nodes) {
			boolean modified = false;
			for (Entry<Address, TopologyParticipant> entry : other.nodes.entrySet()) {
				Address key = entry.getKey();
				TopologyParticipant value = entry.getValue();
				if (value == null) {
					continue;
				}
				
				TopologyParticipant participant = nodes.get(key);
				
				if (participant == null) {
					boolean joined = !nodes.containsKey(key);
					nodes.put(key, value);
					if (joined) {
						notifyListenersOfJoin(value.getAddress(), value.getRole());
					}
					modified = true;
				}
				else if (value != null) {
					participant.mergeDataWith(value);
					modified = true;
				}
			}
			return modified;
		}
	}

	boolean joinNode(Address address) {
		synchronized (nodes) {
			if (!nodes.containsKey(address)) {
				nodes.put(address, null);
				return true;
			}
			return false;
		}
	}
	
	@Override
	public String toString() {
		Set<Address> addresses = Sets.newHashSet();
		synchronized (nodes) {
			for (Entry<Address, TopologyParticipant> entry : nodes.entrySet()) {
				if (entry.getValue() != null) {
					addresses.add(entry.getKey());
				}
			}
		}
		return Joiner.on(", ").join(addresses);
	}
	
	@Override
	public int hashCode() {
		synchronized (nodes) {
			return new HashCodeBuilder().append(nodes).toHashCode();
		}
	}
	
	@Override
	public boolean equals(Object other) {
		synchronized (nodes) {
			if (other instanceof Topology) {
				Topology topo = (Topology) other;
				if (nodes.size() != topo.nodes.size()) {
					return false;
				}
				
				for (Entry<Address, TopologyParticipant> entry : nodes.entrySet()) {
					TopologyParticipant participant = topo.nodes.get(entry.getValue());
					if ((participant == null && entry.getValue() != null) || (participant != null && !participant.equals(entry.getValue()))) {
						return false;
					}
				}
				return true;
			}
			return false;
		}
	}
	
	public Topology copy() {
		Topology topology = new Topology();
		synchronized (nodes) {
			for (Entry<Address, TopologyParticipant> entry : nodes.entrySet()) {
				topology.nodes.put(entry.getKey(), entry.getValue());
			}
		}
		return topology;
	}

	public int size() {
		int count = 0;
		synchronized (nodes) {
			for (Entry<Address, TopologyParticipant> entry : nodes.entrySet()) {
				if (entry.getValue() != null) {
					count++;
				}
			}
		}
		return count;
	}

	public List<Address> getClients() {
		return listParticipants(Role.CLIENT);
	}
	
	public List<Address> getServers() {
		return listParticipants(Role.SERVER);
	}
		
	private List<Address> listParticipants(Role role) {
		List<Address> addresses = Lists.newArrayList();
		synchronized (nodes) {
			List<TopologyParticipant> participants = Lists.newArrayList();
			for (Entry<Address, TopologyParticipant> entry : nodes.entrySet()) {
				TopologyParticipant participant = entry.getValue();
				if (participant != null && participant.getRole() == role) {
					participants.add(participant);
				}
			}
			for (TopologyParticipant participant : participants) {
				addresses.add(participant.getAddress());
			}
		}
		
		return addresses;
	}

	void notifyListenersOfJoin(Address address, Role role) {
		synchronized (listeners) {
			for (TopologyListener listener : listeners) {
				listener.onNodeJoin(address, role);
			}
		}
	}

	void notifyListenersOfLeft(Address address, Role role) {
		synchronized (listeners) {
			for (TopologyListener listener : listeners) {
				listener.onNodeLeft(address, role);
			}
		}
	}

	public void addListener(TopologyListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

}
