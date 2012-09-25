package org.jnbis.imageio;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;

import org.w3c.dom.Node;

/**
 * WSQ Metadata is stored as Key-Value pairs, known as NISTCOM. see <code>nistcom.h</code> inside the <code>j2wsq</code> project.  
 * <p>
 * Almost anything can be stored into the key-value pairs, but most importantly, pixel density is stored as Pixels-Per-Inch under the key "PPI" ({@link #KEY_PPI}), and Bit-Rate under the key "WSQ_BITRATE" ({@link #KEY_BITRATE}).
 * <p>
 * Also, some NISTCOM keys are always discarded:
 * <ul>
 *   <li>NIST_COM: This special tag is meaningless, just indicates a NITSCOM block.
 *   <li>PIX_WIDTH, PIX_HEIGHT: Image dimensions are stored in the image, not in the metadata.
 *   <li>PIX_DEPTH: Always 8 bits
 *   <li>COLORSPACE: Always Grayscale
 *   <li>NUM_COMPONENTS: Always 1 (Grayscale image)
 *   <li>INTERLEAVE: Always false (Grayscale image)
 *   <li>LOSSY: Always true
 *   <li>COMPRESSION: Always WSQ
 *   <li>HV_FACTORS: Not aplicable
 *   <li>JPEGB_QUALITY: Not aplicable
 *   <li>JPEGL_PREDICT: Not aplicable
 * </ul>
 * 
 * @author Paulo Costa <me@paulo.costa.nom.br>
 */
public class WSQMetadata extends IIOMetadata {
	/** Name of the property storing pixel density, in pixel-per-inch */
	public static final String KEY_PPI = "PPI";
	/** Name of the property storing compression rate, in bits-per-inch */
	public static final String KEY_BITRATE = "WSQ_BITRATE";

	/** NistCom key/value pairs */
	Map<String, String> nistcom = new LinkedHashMap<String, String>();
	/** General Comments */
	List<String> comments = new ArrayList<String>();

	/** Creates a new (empty) WSQ Metadata */
	public WSQMetadata() {
		super(
				true, 
				WSQMetadataFormat.nativeMetadataFormatName,
				WSQMetadataFormat.class.getName(),
				null, null);
		reset();
	}

	/** 
	 * Creates a new WSQ Metadata with the specified pixel density.
	 * @param PPI pixel density, in Pixels-Per-Inch
	 */
	public WSQMetadata(double PPI) {
		this();
		setProperty(KEY_PPI, Double.toString(PPI));
	}

	/** 
	 * Creates a new WSQ Metadata with the specified pixel-density and bit-rate.
	 * @param PPI pixel density, in Pixels-Per-Inch
	 * @param BitRate Estimated number of bits per pixel in the compressed image.
	 */
	public WSQMetadata(double PPI, double BitRate) {
		this();
		setProperty(KEY_PPI    , Double.toString(PPI));
		setProperty(KEY_BITRATE, Double.toString(BitRate));
	}

	/** 
	 * Retrieves the Pixel density (In Pixels per Inch), 
	 * or {@link Double#isNaN()} if this information is not available.
	 * 
	 * @see Double#isNaN()
	 */
	public double getPPI() {
		if (!nistcom.containsKey(KEY_PPI))
			return Double.NaN;
		else
			return Double.parseDouble(nistcom.get(KEY_PPI));
	}

	/** 
	 * Retrieves the Bit rate (In Bits per Pixel), 
	 * or {@link Double#isNaN()} if this information is not available.
	 * 
	 * @see Double#isNaN()
	 */
	public double getBitrate() {
		if (!nistcom.containsKey(KEY_BITRATE))
			return Double.NaN;
		else
			return Double.parseDouble(nistcom.get(KEY_BITRATE));
	}

	/** WSQ Metadata is not Read-Only */
	@Override
	public boolean isReadOnly() {
		return false;
	}

	/** Removes all metadata information */
	@Override
	public void reset() {
		nistcom.clear();
	}

