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
 * $Id: $
 */

package org.jmrtd.lds;

/**
 * Interface for MRZ field transformers, for instance for abbreviating long names and
 * transliteration of non-ISO characters.
 * 
 * TODO: work in progress
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 * 
 * @since 0.4.7
 */
public interface MRZFieldTransformer {
	
	/**
	 * Truncates the primary and secondary identifiers. The resulting array has length
	 * 2 and contains the truncated primary and secondary identifiers.
	 * 
	 * Each identifier consists of components separated by <code>&quot;&lt;&quot;</code>.
	 * The maximum length of the resulting
	 * <code>primaryIdentifier + &quot;&lt;&lt;&quot; secondaryIdentifier</code>
	 * is <code>length</code>.
	 * 
	 * @param primaryIdentifier the original primary identifier
	 * @param secondaryIdentifier the original secondary identifier
	 * @param length the maximum length of the resulting identifier (including the <code>&quot;&lt;&lt;&quot;</code>
	 *
	 * @return an array of length 2 containing the resulting identifier
	 */
	String[] truncateNames(String primaryIdentifier, String secondaryIdentifier, int length);

	/**
	 * Transliterates <code>text</code>, replacing non-ISO characters with ISO characters.
	 * 
	 * @param text some text containing non-ISO characters
	 *
	 * @return the same text containing only ISO characters
	 */
	String transliterate(String text);
}
