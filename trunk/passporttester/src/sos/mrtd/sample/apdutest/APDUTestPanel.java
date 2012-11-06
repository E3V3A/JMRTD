package sos.mrtd.sample.apdutest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.security.GeneralSecurityException;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.ISO7816;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.PassportApduService;

import sos.mrtd.sample.CommandAPDUField;

/**
 * Complete test panel for testing APDUs.
 *
 * @author Henning Richter (hrichter@fh-lausitz.de)
 *
 * @version $Revision: 1 $
 */
public class APDUTestPanel extends JPanel implements ISO7816,ActionListener{

    public static final String DEF_FILE_NAME = "apdu_default.txt";
    public static final String DEF_SAVE_LOCATION = ".";
    
    private File saveLocation = new File(DEF_SAVE_LOCATION);
    
	private static final byte CLA = 0x00; 
	private CommandAPDUField bApduField, eApduField;	
	private static final Border Panel_Border = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
	private JTextArea area;
	private DrawImage paintarea;
	private JTextField field;
	private JButton testButton, deleteButton, helpButton;
	final private JRadioButton rb1,rb2,rb3,rb4,standard, specific, rb5, rb6;
	private PassportApduService apduService;
	private JFileChooser fileChooser;
    private JCheckBox defFileBox;
	private File saveFile;
	private int caseselect = 5, lineCount=1, rapdusave, p1=0, p2=0, le=0, stle;
	private boolean status;
	public FileWriter filewriter;	
	private FileCompare filecomp;

	public APDUTestPanel(PassportApduService service) throws GeneralSecurityException{
//-----------------GUI------------------------------
		super(new BorderLayout());
		this.apduService = service;
		
		JPanel contentPane = new JPanel(new GridLayout(3,1));
		
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		if(true){
			c.fill = GridBagConstraints.HORIZONTAL;
		}
		
		JPanel rbuttonPanel = new JPanel(new GridLayout(6,1));
		JPanel specifyPanel = new JPanel(new GridLayout(1,2));
		
        defFileBox = new JCheckBox("Use default save file", false);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        buttonPanel.add(defFileBox,c);

        testButton = new JButton("test APDUs");
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;
		buttonPanel.add(testButton,c);
		deleteButton = new JButton("delete");
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 0;
		buttonPanel.add(deleteButton,c);
		helpButton = new JButton("?");
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 3;
		c.gridy = 0;
		buttonPanel.add(helpButton,c);
		paintarea = new DrawImage();
		paintarea.setBackground(Color.white);
		c.fill = GridBagConstraints.VERTICAL;
		c.gridx = 4;
		c.gridheight = 2;
		c.gridwidth = 3;
		c.gridy = 0;
		buttonPanel.add(paintarea,c);
		JLabel label = new JLabel("Nationality");
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		buttonPanel.add(label,c);
		field = new JTextField();
		field.setBackground(Color.white);
		field.setEditable(false);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridwidth = 2;
		c.gridy = 1;
		buttonPanel.add(field,c);
		
		testButton.addActionListener(this);
		deleteButton.addActionListener(this);
		helpButton.addActionListener(this);
		
		rb1 = new JRadioButton("case 1:    C-APDU: Header | R-APDU: SW");
		rb2 = new JRadioButton("case 2:    C-APDU: Header + Le | R-APDU: data + SW");
		rb3 = new JRadioButton("case 3:    C-APDU: Header + Lc + data | R-APDU: SW");
		rb4 = new JRadioButton("case 4:    C-APDU: Header + Lc + data + Le | R-APDU: data + SW");
		rb5 = new JRadioButton("Le = 0");
		rb5.setSelected(true);
		rb6 = new JRadioButton("Le = 1");
		standard = new JRadioButton("standard instruction set");
        standard.setSelected(true);
		specific = new JRadioButton("specify instruction set");

		rb1.addActionListener(this);
		rb2.addActionListener(this);
		rb3.addActionListener(this);
		rb4.addActionListener(this);
		rb5.addActionListener(this);
		rb6.addActionListener(this);
		standard.addActionListener(this);
		specific.addActionListener(this);
		final ButtonGroup radiogroup = new ButtonGroup();
			radiogroup.add(rb1);
			radiogroup.add(rb2);
			radiogroup.add(rb3);
			radiogroup.add(rb4);
			radiogroup.add(standard);
			radiogroup.add(specific);
		final ButtonGroup rbg2 = new ButtonGroup();
			rbg2.add(rb5);
			rbg2.add(rb6);
			
		rbuttonPanel.add(rb1);
		rbuttonPanel.add(rb2);
		rbuttonPanel.add(rb3);
		rbuttonPanel.add(rb4);
		JPanel standardP = new JPanel(new GridLayout(1,3));
			standardP.add(standard);
			standardP.add(rb5);
			standardP.add(rb6);
		rbuttonPanel.add(standardP);
		rbuttonPanel.add(specific);
		
		JPanel beginPanel = new JPanel(new FlowLayout());
			beginPanel.setBorder(BorderFactory.createTitledBorder(Panel_Border,
			"Begin APDU"));
			bApduField = new CommandAPDUField();
			beginPanel.add(bApduField);
					
		JPanel endPanel = new JPanel(new FlowLayout());
		endPanel.setBorder(BorderFactory.createTitledBorder(Panel_Border,
        	"End APDU"));
			eApduField = new CommandAPDUField();
			endPanel.add(eApduField);
			
		specifyPanel.add(beginPanel);
		specifyPanel.add(endPanel);
		
		contentPane.setBorder(BorderFactory.createTitledBorder(Panel_Border,"APDU-Test"));
		contentPane.add(buttonPanel);
		contentPane.add(rbuttonPanel);
		contentPane.add(specifyPanel);
		area = new JTextArea(5, 10);
	    area.setEditable(false);
	    add(contentPane, BorderLayout.NORTH);
	    add(new JScrollPane(area), BorderLayout.SOUTH);
	}

//--------Filesaving----------	
    