	/** NISTCOM Keys that will be automatically discarded */
	private static final Set<String> NISTCOM_DISCARD=new HashSet<String>();
	/** NISTCOM Keys that must be formatted as valid non-negative numbers to be accepted */
	private static final Set<String> NISTCOM_NUMBER=new HashSet<String>();

	static {
		NISTCOM_DISCARD.add("NIST_COM");
		NISTCOM_DISCARD.add("PIX_WIDTH");
		NISTCOM_DISCARD.add("PIX_HEIGHT");
		NISTCOM_DISCARD.add("PIX_DEPTH");
		NISTCOM_DISCARD.add("COLORSPACE");
		NISTCOM_DISCARD.add("NUM_COMPONENTS");
		NISTCOM_DISCARD.add("NUM_COMPONENTS");
		NISTCOM_DISCARD.add("HV_FACTORS");
		NISTCOM_DISCARD.add("INTERLEAVE");
		NISTCOM_DISCARD.add("COMPRESSION");
		NISTCOM_DISCARD.add("JPEGB_QUALITY");
		NISTCOM_DISCARD.add("JPEGL_PREDICT");
		NISTCOM_DISCARD.add("LOSSY");

		NISTCOM_NUMBER.add("PPI");
		NISTCOM_NUMBER.add("WSQ_BITRATE");
	}

	/** Returns the NISTCOM metadata, formatted as a key-value pair per line */
	public String getNistcom() {
		StringBuffer ret=new StringBuffer();
		ret.append("NIST_COM ").append(nistcom.size()).append("\n");
		for (Map.Entry<String, String> entry : nistcom.entrySet()) {
			ret.append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
		}
		return ret.toString();
	}

	/** Returns the value associated to the specified NISTCOM key, or <code>null</code> if there is no value associated to it. */
	public String getProperty(String key) {
		return nistcom.get(key);
	}

	/**
	 * Sets the specified property to the specified value.
	 * <p>
	 * Using value==null removes the property from this metadata
	 * <p>
	 * Some properties may be ignored (e.g., NIST_COM, COLORSPACE, etc), and other may be subject to checks (e.g., PPI and WSQ_BITRATE must be valid positive numbers).
	 * 
	 * @param key Name of the property being set.
	 * @param value new value of the property, or <code>null</code> if the property will be removed.
	 * @return <code>true</code> if the modification was successfull, <code>false</code> otherwise.
	 */
	public boolean setProperty(String key, String value) {
		//Non-writable property
		if (NISTCOM_DISCARD.contains(key)) { 
			return false; 
		}

		//Numeric Properties. Values <0 will erase the key
		if (NISTCOM_NUMBER.contains(key)) {
			try {
				double v = Double.parseDouble(value);
				if (!(v>0)) 
					value=null;
				else
					value=Double.toString(v);
			} catch (Throwable t) {
				return false; //Invalid number
			}
		}

		//General Property => Just put into the hash
		if (value==null) {
			nistcom.remove(key);
		} else {
			nistcom.put(key, value);
		}
		return true;
	}

	/** Returns this Metadata as a tree, either in standard or native format */
	@Override
	public Node getAsTree(String formatName) {
		if (formatName.equals(IIOMetadataFormatImpl.standardMetadataFormatName)) 
			return getStandardTree();
		else if (formatName.equals(nativeMetadataFormatName)) 
			return getNativeTree();
		else
			throw new IllegalArgumentException("Not a recognized format!");
	}

	/** 
	 * Returns this Metadata as a native metadata tree.
	 * <p>
	 * The native format stores the <key,value> pairs in the {@link #nistcom}.
	 * 
	 * @see WSQMetadataFormat
	 */
	protected IIOMetadataNode getNativeTree() {
		IIOMetadataNode root = new IIOMetadataNode(nativeMetadataFormatName);
		for (Map.Entry<String, String> entry : nistcom.entrySet()) {
			IIOMetadataNode node = new IIOMetadataNode("property");
			node.setAttribute("name", entry.getKey());
			node.setAttribute("value", entry.getValue());
			root.appendChild(node);
		}
		return root;
	}

