/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2010  The JMRTD team
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

import java.util.ArrayList;
import java.util.List;

/**
 * Memory based database for BAC entries.
 *
 * Contributed by Aleksandar Kamburov (wise_guybg).
 *
 * @version $Rev$
 */
public class MemoryBACStore implements BACStore {
    private List<BACKeySpec> entries;

    public MemoryBACStore() {
        this(null);
    }

    public MemoryBACStore(BACStore store) {
        entries = new ArrayList<BACKeySpec>();
        if (store != null) {
            entries.addAll(store.getEntries());
        }
    }

    public List<BACKeySpec> getEntries() {
        return entries;
    }

    public synchronized void addEntry(BACKeySpec entry) {
        if (!entries.contains(entry)) {
            entries.add(entry);
        } else {
            entries.remove(entry);
            entries.add(entry);
        }
    }

    public synchronized void addEntry(int i, BACKeySpec entry) {
        entries.add(i, entry);
    }

    public synchronized void removeEntry(int index) {
        entries.remove(index);
    }

    public BACKeySpec getEntry(int entryRowIndex) {
        return entries.get(entryRowIndex);
    }
}
