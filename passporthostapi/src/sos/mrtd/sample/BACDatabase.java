/*
 * Created on Mar 30, 2007
 */
package sos.mrtd.sample;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class BACDatabase {
     
   private static final File BACDB_FILE =
      new File(PassportGUI.JMRTD_USER_DIR, "bacdb.txt");

   private List<String> entries;

   public BACDatabase() {
      entries = new ArrayList<String>();
      read();
   }
   
   public void addEntry(String documentNumber, String dateOfBirth, String dateOfExpiry) {
	   System.out.println("DEBUG: addEntry");
      String entry = documentNumber.trim() + ","
         + dateOfBirth.trim() + ","
         + dateOfExpiry.trim();
      if (!entries.contains(entry)) {
         entries.add(entry);
         write();
      }
   }
   
   public String getDocumentNumber() {
      return getMostRecentEntry()[0];
   }
   
   public String getDateOfBirth() {
      return getMostRecentEntry()[1];
   }
   
   public String getDateOfExpiry() {
      return getMostRecentEntry()[2];
   }
   
   private String[] getMostRecentEntry() {
      if (entries.isEmpty()) {
         String[] result = { "", "", "" };
         return result;
      }
      return getFields(entries.get(entries.size() - 1));
   }
   
   private String[] getFields(String entry) {
      StringTokenizer st = new StringTokenizer(entry.trim(), ",");
      int tokenCount = st.countTokens();
      String[] result = new String[tokenCount];
      for (int i = 0; i < tokenCount; i++) {
         result[i] = st.nextToken();
      }
      return result;
   }
   
   private void read() {
      try {
         BufferedReader d = new BufferedReader(new FileReader(BACDB_FILE));
         while (true) {
            String line = d.readLine();
            if (line == null) { break; }
            entries.add(line);
         }
      } catch (FileNotFoundException fnfe) {
         /* NOTE: no problem... */
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   private void write() {
      try {
         if (!BACDB_FILE.exists()) {
            if (!PassportGUI.JMRTD_USER_DIR.isDirectory()) {
            	System.out.println("DEBUG: creating " + PassportGUI.JMRTD_USER_DIR);
               PassportGUI.JMRTD_USER_DIR.mkdirs();
            } else {
            	System.out.println("B");
            }
            BACDB_FILE.createNewFile();
         } else {
        	 System.out.println("C");
         }
         PrintWriter d = new PrintWriter(new FileWriter(BACDB_FILE));
         for (String entry: entries) {
            d.println(entry);
         }
         d.flush();
         d.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
