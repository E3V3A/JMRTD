package org.jmrtd.app;

import org.jmrtd.BACKeySpec;

public interface MutableBACStore extends BACStore {

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
}
