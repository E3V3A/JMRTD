/*
 * $Id: $
 */

package sos.data;

/**
 * Possible values for a person's gender.
 * Integer values correspond to Section 5.5.3 of ISO 19794-5.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * @author Cees-Bart Breunesse (ceesb@riscure.com)
 */
public enum Gender
{
	MALE { public int toInt() { return 0x01; } }, 
	FEMALE {public int toInt() { return 0x02; }}, 
	UNKNOWN { public int toInt() { return 0x03; } }, 
	UNSPECIFIED {public int toInt() { return 0x00; } };

	public abstract int toInt();

	public static Gender toGender(int b) {
		for(Gender g : Gender.values()) {
			if(g.toInt() == b) {
				return g;
			}
		}
		return null;
	}
}


