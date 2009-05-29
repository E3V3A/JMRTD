package org.jmrtd.test;

import java.text.ParseException;

import javax.smartcardio.CardException;

import net.sourceforge.scuba.smartcards.CardServiceException;

class ExampleUsingPassportTestService {
	
	private PassportTestService service;
	
	public void main() throws CardServiceException {
		try {
			PassportTestService service =  PassportTestService.createPassportTestService();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Couldn't create the PassportTestService");
			System.exit(-1);
		}	
		
		try {
			service.setMRZ("XX1234587","197608030101","20140507");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		service.resetCard();
		// try to read photo, should return false
		boolean a = service.canSelectFile((short) 0x0101, false);
		// try to do BAC photo, should return true, assuming MRZ is correct
		boolean b = service.doBAC();
		// try to read photo, should return b
		boolean c = service.canSelectFile((short) 0x0101, true);
	}

}
