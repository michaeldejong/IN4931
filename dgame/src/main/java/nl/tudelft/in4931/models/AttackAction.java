package nl.tudelft.in4931.models;

public class AttackAction extends Action {

	private final Participant target;

	public AttackAction(long time, Participant participant, Participant target) {
		super(time, participant);
		this.target = target;
	}
	
	public Participant getTarget() {
		return target;
	}
	
}