	/**
	 * Returns the Standard Dimension Node containing pixel density information taken from the "PPI" NISTCOM property.
	 * <p>
	 * Aspect ratio is always set to "1".
	 * <p>
	 * If PPI is known, <code>HorizontalPixelSize</code>, <code>VerticalPixelSize</code>, <code>HorizontalPhysicalPixelSpacing</code> and <code>VerticalPhysicalPixelSpacing</code> are set to <code>25.4/PPI</code>.
	 */
	@Override
	protected IIOMetadataNode getStandardDimensionNode() {
		IIOMetadataNode dimension = new IIOMetadataNode("Dimension");

		IIOMetadataNode aspect_node = new IIOMetadataNode("PixelAspectRatio");
		aspect_node.setAttribute("value", Double.toString(1));
		dimension.appendChild(aspect_node);

		if (nistcom.containsKey(KEY_PPI)) {
			String pixSizeStr = Double.toString(25.4/getPPI());			
			IIOMetadataNode pixelSizeX = new IIOMetadataNode("HorizontalPixelSize");
			pixelSizeX.setAttribute("value", pixSizeStr);
			dimension.appendChild(pixelSizeX);

			IIOMetadataNode pixelSizeY = new IIOMetadataNode("VerticalPixelSize");
			pixelSizeY.setAttribute("value", pixSizeStr);
			dimension.appendChild(pixelSizeY);

			IIOMetadataNode pixelPhysSizeX = new IIOMetadataNode("HorizontalPhysicalPixelSpacing");
			pixelPhysSizeX.setAttribute("value", pixSizeStr);
			dimension.appendChild(pixelPhysSizeX);

			IIOMetadataNode pixelPhysSizeY = new IIOMetadataNode("VerticalPhysicalPixelSpacing");
			pixelPhysSizeY.setAttribute("value", pixSizeStr);
			dimension.appendChild(pixelPhysSizeY);
		}

		return dimension;
	}

	/** 
	 * Returns the standard Chroma node. 
	 * <p>
	 * It contains <code>ColorSpaceType=GRAY</code>. 
	 */
	@Override
	protected IIOMetadataNode getStandardChromaNode() {
		IIOMetadataNode chroma = new IIOMetadataNode("Chroma");

		IIOMetadataNode colorspace = new IIOMetadataNode("ColorSpaceType");
		colorspace.setAttribute("name", "GRAY");
		chroma.appendChild(colorspace);

		return chroma;
	}

	/** 
	 * Returns the standard Compression node containing bitrate information taken from the "WSQ_BITRATE" NISTCOM property. 
	 * <p>
	 * It contains <code>CompressionTypeName=WSQ</code>, <code>Lossless=FALSE</code> and, if available, <code>BitRate=WSQ_BITRATE<code>
	 */
	@Override
	protected IIOMetadataNode getStandardCompressionNode() {
		IIOMetadataNode compression = new IIOMetadataNode("Compression");

		IIOMetadataNode compressionName = new IIOMetadataNode("CompressionTypeName");
		compressionName.setAttribute("value", "WSQ");
		compression.appendChild(compressionName);

		IIOMetadataNode lossless = new IIOMetadataNode("Lossless");
		lossless.setAttribute("value", "FALSE");
		compression.appendChild(lossless);

		if (nistcom.containsKey(KEY_BITRATE)) {
			IIOMetadataNode bitrate = new IIOMetadataNode("BitRate");
			bitrate.setAttribute("value", nistcom.get(KEY_BITRATE));
			compression.appendChild(bitrate);
		}
		return compression;
	}

	/**
	 * Same as {@link #reset()} followed by {@link #mergeTree(String, Node)}. 
	 */
	@Override
	public void setFromTree(String formatName, Node root) throws IIOInvalidTreeException {
		Map<String, String> backup = new LinkedHashMap<String, String>(nistcom);
		boolean success=false;
		try {
			reset();
			mergeTree(formatName, root);
			success=true;
		} finally {
			if (!success) 
				nistcom = backup;
		}
	}

