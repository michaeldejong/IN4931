package nl.tudelft.in4931.models;

public class AttackAction extends Action {

	private static final long serialVersionUID = 5560095500008790558L;

	private final String target;

	public AttackAction(Long time, String target) {
		super(time);
		this.target = target;
	}
	
	public String getTarget() {
		return target;
	}
	
}
