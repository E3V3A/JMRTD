/* $Id: $ */

package org.jmrtd.imageio;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;

import org.w3c.dom.Node;

public class JJ2000Metadata extends IIOMetadata {
	
	/** Name of the property storing compression rate, in bits-per-inch */
	public static final String KEY_BITRATE = "JJ2000_BITRATE";

	private double rate;
	
	/** Creates a new (empty) WSQ Metadata */
	public JJ2000Metadata() {
		super(
				true, 
				JJ2000MetadataFormat.nativeMetadataFormatName,
				JJ2000MetadataFormat.class.getName(),
				null, null);
		reset();
	}
	
	public void setBitRate(double rate) {
		this.rate = rate;
	}

	public double getBitRate() {
		return rate;
	}
	
	@Override
	public Node getAsTree(String formatName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public void mergeTree(String formatName, Node root) throws IIOInvalidTreeException {
		// TODO Auto-generated method stub
	}

	@Override
	public void reset() {
		rate = 0.0;
	}
}