	/**
	 * Clear this metadata state and puts the information from the specified Metadata Tree. 
	 */
	@Override
	public void mergeTree(String formatName, Node root) throws IIOInvalidTreeException {
		Map<String, String> backup = new LinkedHashMap<String, String>(nistcom);
		boolean success=false;
		try {
			if (root == null) 
				throw new IllegalArgumentException("root == null!");

			if (formatName.equals(IIOMetadataFormatImpl.standardMetadataFormatName)) { 
				mergeStandardTree(root);
			} else if (formatName.equals(nativeMetadataFormatName)) { 
				mergeNativeTree(root);
			} else {
				throw new IllegalArgumentException("Not a recognized format!");
			}
			success=true;
		} finally {
			if (!success) 
				nistcom = backup;
		}
	}

	/**
	 * implementation of {@link #mergeTree(String, Node)} for the Native Metadata format.
	 */
	protected void mergeNativeTree(Node root) throws IIOInvalidTreeException {
		if (!nativeMetadataFormatName.equals(root.getNodeName())) 
			throw new IIOInvalidTreeException("Root node should be a " + nativeMetadataFormatName, root);

		for (Node property=root.getFirstChild(); property!=null; property=property.getNextSibling()) {
			if (!"property".equals(property.getNodeName()))
				throw new IIOInvalidTreeException("Expected 'property' element", property);

			String propertyName=null;
			String propertyValue=null;
			for (int i=0; i<property.getAttributes().getLength(); i++) {
				Node attribute = property.getAttributes().item(i);
				if ("name".equals(attribute.getNodeName())) { 
					propertyName=attribute.getNodeValue();
				} else if ("value".equals(attribute.getNodeName())) {
					propertyValue=attribute.getNodeValue();
				} else {
					throw new IIOInvalidTreeException("Invalid attribute '" + attribute.getNodeName() + "', should be 'name' or 'value'", attribute);
				}
			}

			if (propertyName==null)
				throw new IIOInvalidTreeException("Property does not have the 'name' attribute", property);

			try {
				setProperty(propertyName, propertyValue);
			} catch (Throwable t) {
				throw new IIOInvalidTreeException(t.toString(), property);
			}
		}
	}

	/**
	 * implementation of {@link #mergeTree(String, Node)} for the Standard Metadata format.
	 */
	protected void mergeStandardTree(Node root) throws IIOInvalidTreeException {
		if (!IIOMetadataFormatImpl.standardMetadataFormatName.equals(root.getNodeName())) 
			throw new IIOInvalidTreeException("Expected '" + IIOMetadataFormatImpl.standardMetadataFormatName + "' element", root);

		for (Node element=root.getFirstChild(); element!=null; element=element.getNextSibling()) {
			if ("Compression".equals(element.getNodeName())) 
				mergeStandardCompressionNode(element);
			if ("Dimension".equals(element.getNodeName())) 
				mergeStandardDimensionNode(element);
		}
	}

	/**
	 * Retrieves the Compression Bit-Rate from a standard compression metadata tree and stores it in the NISTCOM as {@link #KEY_BITRATE}. 
	 */
	protected void mergeStandardCompressionNode(Node compression) throws IIOInvalidTreeException {
		if (!"Compression".equals(compression.getNodeName())) 
			throw new IIOInvalidTreeException("Expected 'Compression' element", compression);

		for (Node bitrate=compression.getFirstChild(); bitrate!=null; bitrate=bitrate.getNextSibling()) {
			if (!"BitRate".equals(bitrate.getNodeName())) continue;

			for (int i=0; i<bitrate.getAttributes().getLength(); i++) {
				Node attribute = bitrate.getAttributes().item(i);
				if ("value".equals(attribute.getNodeName())) { 
					String bitrate_value=attribute.getNodeValue();
					setProperty(KEY_BITRATE, bitrate_value);
				}
			}
		}
	}

