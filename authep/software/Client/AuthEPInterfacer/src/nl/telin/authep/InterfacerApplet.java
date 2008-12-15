package nl.telin.authep;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Ellipse2D;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import sos.smartcards.CardEvent;
import sos.smartcards.CardManager;
import sos.smartcards.CardTerminalListener;

/**
 * May require adding the following lines to your local <code>.java.policy</code> file.
 * <pre>
 *    grant {
 *       permission javax.smartcardio.CardPermission "*", "*";
 *    };
 * </pre>
 */
public class InterfacerApplet extends Applet implements CardTerminalListener, KeyListener, Runnable
{
	private Interfacer interfacer;
	public boolean isRunning = false;
	private DebugFrame debugFrame;
	private boolean debug = false;
	private boolean cardPresent = false;
	private Thread cardCheckThread;

	public InterfacerApplet() {
		try {
			setLayout(new BorderLayout());
			debugFrame = new DebugFrame();
			debugFrame.setVisible(debug);
			this.addKeyListener(this);
			
			interfacer = new Interfacer(debugFrame);
			
			CardManager.getInstance().addCardTerminalListener(this);
			CardManager.getInstance().start();
			//cardPresent = Interfacer.getPassportLink().getStatus().equals("Card present");
			//cardCheckThread = new Thread(this);
			//cardCheckThread.start();
			isRunning = true;
		} catch (Exception e) {
			e.printStackTrace();
			log(e.toString());
			isRunning = false;
			Interfacer.getNetworkLink().stopAccept();
		}
	}
	
	public void paint(Graphics g) {
		Shape circle = new Ellipse2D.Float(25.0f, 25.0f, 25.0f, 25.0f);
		Graphics2D g2 = (Graphics2D) g;
		g2.draw(circle);
		if(cardPresent)
		{
			g2.setPaint(Color.GREEN);
		}
		else
		{
			g2.drawString("Please insert a passport", 0, 10);
			g2.setPaint(Color.RED);
		}
		g2.fill(circle);

	}

	public void init() {
		log("Auth EP - InterfacerApplet - init");
	}

	public void start()	{
		log("Auth EP - InterfacerApplet - start");
	}

	public void stop() {
		log("Auth EP - InterfacerApplet - stop");
		isRunning = false;
		CardManager.getInstance().stop();
		Interfacer.getNetworkLink().stopAccept();
	}

	public void destroy() {
		log("Auth EP - InterfacerApplet - destroy");
		isRunning = false;
		CardManager.getInstance().stop();
		Interfacer.getNetworkLink().stopAccept();
	}

	private void log(String msg) {
		debugFrame.log(msg);
	}

	@Override
	public void cardInserted(CardEvent ce) {
		cardPresent = true;
		log("CARD INSERTED");
		this.repaint();
	}

	@Override
	public void cardRemoved(CardEvent ce) {
		cardPresent = false;
		log("CARD REMOVED");
		this.repaint();
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
		if(e.getKeyChar() == 'D')
		{
			debug = true;
			debugFrame.setVisible(debug);
			log("DEBUG ON");
		}
		
	}

	@Override
	public void run() {
		try {
			while(isRunning)
			{
				Thread.sleep(1000);
				cardPresent = Interfacer.getPassportLink().getStatus().equals("Card present");
				this.repaint();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
