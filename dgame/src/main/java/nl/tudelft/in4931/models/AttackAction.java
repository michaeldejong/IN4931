package nl.tudelft.in4931.models;

public class AttackAction extends Action {

	private static final long serialVersionUID = 5560095500008790558L;
	
	private final Participant target;

	public AttackAction(long time, Participant participant, Participant target) {
		super(time, participant);
		this.target = target;
	}
	
	public Participant getTarget() {
		return target;
	}
	
}
