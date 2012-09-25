/*
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
 * --
 * This code is based on JNBIS 1.0.3 which was licensed under Apache License 2.0.
 * 
 * $Id: $
 */

package org.jnbis;

import java.io.Serializable;

/**
 * Original comment in JNBIS:
 * 
 * @author <a href="mailto:m.h.shams@gmail.com">M. H. Shamsi</a>
 * @version 1.0.0
 * @date Oct 6, 2007
 */
public class Bitmap implements Serializable {

	private static final long serialVersionUID = -8632563339133022850L;
	
	private int width;
    private int height;
    private int ppi;
    private int depth;
    private int lossyflag;

    private byte[] pixels;
    private int length;

    public Bitmap(byte[] pixels, int width, int height, int ppi, int depth, int lossyflag) {
        this.pixels = pixels;
        this.length = pixels != null ? pixels.length : 0;

        this.width = width;
        this.height = height;
        this.ppi = ppi;
        this.depth = depth;
        this.lossyflag = lossyflag;
    }


    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getPpi() {
        return ppi;
    }

    public byte[] getPixels() {
        return pixels;
    }

    public int getLength() {
        return length;
    }

    public int getDepth() {
        return depth;
    }

    public int getLossyflag() {
        return lossyflag;
    }
    
    public String toString() {
    	StringBuffer result = new StringBuffer();
    	result.append("Bitmap [");
    	result.append(width);
    	result.append(" x "); result.append(height);
    	result.append(" x "); result.append(depth); result.append(", ");
    	result.append("ppi = "); result.append(ppi); result.append(", ");
    	result.append("lossy = "); result.append(lossyflag);
    	result.append("]");
    	return result.toString();
    }
}
