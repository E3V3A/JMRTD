package sos.mrtd.clone;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;

import sos.mrtd.AAEvent;
import sos.mrtd.AuthListener;
import sos.mrtd.BACEvent;
import sos.mrtd.DG1File;
import sos.mrtd.DG2File;
import sos.mrtd.PassportApduService;
import sos.mrtd.PassportManager;
import sos.mrtd.PassportService;
import sos.mrtd.SODFile;
import sos.mrtd.SecureMessagingWrapper;
// import sos.smartcards.ISO7816;
import sos.mrtd.PassportPersoService;
import sos.mrtd.sample.PassportGUI;
import sos.smartcards.CardService;
import sos.smartcards.CardServiceException;
import sos.smartcards.TerminalCardService;

public class ClonePanel extends JPanel implements ActionListener, AuthListener {

    private static final String CLONE = "cloneButton";
    
    private PassportService service = null;
    private PassportPersoService newPassport = null;
    private JComboBox readers = null;
    private JButton clone = null;
    private SecureMessagingWrapper wrapper = null;
    private PassportGUI gui = null;
    
    
    public ClonePanel(PassportService service, PassportGUI gui) throws CardServiceException {
            super(new GridBagLayout());
            this.service = service;
            this.gui = gui;
            
            try {
                TerminalFactory tf = TerminalFactory.getInstance("PC/SC", null);
            CardTerminal[] terminals = null;
            List<CardTerminal> l = tf.terminals().list();
            terminals = new CardTerminal[l.size()];
            int i = 0;
            for(CardTerminal t : l) {
                terminals[i++] = t;
            }
            readers = new JComboBox(terminals);
            }catch(Exception ce) {
              ce.printStackTrace();    
            }
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(5, 5, 5, 5);
            add(new JLabel("Card writing terminal: "),c);
            add(readers,c);
            clone = new JButton("Clone");
            clone.setActionCommand(CLONE);
            clone.addActionListener(this);
            add(clone,c);
    }
    

    private byte[] getByteArray(InputStream in) {
        try {
        Vector<Integer> vec = new Vector<Integer>();
        int c = 0;
        while((c = in.read())!= -1) {
            vec.add(new Integer(c));            
        }
        byte[] result = new byte[vec.size()];
        int index = 0;
        for(Integer i : vec) {
            result[index++] = i.byteValue(); 
        }
        return result;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
       
    }
    
    private void actionClone() {
        try {        
            PassportService s = new PassportService(service);
            s.setWrapper(wrapper);
            InputStream sodStream = s.readFile(PassportService.EF_SOD);

            new ProgressThread(this, sodStream, "Reading SOD").start();

            byte[] sodContents = getByteArray(sodStream);
            sodStream = new ByteArrayInputStream(sodContents);
            SODFile sodFile = new SODFile(sodStream);
            
            InputStream dg1Stream = s.readFile(PassportService.EF_DG1);
            new ProgressThread(this, sodStream, "Reading DG1").start();
            byte[] dg1Contents = getByteArray(dg1Stream);
            DG1File dg1File = new DG1File(new ByteArrayInputStream(dg1Contents));
            InputStream dg2Stream = s.readFile(PassportService.EF_DG2);
            new ProgressThread(this, sodStream, "Reading DG2").start();
            byte[] dg2Contents = getByteArray(dg2Stream);
            String reader = ((CardTerminal)readers.getSelectedItem()).getName();

            String num = dg1File.getMRZInfo().getDocumentNumber();
            String dob = getDateString(dg1File.getMRZInfo().getDateOfBirth());
            String doe = getDateString(dg1File.getMRZInfo().getDateOfExpiry());

            System.out.println("dg1Len: "+dg1Contents.length);
            System.out.println("dg2Len: "+dg2Contents.length);
            System.out.println("Num: "+ num);
            System.out.println("dob: "+ dob);
            System.out.println("doe: "+ doe);

            PassportManager.getInstance().removePassportListener(gui);
            JOptionPane.showMessageDialog(this, "Insert a passport applet card into reader \""+ reader + "\"");
            

            newPassport = new PassportPersoService(new TerminalCardService((CardTerminal)readers.getSelectedItem()));
            newPassport.open();
            newPassport.putMRZ(num.getBytes("ASCII"), dob.getBytes("ASCII"), doe.getBytes("ASCII"));

            InputStream in = new ByteArrayInputStream(sodContents);
            
            new ProgressThread(this, in, "Writing SOD").start();
            newPassport.createFile(null, PassportService.EF_SOD, (short)sodContents.length);
            newPassport.selectFile(null, PassportService.EF_SOD);
            newPassport.writeFile(null, PassportService.EF_SOD, in);
                        
            in = new ByteArrayInputStream(dg1Contents);
            new ProgressThread(this, in, "Writing DG1").start();
            newPassport.createFile(null, PassportService.EF_DG1, (short)dg1Contents.length);
            newPassport.selectFile(null, PassportService.EF_DG1);
            newPassport.writeFile(null, PassportService.EF_DG1, in);

            in = new ByteArrayInputStream(dg2Contents);
            new ProgressThread(this, in, "Writing DG2").start();
            newPassport.createFile(null, PassportService.EF_DG2, (short)dg2Contents.length);
            newPassport.selectFile(null, PassportService.EF_DG2);
            newPassport.writeFile(null, PassportService.EF_DG2, in);
            
            newPassport.lockApplet(null);

            JOptionPane.showMessageDialog(this, "Finished. Remove the card.");

            PassportManager.getInstance().addPassportListener(gui);
            
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    // Woj: there should be a cleaner way to do it!
    private String getDateString(Date date) {
        String s = date.toString();
        String year = s.substring(s.lastIndexOf(' ')+3);
        String day  = s.substring(8, 10);
        String month = s.substring(4, 7);
        if(month.equals("Jan")) month = "01";
        if(month.equals("Feb")) month = "02";
        if(month.equals("Mar")) month = "03";
        if(month.equals("Apr")) month = "04";
        if(month.equals("May")) month = "05";
        if(month.equals("Jun")) month = "06";
        if(month.equals("Jul")) month = "07";
        if(month.equals("Aug")) month = "08";
        if(month.equals("Sep")) month = "09";
        if(month.equals("Oct")) month = "10";
        if(month.equals("Nov")) month = "11";
        if(month.equals("Dec")) month = "12";
        return year+month+day;
    }
    
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals(CLONE)) {
            actionClone();
        }        
    }

    public void performedAA(AAEvent ae) {
    }

    public void performedBAC(BACEvent be) {
        wrapper = be.getWrapper();        
    }

    private class ProgressThread extends Thread {
        private InputStream in;
        private String title = null;
        private JPanel panel = null;
        
        ProgressThread(JPanel panel, InputStream in, String title) {
            this.in = in;
            this.title = title;
            this.panel = panel;
        }
        
        public void run() {
            try {
                int fileLength = in.available();
                ProgressMonitor m = new ProgressMonitor(panel, title, "[" + 0 + "/" + fileLength + "]", 0, in.available());
                  while (in.available() >  0) {
                     Thread.sleep(200);
                     int bytesRead = fileLength - in.available();
                     m.setProgress(bytesRead);
                     m.setNote("[" + bytesRead + "/" + fileLength + "]");
                  }
               } catch (InterruptedException ie) {
               } catch (Exception e) {
               }
            }
         }
    }

