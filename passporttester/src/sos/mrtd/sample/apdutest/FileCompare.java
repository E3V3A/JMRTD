package sos.mrtd.sample.apdutest;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;

import javax.swing.JFileChooser;

public class FileCompare {
	
	public final static String DEFAULT_PATH = "./fingerprintfiles";

    private static File fingerprintPath = new File(DEFAULT_PATH);
    
    private File[] fileArray;
	private File compareFile, saveFile;
	private JFileChooser jfc;
	private boolean status;
	private String nat;
	
	public FileCompare(File saveFile, Component parent){
		this.saveFile = saveFile;
		status = false;
		try{
			BufferedReader br2 = new BufferedReader(new FileReader(saveFile));
			String fullFile2 = "";
			String input2 = "";
			while((input2 = br2.readLine()) != null){
				fullFile2 += input2;
			}
			
			File f = getFingerprintDir(parent);
			fileArray = f.listFiles(new FilenameFilter() { 
                public boolean accept(File f, String name) {
                    return name.endsWith(".txt");
                }
            });
		
			for(int i=0; i < fileArray.length; i++){
				if(status == false){
					compareFile = fileArray[i];
                    //System.out.println("Compare file: "+compareFile);
					comparison(compareFile, fullFile2);
				}else{
					getChoice(compareFile);
					break;
				}
			setNat("Unknown");
			}			
		}catch(Exception e){}
	}

    private File getFingerprintDir(Component parent) {
      if(fingerprintPath != null && fingerprintPath.isDirectory() && fingerprintPath.exists()) {
          return fingerprintPath;
      }
      jfc = new JFileChooser();
      jfc.setDialogTitle("Choose fingerprint directory...");
      jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      jfc.showOpenDialog(parent);
      File f = jfc.getSelectedFile();
      if(f != null && f.isDirectory() && f.exists()) {
          fingerprintPath = f;
          return f;
      }
      return new File(".");      
    }
    
	public void comparison(File compareFile, String fullFile2){
        //System.out.println("Comparing files.");
		try{
			BufferedReader br1 = new BufferedReader(new FileReader(compareFile));
			String fullFile1 = "";
			String input1 = "";
			while((input1 = br1.readLine()) != null){
				fullFile1 += input1;
			}
			if(fullFile1.equals(fullFile2)){
				//System.out.println("-=EQUAL=-");
				status = true;
			}		
		}catch(Exception e){}	
	}
	
	public void getChoice(File compareFile){
		//System.out.println("File "+saveFile.getName()+" is equal to: "+compareFile.getName());
		if((compareFile.getName()).equals("swedish.txt"))
			{setNat("Swedish");}
		else if((compareFile.getName()).equals("dutch trial passport.txt"))
			{setNat("Dutch (trial version)");}
		else if((compareFile.getName()).equals("dutch id.txt"))
			{setNat("Dutch ID");}
        else if((compareFile.getName()).equals("dutch.txt"))
            {setNat("Dutch");}
		else if((compareFile.getName()).equals("german.txt"))
			{setNat("German");}
		else if((compareFile.getName()).equals("italian.txt"))
			{setNat("Italian");}
		else if((compareFile.getName()).equals("french.txt"))
			{setNat("France");}
		else if((compareFile.getName()).equals("australian.txt"))
			{setNat("Australian");}
		else if((compareFile.getName()).equals("greek.txt"))
			{setNat("Greece");}
		else if((compareFile.getName()).equals("belgian.txt"))
			{setNat("Belgian");}
		else if((compareFile.getName()).equals("polish.txt"))
			{setNat("Polish");}
	}
	
	public void setNat(String nat){
		this.nat = nat;
	}
	
	public String getNat(){
		return nat;
	}

    public static String getPath() {
        return fingerprintPath.getAbsolutePath() + File.separatorChar;
    }
    
}
