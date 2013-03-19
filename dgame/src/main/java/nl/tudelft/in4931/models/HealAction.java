package nl.tudelft.in4931.models;

public class HealAction extends Action {

	private static final long serialVersionUID = 6723052501010411984L;
	
	private final String target;

	public HealAction(Long time, String target) {
		super(time);
		this.target = target;
	}
	
	public String getTarget() {
		return target;
	}
	
}
