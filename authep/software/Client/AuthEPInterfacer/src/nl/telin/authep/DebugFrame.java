package nl.telin.authep;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class DebugFrame extends JFrame implements ILogger {
	
	private JTextArea area;
	
	public DebugFrame()
	{
		setLayout(new BorderLayout());
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BorderLayout());
		area = new JTextArea();
		centerPanel.add(new JScrollPane(area), BorderLayout.CENTER);
		
		add(centerPanel, BorderLayout.CENTER);
		this.setSize(300, 300);
	}
	
	public void log(String msg) {
		area.append(msg + "\n");
		System.out.println(getClass().getCanonicalName() + ": " + msg);
	}
}
