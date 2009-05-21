package org.jmrtd.test;

import org.jmrtd.PassportService;

import net.sourceforge.scuba.smartcards.CardServiceException;

/** 
 *  Testing BAC, incl. checks on access control to data groups
 *  at various stages of the protocol 
 *  
 *  @author Erik Poll (erikpoll@cs.ru.nl)
 */
public class PassportBACTester extends PassportTesterBase { 

	/* Some general observations:
     *  
     * The abstraction provided by PassportService and PassportApduService,
     * such as 
     *  - automatically adding/removing SM to APDUs
     *  - translating some (all?) status words other than 9000 into
     *    a CardServiceException
     * can be annoying when testing. Sometimes you want more low level 
     * control over APDUs sent.
     * 
     *  The PassportService won't reset the wrapper to null after an SM failure.
     *  So after SM has been aborted, it will still try to SM-wrap.
     */
	
    public PassportBACTester(String name) {
        super(name); 
    }
    
    
    public void testDataGroup1() {
        traceApdu = true;
          try {
        	resetCard();
			service.doBAC("PPNUMMER0", getDate("19560507"), getDate("20100101"));
		  } catch (CardServiceException cse) {
	            fail(cse.getMessage());
	            return;
	      }
          // We should now be able to read MRZ, photo and public key for AA
          assert (canSelectFile(PassportService.EF_DG1)); 
          assert (canSelectFile(PassportService.EF_DG2));
          assert (canSelectFile(PassportService.EF_DG15)); 
          // but not fingerprint or iris 
          assert (!canSelectFile(PassportService.EF_DG3));
          assert (!canSelectFile(PassportService.EF_DG4)); 
          // Datagroups that are RFU should not be selectable 
          assert (!canSelectFile(PassportService.EF_DG6)); 
          assert (!canSelectFile(PassportService.EF_DG14));       
          // not so sure about other datagroups
          assert (canSelectFile(PassportService.EF_DG5));  //?
          assert (canSelectFile(PassportService.EF_DG7));  //?
          assert (canSelectFile(PassportService.EF_DG8));  //?
          assert (canSelectFile(PassportService.EF_DG9));  //?
          assert (canSelectFile(PassportService.EF_DG10)); //?
          assert (canSelectFile(PassportService.EF_DG11)); //?
          assert (canSelectFile(PassportService.EF_DG12)); //?
          assert (canSelectFile(PassportService.EF_DG13)); //?
          assert (canSelectFile(PassportService.EF_DG15)); //? 
          assert (canSelectFile(PassportService.EF_DG16)); //?
          // Anything without SM should not work, eg
          assert (!canSelectFileWithoutSM(PassportService.EF_DG1));
          // This should result in an SM error and aborting the SM session
          assert last_rapdu.getSW()==0x6987||last_rapdu.getSW()==0x6988;
          // and nothing should no be selectable; note that in the calls below
          // will use SM wrapping with the old keys
          assert (!canSelectFile(PassportService.EF_DG1));
          assert (!canSelectFile(PassportService.EF_DG2));
      
        assertNotNull(service);
    }

}
