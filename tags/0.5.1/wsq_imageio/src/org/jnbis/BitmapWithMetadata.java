package org.jnbis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BitmapWithMetadata extends Bitmap {
	private static final long serialVersionUID = -4243273616650162026L;
	
	private Map<String, String> metadata = new LinkedHashMap<String, String>();
	private List<String> comments = new ArrayList<String>();
	
	public BitmapWithMetadata(byte[] pixels, int width, int height, int ppi, int depth, int lossyflag) {
		this(pixels, width, height, ppi, depth, lossyflag, null);		
	}
	
	public BitmapWithMetadata(byte[] pixels, int width, int height, int ppi, int depth, int lossyflag, Map<String,String> metadata, String... comments) {
		super(pixels, width, height, ppi, depth, lossyflag);
		if (metadata != null)
			this.metadata.putAll(metadata);
		if (comments != null)
			for (String s:comments)
				if (s != null)
				this.comments.add(s);
	}
	
	public Map<String, String> getMetadata() {
		return metadata;
	}
	
	public List<String> getComments() {
		return comments;
	}
	
}
