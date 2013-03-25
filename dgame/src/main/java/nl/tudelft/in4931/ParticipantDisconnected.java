package nl.tudelft.in4931;

import nl.tudelft.in4931.models.Action;
import nl.tudelft.in4931.network.Address;

public class ParticipantDisconnected extends Action {

	private static final long serialVersionUID = -1312789172286525481L;

	private final Address address;
	private final String participantName;
	
	public ParticipantDisconnected(Long time, Address address, String participantName) {
		super(time);
		this.address = address;
		this.participantName = participantName;
	}

	public Address getAddress() {
		return address;
	}

	public String getParticipantName() {
		return participantName;
	}

}
