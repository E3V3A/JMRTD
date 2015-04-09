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

package org.jmrtd.app;

import java.util.ArrayList;
import java.util.List;

import org.jmrtd.BACKeySpec;

/**
 * Memory based database for BAC entries.
 *
 * Contributed by Aleksandar Kamburov (wise_guybg).
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision$
 */
public class MemoryBACStore implements MutableBACStore {

    private List<BACKeySpec> entries;

    /**
     * Constructs a BAC store.
     */
    public MemoryBACStore() {
        this(null);
    }

    /**
     * Constructs a BAC store given another BAC store.
     * 
     * @param store the other BAC store
     */
    public MemoryBACStore(BACStore store) {
        entries = new ArrayList<BACKeySpec>();
        if (store != null) {
            entries.addAll(store.getEntries());
        }
    }

    /**
     * Gets the entries in this BAC store.
     * 
     * @return the list of BAC key entries
     */
    public List<BACKeySpec> getEntries() {
        return entries;
    }

    /**
     * Adds a BAC key entry to this BAC store.
     * 
     * @param entry the BAC key entry to add
     */
    public synchronized void addEntry(BACKeySpec entry) {
        if (!entries.contains(entry)) {
            entries.add(entry);
        } else {
            entries.remove(entry);
            entries.add(entry);
        }
    }

    /**
     * Adds a BAC key entry to this BAC store at a specific index.
     * 
     * @param index the index
     * @param entry the BAC key entry to add
     */
    public synchronized void addEntry(int index, BACKeySpec entry) {
        entries.add(index, entry);
    }

    /**
     * Removes a BAC key entry from this BAC store.
     * 
     * @param index the index of the BAC key entry to remove
     */
    public synchronized void removeEntry(int index) {
        entries.remove(index);
    }

    /**
     * Gets a BAC key entry from this BAC store.
     * 
     * @param index the index of the BAC key entry to get
     * 
     * @return a BAC key entry
     */
    public BACKeySpec getEntry(int index) {
        return entries.get(index);
    }
}
