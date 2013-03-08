package nl.tudelft.in4931.dvgs.network;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


public class Topology implements Message {

	private static final long serialVersionUID = -991147078192111108L;
	
	private final Map<Address, TopologyParticipant> nodes;
	
	private transient final Address address;

	private Topology() {
		this.address = null;
		this.nodes = Maps.newTreeMap();
	}
	
	public Topology(Address address, Role role) {
		this.address = address;
		this.nodes = Maps.newTreeMap();
		
		nodes.put(address, new TopologyParticipant(address, 0, role));
	}

	public boolean knowsRoleOfNode(Address address) {
		synchronized (nodes) {
			return nodes.get(address) != null;
		}
	}
	
	boolean removeNode(Address address) {
		synchronized (nodes) {
			return nodes.remove(address) != null;
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
				TopologyParticipant participant = nodes.get(key);
				
				if (participant == null) {
					nodes.put(key, value);
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
		return new HashCodeBuilder().append(nodes).toHashCode();
	}
	
	@Override
	public boolean equals(Object other) {
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
	
	public Collection<Address> getSchedulers() {
		List<TopologyParticipant> schedulers = Lists.newArrayList();
		synchronized (nodes) {
			for (Entry<Address, TopologyParticipant> entry : nodes.entrySet()) {
				TopologyParticipant participant = entry.getValue();
				if (participant != null && participant.getRole() == Role.SCHEDULER) {
					schedulers.add(participant);
				}
			}
		}
		
		Collections.sort(schedulers);
		return Collections2.transform(schedulers, new Function<TopologyParticipant, Address>() {
			public Address apply(TopologyParticipant input) {
				return input.getAddress();
			}
		});
	}

}
