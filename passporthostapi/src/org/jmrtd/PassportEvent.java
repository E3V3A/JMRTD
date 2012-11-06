/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2012  The JMRTD team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * $Id$
 */

package org.jmrtd;

import java.util.EventObject;

/**
 * Event for passport insertion and removal.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision$
 */
public class PassportEvent extends EventObject {

	private static final long serialVersionUID = 3400137567708197350L;

	/** Event type constant. */
	public static final int REMOVED = 0, INSERTED = 1;

	private int type;
	private PassportService service;

	/**
	 * Creates an event.
	 *
	 * @param type event type
	 * @param service event source
	 */
	public PassportEvent(int type, PassportService service) {
		super(service);
		this.type = type;
		this.service = service;
	}

	/**
	 * Gets the event type.
	 *
	 * @return event type
	 */
	public int getType() {
		return type;
	}

	/**
	 * Gets the event source.
	 *
	 * @return a passport service
	 */
	public PassportService getService() {
		return service;
	}

	/**
	 * Gets a textual description of this event.
	 * 
	 * @return a textual description of this event
	 */
	public String toString() {
		switch (type) {
		case REMOVED: return "Passport removed from " + service;
		case INSERTED: return "Passport inserted in " + service;
		}
		return "CardEvent " + service;
	}

	/**
	 * Whether this event is equal to the event in <code>other</code>.
	 * 
	 * @return a boolean
	 */
	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!other.getClass().equals(this.getClass())) { return false; }
		PassportEvent otherCardEvent = (PassportEvent)other;
		return type == otherCardEvent.type && service.equals(otherCardEvent.service);
	}
	
	/**
	 * Gets a hash code for this event.
	 * 
	 * @return a hash code for this event
	 */
	public int hashCode() {
		return 3 * service.hashCode() + 5 * type;
	}
}
