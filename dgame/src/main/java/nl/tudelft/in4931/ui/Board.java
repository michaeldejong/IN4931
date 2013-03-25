package nl.tudelft.in4931.ui;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import nl.tudelft.in4931.models.GameState;
import nl.tudelft.in4931.models.Participant;
import nl.tudelft.in4931.models.Position;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Board extends JFrame {

	private static final long serialVersionUID = -9064899378488632114L;
	
	private final Object lock = new Object();
	
	private final Map<String, Dot> dots = Maps.newConcurrentMap();
	private final JPanel back;
	private final AtomicLong timer = new AtomicLong();
	private GameState gameState = null;
	
	public Board(final BoardListener listener) {
		setLocation(50, 50);
		setSize(500, 522);
		setVisible(true);
		setLayout(null);
		
		back = new JPanel();
		back.setBounds(0, 0, 500, 500);
		back.setLayout(null);
		back.setBorder(new LineBorder(Color.GRAY));
		add(back);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		back.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) { }
			
			@Override
			public void mousePressed(MouseEvent e) { }
			
			@Override
			public void mouseExited(MouseEvent e) { }
			
			@Override
			public void mouseEntered(MouseEvent e) { }
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (listener == null) {
					return;
				}
				
				int x = (int) e.getX() / 20;
				int y = (int) e.getY() / 20;
				listener.spawnDragon(x, y);
			}
		});
	}
	
	public void update(final GameState state) {
		synchronized (lock) {
			if (!shouldDisplay(state)) {
				return;
			}
			
			setTitle("T: " + state.getTime() + " P: " + state.getParticipants().size());
			Set<String> alive = Sets.newHashSet();
			for (Entry<Participant, Position> entry : state.getParticipants().entrySet()) {
				String name = entry.getKey().getName();
				int hp = entry.getKey().getHp();
				int x = entry.getValue().getX();
				int y = entry.getValue().getY();
				if (!dots.containsKey(name)) {
					Participant.Type type = entry.getKey().getType();
					Dot dot = new Dot(type, hp, x, y);
					back.add(dot);
					dots.put(name, dot);
				}
				else if (hp > 0) {
					dots.get(name).update(hp, x, y);
				}
				
				alive.add(name);
			}
			
			Set<String> died = Sets.difference(dots.keySet(), alive);
			for (String name : died) {
				back.remove(dots.remove(name));
			}
			
			back.repaint();
			
			gameState = state;
			setTimeout();
		}
	}
	
	public void setTimeout() {
		timer.set(System.currentTimeMillis() + 1000);
	}
	
	public boolean shouldDisplay(GameState state) {
		return gameState == null || timerExpired();
	}
	
	public boolean timerExpired() {
		return timer.get() >= System.currentTimeMillis();
	}
	
	public interface BoardListener {
		void spawnDragon(int x, int y);
	}

}
