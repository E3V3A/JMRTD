/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2013  The JMRTD team
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

import java.util.List;

/**
 * Interface for persistent storage of BAC keys.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 */
public interface BACStore {

	/**
	 * Gets the entries in this BAC store.
	 * 
	 * @return a list of BAC key entries
	 */
    List<BACKeySpec> getEntries();

    /**
     * Adds an entry to this BAC store.
     * 
     * @param entry the BAC key entry to add
     */
    void addEntry(BACKeySpec entry);

    /**
     * Inserts an entry to this BAC store at a specific index.
     * 
     * @param index the index
     * @param entry the BAC key entry to add
     */
    void addEntry(int index, BACKeySpec entry);

    /**
     * Removes an entry from this BAC store.
     * 
     * @param index the index of the BAC key entry to remove
     */
    void removeEntry(int index);

    /**
     * Gets a BAC key entry from this BAC store.
     * 
     * @param index the index of the BAC key entry to get
     * 
     * @return a BAC key entry
     */
    BACKeySpec getEntry(int index);
}
