package sos.mrtd.sample.clone;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Vector;

import javax.smartcardio.CardTerminal;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;

import net.sourceforge.scuba.smartcards.CardManager;
import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.TerminalCardService;

import org.jmrtd.AAEvent;
import org.jmrtd.AuthListener;
import org.jmrtd.BACEvent;
import org.jmrtd.EACEvent;
import org.jmrtd.PassportManager;
import org.jmrtd.PassportPersoService;
import org.jmrtd.PassportService;
import org.jmrtd.SecureMessagingWrapper;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.MRZInfo;

import sos.mrtd.sample.PassportGUI;

public class ClonePanel extends JPanel implements Runnable, ActionListener, AuthListener {

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
                        
//            TerminalFactory tf = TerminalFactory.getInstance("PC/SC", null);
//            CardTerminal[] terminals = null;
//            List<CardTerminal> l = tf.terminals().list();
//            terminals = new CardTerminal[l.size()];
//            int i = 0;
//            for(CardTerminal t : l) {
//                terminals[i++] = t;
//            }
            Collection<CardTerminal> terminals = CardManager.getInstance().getTerminals();
            readers = new JComboBox(terminals.toArray());
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
    
    public void run() {
        try {        
            clone.setEnabled(false);
            readers.setEnabled(false);
            PassportService s = new PassportService(service);
            s.setWrapper(wrapper);
            
            InputStream input = null;
            
            input = s.readFile(PassportService.EF_SOD);

            //new ProgressThread(this, sodStream, "Reading SOD").start();

            byte[] sodContents = getByteArray(input);
            
            input = s.readFile(PassportService.EF_DG1);
            //new ProgressThread(this, dg1Stream, "Reading DG1").start();
            byte[] dg1Contents = getByteArray(input);
            DG1File dg1File = new DG1File(new ByteArrayInputStream(dg1Contents));
            byte[] dg11Contents = null; 
            try {
                input = s.readFile(PassportService.EF_DG11);
                dg11Contents = getByteArray(input);            
            }catch(Exception e) {
                e.printStackTrace();
                System.out.println("DG11 not found on the passport.");
            }

            byte[] dg15Contents = null; 
            try {
                input = s.readFile(PassportService.EF_DG15);
                dg15Contents = getByteArray(input);            
            }catch(Exception e) {
                e.printStackTrace();
                System.out.println("DG15 not found on the passport.");
            }

            input = s.readFile(PassportService.EF_DG2);
            new ProgressThread(this, input, "Reading DG2").start();
            byte[] dg2Contents = getByteArray(input);
            String reader = ((CardTerminal)readers.getSelectedItem()).getName();

            MRZInfo mrzInfo = dg1File.getMRZInfo();

            PassportManager.getInstance().removePassportListener(gui);
            JOptionPane.showMessageDialog(this, "Insert a passport applet card into reader \""+ reader + "\"");
            
            CardService ps = new TerminalCardService((CardTerminal)readers.getSelectedItem());
            newPassport = new PassportPersoService(ps);
            ps.open();
            newPassport.setBAC(mrzInfo.getDocumentNumber(), mrzInfo.getDateOfBirth(), mrzInfo.getDateOfExpiry());

            InputStream in = new ByteArrayInputStream(sodContents);
            
            //new ProgressThread(this, in, "Writing SOD").start();
            newPassport.createFile(PassportService.EF_SOD, (short)sodContents.length);
            newPassport.selectFile(PassportService.EF_SOD);
            newPassport.writeFile(PassportService.EF_SOD, in);
                        
            in = new ByteArrayInputStream(dg1Contents);
            //new ProgressThread(this, in, "Writing DG1").start();
            newPassport.createFile(PassportService.EF_DG1, (short)dg1Contents.length);
            newPassport.selectFile(PassportService.EF_DG1);
            newPassport.writeFile(PassportService.EF_DG1, in);

            if(dg11Contents != null) {
                in = new ByteArrayInputStream(dg11Contents);
                //new ProgressThread(this, in, "Writing DG1").start();
                newPassport.createFile(PassportService.EF_DG11, (short)dg11Contents.length);
                newPassport.selectFile(PassportService.EF_DG11);
                newPassport.writeFile(PassportService.EF_DG11, in);                
            }

            if(dg15Contents != null) {
                in = new ByteArrayInputStream(dg15Contents);
                //new ProgressThread(this, in, "Writing DG1").start();
                newPassport.createFile(PassportService.EF_DG15, (short)dg15Contents.length);
                newPassport.selectFile(PassportService.EF_DG15);
                newPassport.writeFile(PassportService.EF_DG15, in);                
            }

            in = new ByteArrayInputStream(dg2Contents);
            new ProgressThread(this, in, "Writing DG2").start();
            newPassport.createFile(PassportService.EF_DG2, (short)dg2Contents.length);
            newPassport.selectFile(PassportService.EF_DG2);
            newPassport.writeFile(PassportService.EF_DG2, in);
            
            newPassport.lockApplet();

            LinkedList<String> list = new LinkedList<String>();
            
            if(sodContents!=null) list.add("SOD");
            if(dg1Contents!=null) list.add("DG1");
            if(dg2Contents!=null) list.add("DG2");
            if(dg11Contents!=null) list.add("DG11");
            if(dg15Contents!=null) list.add("DG15");
            
            JOptionPane.showMessageDialog(this, "Finished.\n" +
                    "Data groups copied: "+ list + "\nRemove the card.");

            PassportManager.getInstance().addPassportListener(gui);
            clone.setEnabled(true);
            readers.setEnabled(true);
            
        }catch(Exception e) {
            e.printStackTrace();
            clone.setEnabled(true);
            readers.setEnabled(true);
        }
    }
    
    // Woj: there should be a cleaner way to do it!
    // MO: Use SimpleDataFormat
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
            new Thread(this).start();
        }        
    }

    public void performedAA(AAEvent ae) {
    }

    public void performedBAC(BACEvent be) {
        wrapper = be.getWrapper();        
    }

    public void performedEAC(EACEvent ee) {
        wrapper = ee.getWrapper();        
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