    private void saveFile(boolean status, boolean defFile){
		if(defFile) {
            saveFile = new File (DEF_FILE_NAME);
            status = true;            
        }else{
            fileChooser = new JFileChooser();
            if(saveLocation == null || !saveLocation.isDirectory() || !saveLocation.exists()) {
                saveLocation = new File(".");
            }
            fileChooser.setCurrentDirectory (saveLocation);            
            int returnVal = fileChooser.showSaveDialog(APDUTestPanel.this);
            if(returnVal == JFileChooser.CANCEL_OPTION){
                append("Filesaving canceled\n");
                status = false;
            }else if(returnVal == JFileChooser.APPROVE_OPTION){
                saveFile = fileChooser.getSelectedFile();
                status = true;
                if(saveFile.exists()){
                    int response = JOptionPane.showConfirmDialog(null,
                            "File aleady exists - Overwrite?","Confirm Overwrite",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if(response == JOptionPane.CANCEL_OPTION){
                        append("Overwriting canceled\n");
                        status = false;
                    }else{
                        status = true;
                    }       
                }
            }
        }
		if(status == true){
			try{
				filewriter = new FileWriter(saveFile);
				append("Saving file to: "+saveFile.getAbsolutePath()+"\n");
			}catch(IOException e){}
		}

		switchCase(status);
	}
	
//-------SwitchCases------------
	private void switchCase(boolean status){
		if(status == true){
			switch(caseselect){
				case 1:		case1();
							break;
				case 2:		case2();
							break;
				case 3:		case3();
							break;
				case 4:		case4();
							break;
				case 5:		stINSset(stle);
							break;
				case 6:		specificCase();
							break;
				default:	JOptionPane.showMessageDialog(null,
						"case 1: Header only\ncase 2: Header + Le\n" +
						"case 3: Header + Lc + Data\n" +
						"case 4: Header + Lc + Data + Le","Choose a case!",
						JOptionPane.INFORMATION_MESSAGE);	
			}
		}
	}
	
//-------TextAppending----------
	public void append(String txt){
		area.append(txt);
	    area.setCaretPosition(area.getDocument().getLength() - 1);
	}
	
	//case6
	private void sendAPDU(CommandAPDU bApdu, CommandAPDU eApdu) throws CardServiceException {
		byte[] a = bApdu.getBytes();
	    byte[] b = eApdu.getBytes();
//	    if (a.length != b.length) {
//	    	throw new IllegalArgumentException("APDUs should have same length");
//	      }
	    if((bApdu.getNe() > eApdu.getNe())&& bApdu.getINS()<= eApdu.getINS()){
	    	throw new IllegalArgumentException("Start-Le should be greater than End-Le");
	    }
/*	    if(bApdu.getNe() < eApdu.getNe()){
	    	System.out.println("Le von bApdu < Le von eApdu");
	    	for(int aLe = bApdu.getNe(); aLe <= eApdu.getNe(); aLe++){
	    		System.out.println("aLe: "+aLe);
	    	}
	    }*/	
	    
	    byte[] c = new byte[a.length];
	    for (int i = 0; i < a.length; i++) {
	    	c[i] = (byte) Math.min(a[i] & 0x000000FF, b[i] & 0x000000FF);
	     }
	    sendAll(a,b,c,0);
	}
	
	private void sendAll(byte[] a, byte[] b, byte[] c, int offset) throws CardServiceException {
		int n = a.length - offset;
		CommandAPDU capdu,eapdu;
		eapdu = new CommandAPDU(b);
		if(n > 0){
			int min = Math.min(a[offset] & 0x000000FF, b[offset] & 0x000000FF);
	        int max = Math.max(a[offset] & 0x000000FF, b[offset] & 0x000000FF);
	        for(int i=min; i<=max;i++){
	        	c[offset] = (byte)i;
	        	sendAll(a, b, c, offset + 1);
	        } 
		}else{
			capdu = new CommandAPDU(c);
			ResponseAPDU rapdu = apduService.transmit(capdu);
			writeFile(saveFile, capdu, rapdu);
			if(eapdu.equals(capdu)){
				append("APDU test completed.\n");
			}
		}
	}
	
	//case1 & standard instruction set
	public byte[] sendTestAPDU(int cla,int ins,int p1,int p2) throws CardServiceException {
		CommandAPDU capdu = new CommandAPDU(CLA,ins, p1,p2);
		ResponseAPDU rapdu = apduService.transmit(capdu);
		writeFile(saveFile,/* str4,*/capdu, rapdu);
		rapdusave = rapdu.getSW();
		
		return rapdu.getData();
	}
	//case2
	public byte[] sendTestAPDU(int cla, int ins, int p1, int p2, int le)
      throws CardServiceException {
		CommandAPDU capdu = new CommandAPDU(CLA,ins,p1,p2,le);
		ResponseAPDU rapdu = apduService.transmit(capdu);
		writeFile(saveFile,/* str4,*/capdu, rapdu);
		rapdusave = rapdu.getSW();
						
		return rapdu.getData();
	}
	//case3
	public byte[] sendTestAPDU(int cla, int ins, int p1, int p2,
								byte[] data) throws CardServiceException {
		CommandAPDU capdu = new CommandAPDU(CLA,ins,p1,p2,data);
		ResponseAPDU rapdu = apduService.transmit(capdu);
		rapdusave = rapdu.getSW();
		writeFile(saveFile,capdu,rapdu);
		
		return rapdu.getData();
		
	}
	//case4
	public byte[] sendtTestAPDU(int cla, int ins, int p1, int p2,
							 byte[] data, int le) throws CardServiceException {
		CommandAPDU capdu = new CommandAPDU(CLA,ins,p1,p2,data,le);
		ResponseAPDU rapdu = apduService.transmit(capdu);
		rapdusave = rapdu.getSW();
		writeFile(saveFile, capdu, rapdu);
		
		return rapdu.getData();
	}
	
//-------Filewriting------------
	private void writeFile(File file, CommandAPDU capdu, ResponseAPDU rapdu){
		  if(!(swToString((short)rapdu.getSW())).equals("INS NOT SUPPORTED")){
			String str0 = "Instruction: " + Integer.toHexString(capdu.getINS())+"\tP1: " 
							+Integer.toHexString(capdu.getP1())+"\tP2: "
							+Integer.toHexString(capdu.getP2())+"\n";	
			String str1 ="C: "+Integer.toHexString(capdu.getINS())+" "+ capdu.toString()+"\t"; 
			String str2 =" R: "+Hex.toHexString(rapdu.getBytes())+ "("+ 
						swToString((short)rapdu.getSW())+")";
			String str4 = "\n\n";
			String str5 = str0 + str1 + str2+str4;
			
			try{
				BufferedReader breader = new BufferedReader(new StringReader(str5));
				PrintWriter out = new PrintWriter(new BufferedWriter (filewriter));
				String s = str5;
				while((s = breader.readLine()) != null)
					out.println(lineCount++ + ": " + s);

				out.flush();			
			}catch(IOException e){append("Error"+ e);}
		  }
//		  }
		}
	
	private void compare(){
		filecomp = new FileCompare(saveFile, this);
		field.setText(filecomp.getNat());
        if((filecomp.getNat()).equals("Dutch"))
            paintarea.update(FileCompare.getPath()+"netherlands.png");
		if((filecomp.getNat()).equals("Dutch (trial version)"))
			paintarea.update(FileCompare.getPath()+"netherlands.png");
		else if((filecomp.getNat()).equals("Swedish"))
			paintarea.update(FileCompare.getPath()+"sweden.png");
		else if((filecomp.getNat()).equals("German")){
            try {
			cmpEspGer();
            }catch(CardServiceException cse) {
                
            }
		}
		else if((filecomp.getNat()).equals("Italian"))
			paintarea.update(FileCompare.getPath()+"italy.png");
		else if((filecomp.getNat()).equals("France"))
			paintarea.update(FileCompare.getPath()+"france.png");
		else if((filecomp.getNat()).equals("Australian"))
			paintarea.update(FileCompare.getPath()+"australia.png");
		else if((filecomp.getNat()).equals("Greece"))
			paintarea.update(FileCompare.getPath()+"greece.png");
		else if((filecomp.getNat()).equals("Belgian"))
			paintarea.update(FileCompare.getPath()+"belgium.png");
		else if((filecomp.getNat()).equals("Polish"))
			paintarea.update(FileCompare.getPath()+"poland.png");
	}
	
	public void cmpEspGer() throws CardServiceException {
		int lc = 40,ins = 130,p1=0,p2=0;
		byte[] data = new byte[lc];
		for(int i=0;i<data.length;i++){
            data[i] = (byte)0x01;
		}
		sendTestAPDU(CLA, (byte)ins, (short)p1,(short) p2, data);
		if((swToString((short)rapdusave)).equals("CONDITIONS NOT SATISFIED")){
			field.setText("Spanish");
			paintarea.update(FileCompare.getPath()+"spain.png");
		}else{
            field.setText("German");
            paintarea.update(FileCompare.getPath()+"german.png");
		}
	}
	
	private void help(){
		JOptionPane.showMessageDialog(null,
				"You can test the APDU instructions. The standard instruction " +
				"set Test only tests for supported instructions.\n The predefined " +
				"cases 1-4 may take several hours.\n In the specific test you can " +
				"set start- and end-APDU manually.\n '00' is the standard CLA, " +
				"Ins is the instruction you want to test, P1 and P2 are " +
				"the parameters (their range is 00 - FF).\n " +
				"If you expect any answer you can toggle the checkbox and the " +
				"size of bytes you expect as answer.\n If you want to send additional " +
				"command-data use the arrow keys and than file in the fields with bytes.\n" +
				"Then click the Test-APDUs button and choose a file where you want to\n" +
				"save the results (don't forget the '.txt').","Help",
				JOptionPane.INFORMATION_MESSAGE);
	}
	

	
//-------APDU-Test-Cases--------
//-------standard instruction set-----
	//test APDUs in range from 0x44 - 0xE4
	public void stINSset(int stle){
		try{
			int ins = 68;	//0x44
			sendTestAPDU(CLA,ins,p1,p2,stle);
			ins = 130;		//0x82
			sendTestAPDU(CLA,ins,p1,p2,stle);
			ins = 132;		//0x84
			sendTestAPDU(CLA,ins,p1,p2,stle);
			ins = 136;		//0x88
			sendTestAPDU(CLA,ins,p1,p2,stle);
			ins = 164;		//0xA4
			sendTestAPDU(CLA,ins,p1,p2,stle);
			ins = 176;		//0xB0
			sendTestAPDU(CLA,ins,p1,p2,stle);
			ins = 177;		//0xB1
			sendTestAPDU(CLA,ins,p1,p2,stle);
			
		}catch(Exception e){append("Error: "+e);}
		append("APDU test completed.\n");
		compare();
	}
	
//-------specificCase----------
	
	public void specificCase(){
		apduService.setListenersState(false);
		try{
			System.out.println("createAPDU");
			CommandAPDU bApdu = bApduField.getAPDU();
			CommandAPDU eApdu = eApduField.getAPDU();
			System.out.println("bAPDU: "+ bApdu);
			System.out.println("eAPDU: "+ eApdu);
			sendAPDU(bApdu,eApdu);
		}catch(Exception e){append("Error: "+e);}
        apduService.setListenersState(true);
	}

	
//-------Case 1-----------------
	public void case1(){
        apduService.setListenersState(false);
		try{	
insup:		for(int ins=0; ins < 112; ins++){
				sendTestAPDU(CLA,/*(byte)*/ ins,/*(short)*/p1,/*(short)*/p2);
				if((swToString((short)rapdusave)).equals("INS NOT SUPPORTED"))
					continue insup;
				if(((swToString((short)rapdusave)).equals("WRONG P1P2"))||
				(swToString((short)rapdusave)).equals("INCORRECT P1P2")){
					for(p1=0;p1<256;p1++){
						for(p2=0;p2<256;p2++){
							sendTestAPDU(CLA, ins,p1,p2);
							System.out.println("Ins: "+Integer.toHexString(ins)+"\tP1: "+p1+"\tP2: "+p2	);
						}
					}
				}
			}					
insup:		for(int ins=113; ins < 256; ins++){
				sendTestAPDU(CLA, ins,p1,p2);
				System.out.println(Integer.toHexString(ins));
				if((swToString((short)rapdusave)).equals("INS NOT SUPPORTED"))
					continue insup;
				if(((swToString((short)rapdusave)).equals("WRONG P1P2"))||
				(swToString((short)rapdusave)).equals("INCORRECT P1P2")){
					for(p1=0;p1<256;p1++){
						for(p2=0;p2<256;p2++){
							sendTestAPDU(CLA,ins,p1,p2);
							System.out.println("Ins: "+Integer.toHexString(ins)+"\tP1: "+p1+"\tP2: "+p2	);
						}
					}
				}
			}
		}catch(Exception e){append("Error: "+e);}
		append("APDU test completed.\n");
        apduService.setListenersState(true);
	}
//------------------Case 2------------
	public void case2(){
        apduService.setListenersState(false);
		try{
insup:		for(int ins = 0; ins < 112; ins++){
				sendTestAPDU(CLA, ins, p1, p2,le);
				System.out.println(Integer.toHexString(ins));
				if((swToString((short)rapdusave)).equals("INS NOT SUPPORTED"))
					continue insup;
				for(le=0;le<256;le++){
					sendTestAPDU(CLA,(byte)ins, (short)p1, (short)p2, (int)le);
					if(((swToString((short)rapdusave)).equals("WRONG P1P2"))||
					(swToString((short)rapdusave)).equals("INCORRECT P1P2")){
						for(p1=0; p1<256; p1++){
							for(p2=0; p2<256; p2++){
								sendTestAPDU(CLA,ins, p1, p2, le);
							}
						}
					}
				}
			}
insup:		for(int ins = 113; ins < 256; ins++){
				sendTestAPDU(CLA, ins, p1, p2,le);
				System.out.println(Integer.toHexString(ins));
				if((swToString((short)rapdusave)).equals("INS NOT SUPPORTED"))
					continue insup;
				for(le=0;le<256;le++){
					sendTestAPDU(CLA, ins, p1, p2, le);
					if(((swToString((short)rapdusave)).equals("WRONG P1P2"))||
					(swToString((short)rapdusave)).equals("INCORRECT P1P2")){
						for(p1=0; p1<256; p1++){
							for(p2=0; p2<256; p2++){
								sendTestAPDU(CLA,ins, p1, p2, le);
							}
						}
					}
				}
			}		
		}catch(Exception e){append("Error: "+e);}
		append("APDU test completed.\n");
        apduService.setListenersState(true);
	}
	//--------------------Case 3----------------
	public void case3(){
        apduService.setListenersState(false);
		try{
insup:		for(int ins = 0;ins < 112;ins++){
				sendTestAPDU(CLA,ins,p1,p2);
				System.out.println("Ins: "+Integer.toHexString(ins));
				if((swToString((short)rapdusave)).equals("INS NOT SUPPORTED"))
					continue insup;
				if(((swToString((short)rapdusave)).equals("WRONG P1P2"))||
						(swToString((short)rapdusave)).equals("INCORRECT P1P2")){
					for(p1=0;p1<256;p1++){
						for(p2=0;p2<256;p2++){
							for(int lc=0; lc<256; lc++){
								System.out.println("Lc: "+lc);
								byte[] data = new byte[lc];
								for(int i=0; i<data.length; i++){
									//fill data[i] with 0x01 until end of array reached
                                    data[i] = (byte)0x01;
								}
								sendTestAPDU(CLA, ins, p1, p2, data);
							}
						}
					}
				}		
			}
		
insup:		for(int ins=113;ins<256;ins++){
				sendTestAPDU(CLA,ins,p1,p2);
				System.out.println("Ins: "+Integer.toHexString(ins));
				if((swToString((short)rapdusave)).equals("INS NOT SUPPORTED"))
					continue insup;
				if((swToString((short)rapdusave)).equals("WRONG LENGTH")){
				for(p1=0;p1<256;p1++){
					for(p2=0;p2<256;p2++){
						for(int lc=0; lc<256; lc++){
							System.out.println("Lc: "+lc);
							byte[] data = new byte[lc];
							for(int i=0; i<data.length; i++){
								//fill data[i] with 0x01 until end of array reached
                                data[i] = (byte)0x01;
							}
							sendTestAPDU(CLA, ins, p1, p2, data);
						}
					}
				}					
				}	
			}				
		}catch(Exception e){append("Error: "+e);}
		append("APDU test completed.\n");
        apduService.setListenersState(true);
	}
	//-------------------Case4---------------------
	public void case4(){
        apduService.setListenersState(false);
		try{
insup:	for(int ins=0;ins <112; ins++){
			sendTestAPDU(CLA,ins,p1,p2);
			System.out.println(Integer.toHexString(ins));
			if((swToString((short)rapdusave)).equals("INS NOT SUPPORTED"))
			continue insup;
			if((swToString((short)rapdusave)).equals("WRONG LENGTH")){
			for(p1=0;p1<256;p1++){
				for(p2=0;p2<256;p2++){
					for(int lc=0;lc<256;lc++){
						byte[] data = new byte[lc];
						for(int i=0; i<data.length;i++){
							//fill data[i] with 0x01 until end of array reached
                            data[i] = (byte)0x01;
						}
						for(le=0;le<256;le++){
							sendtTestAPDU(CLA, ins, p1, p2,
												data, le);
//							System.out.println("Ins: "+Integer.toHexString(ins)+" P1: "+p1
//									+" P2: "+p2+" Lc: "+lc+" I: "+data.length+"Le: "+le);
						}
						sendTestAPDU(CLA, ins, p1, p2, data);
					}
				}	
			}		
			}
		}
	
insup:	for(int ins=113;ins <256; ins++){
			sendTestAPDU(CLA,ins,p1,p2);
			System.out.println(Integer.toHexString(ins));
			if((swToString((short)rapdusave)).equals("INS NOT SUPPORTED"))
				continue insup;
			if((swToString((short)rapdusave)).equals("WRONG LENGTH")){
			for(p1=0;p1<256;p1++){
				for(p2=0;p2<256;p2++){
					for(int lc=0;lc<256 ;lc++){
						byte[] data = new byte[lc];
						for(int i=0; i<data.length;i++){
							//fill data[i] with 0x01 until end of array reached
                            data[i] = (byte)0x01;
						}
						for(le=0;le<256;le++){
							sendtTestAPDU(CLA, ins, p1, p2,
												data, le);
							System.out.println("Ins: "+Integer.toHexString(ins)+" P1: "+p1
							+" P2: "+p2+" Lc: "+lc+" I: "+data.length+"Le: "+le);
						}
						sendTestAPDU(CLA, ins, p1, p2, data);
					}
				}
			}
			}
		}
			
		}catch(Exception e){append("Error: "+e);}
		append("APDU test completed.\n");
        apduService.setListenersState(true);
	}
	
//-------swToString------------
	
	   private static String swToString(short sw) {
		      switch(sw) {
		         case SW_BYTES_REMAINING_00: return "BYTES REMAINING";
		         case SW_END_OF_FILE: return "END OF FILE";
		         case SW_WRONG_LENGTH: return "WRONG LENGTH";
		         case SW_SECURITY_STATUS_NOT_SATISFIED: return "SECURITY STATUS NOT SATISFIED";
		         case SW_FILE_INVALID: return "FILE INVALID";
		         case SW_DATA_INVALID: return "DATA INVALID";
		         case SW_CONDITIONS_NOT_SATISFIED: return "CONDITIONS NOT SATISFIED";
		         case SW_COMMAND_NOT_ALLOWED: return "COMMAND NOT ALLOWED";
		         case SW_APPLET_SELECT_FAILED: return "APPLET SELECT FAILED";
		         case SW_KEY_USAGE_ERROR: return "KEY USAGE ERROR";
		         case SW_WRONG_DATA: return "WRONG DATA";
		         case SW_FUNC_NOT_SUPPORTED: return "FUNC NOT SUPPORTED";
		         case SW_FILE_NOT_FOUND: return "FILE NOT FOUND";
		         case SW_RECORD_NOT_FOUND: return "RECORD NOT FOUND";
		         case SW_FILE_FULL: return "FILE FULL";
		         case SW_INCORRECT_P1P2: return "INCORRECT P1P2";
		         case SW_KEY_NOT_FOUND: return "KEY NOT FOUND";
		         case SW_WRONG_P1P2: return "WRONG P1P2";
		         case SW_CORRECT_LENGTH_00: return "CORRECT LENGTH";
		         case SW_INS_NOT_SUPPORTED: return "INS NOT SUPPORTED";
		         case SW_CLA_NOT_SUPPORTED: return "CLA NOT SUPPORTED";
		         case SW_UNKNOWN: return "UNKNOWN";
		         case SW_CARD_TERMINATED: return "CARD TERMINATED";
		         case SW_NO_ERROR: return "NO ERROR";
		      }
		      return "";
		   }
//-------ActionListener---------
	
	public void actionPerformed(ActionEvent ae){
		Object source = ae.getSource();
		if(source == testButton){
			lineCount=1;
			saveFile(status, defFileBox.isSelected());
		}
		if(source == deleteButton){	area.setText("");}
		if(source == helpButton){ help();}
		if(rb1.isSelected() == true){ caseselect = 1;}
		if(rb2.isSelected() == true){ caseselect = 2;}
		if(rb3.isSelected() == true){ caseselect = 3;}
		if(rb4.isSelected() == true){ caseselect = 4;}
		if(standard.isSelected() == true){ caseselect = 5;}
		if(specific.isSelected() == true){ caseselect = 6;}	
		if(rb5.isSelected() == true){ stle = 0; }
		if(rb6.isSelected() == true){ stle = 1;	}

	}
}
