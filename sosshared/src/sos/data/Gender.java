package sos.data;

/** Possible values for gender. */
public enum Gender {

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


