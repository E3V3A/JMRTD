package org.jmrtd.cert;

public class CVCAuthorizationTemplate
{
	public enum Role {
		CVCA  (0xC0),
		DV_D  (0x80),
		DV_F  (0x40),
		IS    (0x00);

		private byte value;

		private Role(int value) {
			this.value = (byte)value;
		}

		/**
		 * Returns the value as a bitmap
		 * @return
		 */
		public byte getValue(){
			return value;
		}
	}

	public enum Permission
	{
		READ_ACCESS_NONE        (0x00),
		READ_ACCESS_DG3         (0x01),
		READ_ACCESS_DG4         (0x02),
		READ_ACCESS_DG3_AND_DG4 (0x03);

		private byte value;

		private Permission(int value){
			this.value = (byte)value;
		}

		public boolean implies(Permission other) {
			switch (this) {
			case READ_ACCESS_NONE: return other == READ_ACCESS_NONE;
			case READ_ACCESS_DG3: return other == READ_ACCESS_DG3;
			case READ_ACCESS_DG4: return other == READ_ACCESS_DG4;
			case READ_ACCESS_DG3_AND_DG4: return other == READ_ACCESS_DG3 || other == READ_ACCESS_DG4 || other == READ_ACCESS_DG3_AND_DG4;
			}
			return false;
		}

		/**
		 * Returns the tag as a bitmap
		 * @return
		 */
		public byte getValue(){
			return value;
		}
	}

	private Role role;
	private Permission accessRight;

	protected CVCAuthorizationTemplate(org.ejbca.cvc.CVCAuthorizationTemplate template) {
		try {
			switch(template.getAuthorizationField().getRole()) {
			case CVCA: this.role = Role.CVCA; break;
			case DV_D: this.role = Role.DV_D; break;
			case DV_F: this.role = Role.DV_F; break;
			case IS: this.role = Role.IS; break;
			}
			switch(template.getAuthorizationField().getAccessRight()) {
			case READ_ACCESS_NONE: this.accessRight = Permission.READ_ACCESS_NONE; break;
			case READ_ACCESS_DG3: this.accessRight = Permission.READ_ACCESS_DG3; break;
			case READ_ACCESS_DG4: this.accessRight = Permission.READ_ACCESS_DG4; break;
			case READ_ACCESS_DG3_AND_DG4: this.accessRight = Permission.READ_ACCESS_DG3_AND_DG4; break;
			}
		} catch (NoSuchFieldException nsfe) {
			throw new IllegalArgumentException("Error getting role from AuthZ template");
		}
	}
	
	public CVCAuthorizationTemplate(Role role, Permission accessRight) {
		this.role = role;
		this.accessRight = accessRight;
	}

	public Role getRole() {
		return role;
	}

	public Permission getAccessRight() {
		return accessRight;
	}
	
	public String toString() {
		return role.toString() + accessRight.toString();
	}
	
	public boolean equals(Object otherObj) {
		if (otherObj == null) { return false; }
		if (otherObj == this) { return true; }
		if (!this.getClass().equals(otherObj.getClass())) { return false; }
		CVCAuthorizationTemplate otherTemplate = (CVCAuthorizationTemplate)otherObj;
		return this.role == otherTemplate.role && this.accessRight == otherTemplate.accessRight;
	}
}
