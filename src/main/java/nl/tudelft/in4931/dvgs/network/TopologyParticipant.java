package nl.tudelft.in4931.dvgs.network;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

class TopologyParticipant implements Serializable, Comparable<TopologyParticipant> {
	
	private static final long serialVersionUID = 3232079749072407939L;
	
	private Address address;
	private int age; 
	private Role role;
	
	public TopologyParticipant(Address address, int age, Role role) {
		this.address = address;
		this.age = age;
		this.role = role;
	}
	
	public void mergeDataWith(TopologyParticipant other) {
		Address otherAddress = other.address;
		if (address == null && otherAddress != null) {
			address = otherAddress;
		}
		else if (address != null && otherAddress != null && !address.equals(otherAddress)) {
			throw new IllegalArgumentException("Incompatible topologies: " + this + " - " + other);
		}
		
		Role otherRole = other.role;
		if (role == null && otherRole != null) {
			role = otherRole;
		}
		else if (role != null && otherRole != null && !role.equals(otherRole)) {
			throw new IllegalArgumentException("Incompatible topologies: " + this + " - " + other);
		}
		
		age = Math.max(age, other.age);
	}
	
	public Address getAddress() {
		return address;
	}

	public int getAge() {
		return age;
	}
	
	public Role getRole() {
		return role;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(address).append(age).append(role).toHashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof TopologyParticipant) {
			TopologyParticipant o = (TopologyParticipant) other;
			return new EqualsBuilder().append(address, o.address).append(age, o.age).append(role, o.role).isEquals();
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "(" + address + ", " + age + ", " + role.name() + ")";
	}

	@Override
	public int compareTo(TopologyParticipant o) {
		return Integer.compare(o.age, age);
	}
}