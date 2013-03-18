package nl.tudelft.in4931.models;

public class HealAction extends Action {

	private static final long serialVersionUID = 6723052501010411984L;
	
	private final Participant target;

	public HealAction(long time, Participant participant, Participant target) {
		super(time, participant);
		this.target = target;
	}
	
	public Participant getTarget() {
		return target;
	}
	
}