	/**
	 * Retrieves the Pixel-Density from a standard dimension metadata tree and stores it in the NISTCOM as {@link #KEY_PPI}. 
	 */
	protected void mergeStandardDimensionNode(Node dimensionNode) throws IIOInvalidTreeException {
		double pixelSizeX=Double.NaN;
		double pixelSizeY=Double.NaN;
		double pixelPhysicalSizeX=Double.NaN;
		double pixelPhysicalSizeY=Double.NaN;
		double aspectRatio=1;

		if (!"Dimension".equals(dimensionNode.getNodeName())) 
			throw new IIOInvalidTreeException("Expected 'Dimension' element", dimensionNode);

		for (Node pixelSizeNode=dimensionNode.getFirstChild(); pixelSizeNode!=null; pixelSizeNode=pixelSizeNode.getNextSibling()) {
			//Skip the node if we cannot use it.
			if (!"PixelAspectRatio".equals(pixelSizeNode.getNodeName()) &&
					!"HorizontalPhysicalPixelSpacing".equals(pixelSizeNode.getNodeName()) &&
					!"VerticalPhysicalPixelSpacing".equals(pixelSizeNode.getNodeName()) &&
					!"HorizontalPixelSize".equals(pixelSizeNode.getNodeName()) &&
					!"VerticalPixelSize".equals(pixelSizeNode.getNodeName()) )
			{
				continue;
			}

			double value=Double.NaN;
			for (int i=0; i<pixelSizeNode.getAttributes().getLength(); i++) {
				Node valueNode = pixelSizeNode.getAttributes().item(i);
				if (!"value".equals(valueNode.getNodeName())) continue;
				try {
					value = Double.parseDouble(valueNode.getNodeValue());
				} catch (Throwable t) {} //Ignores NumberFormatException, will be handled bellow.								
			}
			if (!(value > 0))
				throw new IIOInvalidTreeException("Empty or Invalid " + pixelSizeNode.getNodeName(), pixelSizeNode);


			if ("PixelAspectRatio".equals(pixelSizeNode.getNodeName()))
				aspectRatio = value;

			if ("HorizontalPhysicalPixelSpacing".equals(pixelSizeNode.getNodeName())) 
				pixelPhysicalSizeX = value;

			if ("VerticalPhysicalPixelSpacing".equals(pixelSizeNode.getNodeName())) 
				pixelPhysicalSizeY = value;

			if ("HorizontalPixelSize".equals(pixelSizeNode.getNodeName())) 
				pixelSizeX = value;

			if ("VerticalPixelSize".equals(pixelSizeNode.getNodeName())) 
				pixelSizeY = value;
		}

		if (!Double.isNaN(pixelPhysicalSizeX) || !Double.isNaN(pixelPhysicalSizeY)) {
			if (Double.isNaN(pixelPhysicalSizeX))
				pixelPhysicalSizeX = pixelPhysicalSizeY*aspectRatio;
			if (Double.isNaN(pixelPhysicalSizeY))
				pixelPhysicalSizeY = pixelPhysicalSizeX/aspectRatio;
			double pixelSize = Math.sqrt(pixelPhysicalSizeX*pixelPhysicalSizeY);
			setProperty(KEY_PPI, Double.toString(25.4 / pixelSize));

		} else if (!Double.isNaN(pixelSizeX) || !Double.isNaN(pixelSizeY)) {
			if (Double.isNaN(pixelSizeX))
				pixelSizeX = pixelSizeY*aspectRatio;
			if (Double.isNaN(pixelSizeY))
				pixelSizeY = pixelSizeX/aspectRatio;
			double pixelSize = Math.sqrt(pixelSizeX*pixelSizeY);
			setProperty(KEY_PPI, Double.toString(25.4 / pixelSize));
		} 
	}

	/**
	 * Returns a string with all information in this metadata formatted as "{key1=value1, key2=value2,...}"
	 * @see AbstractMap#toString() 
	 */
	@Override
	public String toString() {
		return "WSQ metadata: " + nistcom;
	}

	public void addComment(String s) {
		if (s != null)
			comments.add(s);
	}
	
	public List<String> getComments() {
		return comments;
	}
}
