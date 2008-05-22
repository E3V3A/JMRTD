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
            byte[] sodContents = getByteArray(sodStream);
            sodStream = new ByteArrayInputStream(sodContents);
            SODFile sodFile = new SODFile(sodStream);
            
            InputStream dg1Stream = s.readFile(PassportService.EF_DG1);
            byte[] dg1Contents = getByteArray(dg1Stream);
            DG1File dg1File = new DG1File(new ByteArrayInputStream(dg1Contents));
            System.out.println("dg1Len: "+dg1Contents.length);
            InputStream dg2Stream = s.readFile(PassportService.EF_DG2);
            byte[] dg2Contents = getByteArray(dg2Stream);
            System.out.println("dg2Len: "+dg2Contents.length);
            String reader = ((CardTerminal)readers.getSelectedItem()).getName();
            PassportManager.getInstance().removePassportListener(gui);
            JOptionPane.showMessageDialog(this, "Insert a passport applet card into reader \""+ reader + "\"");

            
            String num = dg1File.getMRZInfo().getDocumentNumber();
            String dob = getDateString(dg1File.getMRZInfo().getDateOfBirth());
            String doe = getDateString(dg1File.getMRZInfo().getDateOfExpiry());

            newPassport = new PassportPersoService(new TerminalCardService((CardTerminal)readers.getSelectedItem()));
            newPassport.open();
            newPassport.putMRZ(num.getBytes("ASCII"), dob.getBytes("ASCII"), doe.getBytes("ASCII"));

            newPassport.createFile(null, PassportService.EF_SOD, (short)sodContents.length);
            newPassport.selectFile(null, PassportService.EF_SOD);
            newPassport.writeFile(null, PassportService.EF_SOD, new ByteArrayInputStream(sodContents));

            newPassport.createFile(null, PassportService.EF_DG1, (short)dg1Contents.length);
            newPassport.selectFile(null, PassportService.EF_DG1);
            newPassport.writeFile(null, PassportService.EF_DG1, new ByteArrayInputStream(dg1Contents));

            newPassport.createFile(null, PassportService.EF_DG2, (short)dg2Contents.length);
            newPassport.selectFile(null, PassportService.EF_DG2);
            newPassport.writeFile(null, PassportService.EF_DG2, new ByteArrayInputStream(dg2Contents));
            
            newPassport.lockApplet(null);
            System.out.println("Success.");
            PassportManager.getInstance().addPassportListener(gui);
            
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private String getDateString(Date date) {
        String s = date.toString();
        String year = s.substring(26);
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
        // TODO Auto-generated method stub
        if(e.getActionCommand().equals(CLONE)) {
            actionClone();
        }
        
    }

    public void performedAA(AAEvent ae) {
        // TODO Auto-generated method stub
        
    }

    public void performedBAC(BACEvent be) {
        wrapper = be.getWrapper();
        // TODO Auto-generated method stub
        
    }


}
