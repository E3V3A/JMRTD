/*
 *  AuthEP - Interfacer.
 *
 *  Copyright (C) 2009  Telematica Instituut
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.telin.authep;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Ellipse2D;

import net.sourceforge.scuba.smartcards.CardEvent;
import net.sourceforge.scuba.smartcards.CardManager;
import net.sourceforge.scuba.smartcards.CardTerminalListener;

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
	private static final long serialVersionUID = 8536237157919971795L;

	private Interfacer interfacer;
	public boolean isRunning = false;
	private DebugFrame debugFrame;
	private boolean debug = false;
	private boolean cardPresent = false;
//	private Thread cardCheckThread;

	public InterfacerApplet() {
		try {
			setLayout(new BorderLayout());
			debugFrame = new DebugFrame();
			debugFrame.setVisible(debug);
			this.addKeyListener(this);

			interfacer = new Interfacer(debugFrame);

			CardManager.getInstance().addCardTerminalListener(this);
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
//		CardManager.getInstance().stop();
		Interfacer.getNetworkLink().stopAccept();
	}

	public void destroy() {
		log("Auth EP - InterfacerApplet - destroy");
		isRunning = false;
//		CardManager.getInstance().stop();
		Interfacer.getNetworkLink().stopAccept();
	}

	private void log(String msg) {
		debugFrame.log(msg);
	}

	public void cardInserted(CardEvent ce) {
		cardPresent = true;
		log("CARD INSERTED");
		this.repaint();
	}

	public void cardRemoved(CardEvent ce) {
		cardPresent = false;
		log("CARD REMOVED");
		this.repaint();
	}

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
		if(e.getKeyChar() == 'D')
		{
			debug = true;
			debugFrame.setVisible(debug);
			log("DEBUG ON");
		}
	}

	public void run() {
		try {
			while(isRunning)
			{
				Thread.sleep(1000);
				cardPresent = Interfacer.getPassportLink().getStatus().equals("Card present");
				this.repaint();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
