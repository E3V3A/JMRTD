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

public class BACStore {
     
   private static final File BACDB_FILE =
      new File(PassportGUI.JMRTD_USER_DIR, "bacdb.txt");

   private List<String> entries;

   public BACStore() {
      entries = new ArrayList<String>();
      read();
   }
   
   public void addEntry(String documentNumber, String dateOfBirth, String dateOfExpiry) {
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
         result[i] = st.nextToken().trim();
      }
      return result;
   }
   
   private void read() {
      try {
         BufferedReader d = new BufferedReader(new FileReader(BACDB_FILE));
         while (true) {
            String line = d.readLine();
            if (line == null) { break; }
            entries.add(line.trim());
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
            BACDB_FILE.createNewFile();
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
