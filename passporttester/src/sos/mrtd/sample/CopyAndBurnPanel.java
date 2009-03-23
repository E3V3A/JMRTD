package sos.mrtd.sample;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.smartcardio.CardTerminal;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sourceforge.scuba.smartcards.CardServiceException;

import org.jmrtd.AAEvent;
import org.jmrtd.AuthListener;
import org.jmrtd.BACEvent;
import org.jmrtd.EACEvent;
import org.jmrtd.PassportPersoService;
import org.jmrtd.PassportService;

/**
 * Class for copying and burning passports
 * 
 * @author ceesb (ceeesb@gmail.com)
 *
 */
public class CopyAndBurnPanel extends JPanel implements Runnable, ActionListener,
		AuthListener {
	private static final Border PANEL_BORDER = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

	private CopyAndBurnPanel instance;
	private JComboBox readers1, readers2;
	private CardTerminal terminal;
//	final private APDULogPanel log;
	private PassportService currentService;
	
	public CopyAndBurnPanel(PassportService ps) throws CardServiceException {
		super(new BorderLayout());
		instance = this;
//		log = logPanel;
		currentService = ps;
		JPanel copyPanel = new JPanel(new FlowLayout());
		JPanel burnPanel = new JPanel(new FlowLayout());
		JPanel clonePanel = new JPanel(new FlowLayout());
		final JButton copyButton = new JButton("Copy");
		final JButton burnButton = new JButton("Burn");
		final JButton cloneButton = new JButton("Clone");
		
		copyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {				
				copyButton.setEnabled(false);
				JFileChooser chooser = new JFileChooser();
				chooser.setDialogTitle("Choose ZIP file name to store Passport data");
				chooser.setFileHidingEnabled(false);
				
				int n = chooser.showOpenDialog(instance);
				if (n != JFileChooser.APPROVE_OPTION) {
					System.out.println("DEBUG: select file canceled...");
					return;
				}
				final File f = chooser.getSelectedFile();
				final PassportPersoService persoService;

				try {
					persoService = new PassportPersoService(currentService);
				} catch (CardServiceException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return;
				}

				(new Thread(new Runnable() {
					public void run() {						
							try {
								persoService.dumpPassport(f);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							copyButton.setEnabled(true);
					}					
				})).start();			
			}
		});
		
		burnButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				burnButton.setEnabled(false);
				JFileChooser chooser = new JFileChooser();
				chooser.setDialogTitle("Choose ZIP file containing Passport data");
				chooser.setFileHidingEnabled(false);

				int n = chooser.showOpenDialog(instance);
				if (n != JFileChooser.APPROVE_OPTION) {
					System.out.println("DEBUG: select file canceled...");
					return;
				}
				final File f = chooser.getSelectedFile();
				final PassportPersoService persoService;
//				final PassportService service;
//				terminal = (CardTerminal)readers1.getSelectedItem();
				try {
//					service = new PassportService(new TerminalCardService(terminal));
					persoService = new PassportPersoService(currentService);
//					service.open();
//					service.addAPDUListener(log);
				} catch (CardServiceException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return;
				}

				(new Thread(new Runnable() {
					public void run() {						
							try {
								persoService.burnPassport(new ZipFile(f));
							} catch (ZipException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (CardServiceException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
//							service.removeAPDUListener(log);
//							service.close();	
							burnButton.setEnabled(true);
					}					
				})).start();
			}
		});

//		Collection<CardTerminal> terminals = CardManager.getInstance()
//				.getTerminals();
//		readers1 = new JComboBox(terminals.toArray());		
//		readers2 = new JComboBox(terminals.toArray());
		
		copyPanel.add(copyButton);
		copyPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER,
				"Copy current passport to ZIP file (do BAC first!)"));
				
		burnPanel.add(burnButton);
		burnPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER,
				"Burn passport from ZIP file"));
		add(copyPanel, BorderLayout.NORTH);
		add(burnPanel, BorderLayout.CENTER);
	}

	public void run() {
		// TODO Auto-generated method stub

	}

	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}

	public void performedAA(AAEvent ae) {
		// TODO Auto-generated method stub

	}

	public void performedBAC(BACEvent be) {
		// TODO Auto-generated method stub

	}

    public void performedEAC(EACEvent ee) {
        // TODO Auto-generated method stub

    }

}
