package org.jmrtd.imageio;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;

/**
 * Specified the native metadata for J2K Images.
 * It is a simple list of key-value pairs that can be mapped directly to JJ2000 encoding params.
 * <p>
 * The tree will look like this:
 * <pre>{@code
<org.jmrtd.imageio.JJ2000Metadata_1.0>
  <property name="JJ2000_BITRATE" value="1.5" />
</org.jmrtd.imageio.JJ2000Metadata_1.0>
}</pre>
 * 
 * @author The JMRTD team (info@jmrtd.org)
 */
public class JJ2000MetadataFormat extends IIOMetadataFormatImpl {
    public static final IIOMetadataFormat instance = new JJ2000MetadataFormat();
    
    public static final String nativeMetadataFormatName = "org.jmrtd.imageio.JJ2000Metadata_1.0";
    
    public JJ2000MetadataFormat() {
        super(
        		nativeMetadataFormatName, 
        		CHILD_POLICY_SEQUENCE);        

        addElement(
        		"property",
        		nativeMetadataFormatName,
        		CHILD_POLICY_EMPTY);
        addAttribute("property", "name",
        		DATATYPE_STRING, 
        		true, //Required 
        		null);
        addAttribute("property", "value",
        		DATATYPE_STRING, 
        		false, //Value==null => Remove the key 
        		null);
        
        addElement(
        		"comment",
        		nativeMetadataFormatName,
        		CHILD_POLICY_EMPTY);
        addAttribute("comment", "value",
        		DATATYPE_STRING, 
        		false, //Value==null => Remove the key 
        		null);
    }
    
	@Override
	public boolean canNodeAppear(String elementName, ImageTypeSpecifier imageType) {
		return true;
	}

	public static IIOMetadataFormat getInstance() {
		return instance;
	}
}
