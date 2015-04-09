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
 * $Id: $
 */

package org.jmrtd.jj2000;

import java.io.Serializable;

public class Bitmap implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private int width;
    private int height;
    private int ppi;
    private int depth;
    private double bitRate;
    private boolean isLossy;

    private int[] pixels;

    public Bitmap(int[] pixels, int width, int height, int depth, int ppi, boolean isLossy, double bitRate) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
        this.ppi = ppi;
        this.depth = depth;
        this.bitRate = bitRate;
        this.isLossy = isLossy;
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

    public int[] getPixels() {
        return pixels;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isLossy() {
        return isLossy;
    }
    
    public double getBitRate() {
    	return bitRate;
    }
    
    @Override
	public String toString() {
    	StringBuffer result = new StringBuffer();
    	result.append("Bitmap [");
    	result.append(width);
    	result.append(" x "); result.append(height);
    	result.append(" x "); result.append(depth); result.append(", ");
    	result.append("ppi = "); result.append(ppi); result.append(", ");
    	result.append("bitRate = "); result.append(bitRate); result.append(", ");
    	result.append("lossy = "); result.append(isLossy);
    	result.append("]");
    	return result.toString();
    }
}
