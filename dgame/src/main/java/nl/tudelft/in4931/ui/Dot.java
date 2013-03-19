package nl.tudelft.in4931.ui;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;

import nl.tudelft.in4931.models.Participant;
import nl.tudelft.in4931.models.Participant.Type;

@SuppressWarnings("serial")
public class Dot extends JPanel {

	private final JLabel label;
	private final Type type;

	public Dot(Participant.Type type, int hp, int x, int y) {
		this.type = type;
		setBounds(1 + x * 20, 1 + y * 20, 20, 20);
		setBackground(type == Participant.Type.DRAGON ? Color.RED : color(hp));
		setLayout(null);
		
		label = new JLabel(Integer.toString(hp));
		label.setBounds(0, 0, 20, 20);
		add(label);
	}

	private Color color(int hp) {
		double rate = hp / 20.0;
		return new Color(0, 255, 0, (int) (rate * 255));
	}

	public void update(int hp, int x, int y) {
		setBounds(x * 20, y * 20, 20, 20);
		setBackground(type == Participant.Type.DRAGON ? Color.RED : color(hp));
		label.setText(Integer.toString(hp));
	}
	
}