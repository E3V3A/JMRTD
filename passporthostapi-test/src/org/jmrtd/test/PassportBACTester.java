package org.jmrtd.test;

import org.jmrtd.PassportService;

import net.sourceforge.scuba.smartcards.CardFileInputStream;
import net.sourceforge.scuba.smartcards.CardServiceException;

/** 
 *  Testing BAC, incl checks on access control to Data Groups
 *  at various stages of the protocol 
 *  
 *  @author Erik Poll (erikpoll@cs.ru.nl)
 */
public class PassportBACTester extends PassportTesterBase {

	/* Some general observations:
     *  
     * The abstraction provided by PassportService and PassportApduService,
     * such as 
     *  - automatically adding SM to command APDU
     *  - translating some (all?) status words into a CardServiceException
     *  - possibly removing SM from response APDU
     * can be annoying when testing. Sometimes you want more low level 
     * control over APDUs sent and inspect the actual status word returned.
     */
	
    public PassportBACTester(String name) {
        super(name);
    }
    
    /**    
     *  Return true if datagroup fid can be selected.
     *  SM is used if it is active.
     *  
     *  Not sure if this is correct: when _exactly_ does service.readFile
     *  throw a CardServiceException?
     */
    private boolean canSelectDatagroup(short fid){
    	try { CardFileInputStream in = service.readFile(PassportService.EF_DG1);
              return (in != null);
          } catch(CardServiceException e){
              return false;
          }
    }
    
    /**    
     *  Return true if datagroup fid can be selected.
     *  SM is not used.
     *  
     *  Not sure if this is correct: when _exactly_ does service.sendSelectFile
     *  throw a CardServiceException?
     */
    private boolean canSelectDatagroupWithoutSM(short fid){
    	try { service.sendSelectFile(null,fid); // null means don't use SM
    		  return true;
    	} catch (CardServiceException e){
    		  return false;
    		  // maybe some exceptions should pass passed on ?
    	}
    }

    public void testDataGroup1() {
        traceApdu = true;
        try {
          resetCard();
          service.doBAC("PPNUMMER0", getDate("19560507"), getDate("20100101"));
          //we can read MRZ, photo and public key for AA
          assert (canSelectDatagroup(PassportService.EF_DG1)); 
          assert (canSelectDatagroup(PassportService.EF_DG2));
          assert (!canSelectDatagroup(PassportService.EF_DG15)); 
          // but not fingerprint or iris 
          assert (!canSelectDatagroup(PassportService.EF_DG3));
          assert (!canSelectDatagroup(PassportService.EF_DG4)); 
          // Datagroups that are RFU should not be selectable 
          assert (!canSelectDatagroup(PassportService.EF_DG6)); 
          assert (!canSelectDatagroup(PassportService.EF_DG14));       
          // not so sure about others
          assert (canSelectDatagroup(PassportService.EF_DG5));//?
          assert (canSelectDatagroup(PassportService.EF_DG7));//?
          assert (canSelectDatagroup(PassportService.EF_DG8));//?
          assert (canSelectDatagroup(PassportService.EF_DG9));//?
          assert (canSelectDatagroup(PassportService.EF_DG10));//?
          assert (canSelectDatagroup(PassportService.EF_DG11));//?
          assert (canSelectDatagroup(PassportService.EF_DG12));//?
          assert (canSelectDatagroup(PassportService.EF_DG13));//?
          assert (canSelectDatagroup(PassportService.EF_DG15));//? 
          assert (canSelectDatagroup(PassportService.EF_DG16));//?
          // anything without SM should not work, eg
          assert (!canSelectDatagroupWithoutSM(PassportService.EF_DG1));
          // this should result in an SM error, and aborting the SM session
          assert (!canSelectDatagroup(PassportService.EF_DG1));
          assert (!canSelectDatagroup(PassportService.EF_DG2));
        }catch(CardServiceException cse) {
            fail(cse.getMessage());
        }
        assertNotNull(service);
    }

}
