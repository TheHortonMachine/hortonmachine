/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects;

import java.util.Vector;

import org.hortonmachine.gears.io.dxfdwg.libs.dwg.DwgObject;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.DwgUtil;

/**
 * The DwgVertex2D class represents a DWG Vertex2D
 * 
 * @author jmorell
 */
public class DwgVertex2D extends DwgObject {
	private int flags;
	private double[] point;
	private double initWidth;
	private double endWidth;
	private double bulge;
	private double tangentDir;
	
	/**
	 * Read a Vertex2D in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgVertex2DV15(int[] data, int offset) throws Exception {
		//System.out.println("readDwgVertex2D executing ...");
		int bitPos = offset;
		bitPos = readObjectHeaderV15(data, bitPos);
		Vector v = DwgUtil.getRawChar(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int flags = ((Integer)v.get(1)).intValue();
		this.flags = flags;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double x = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double y = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double z = ((Double)v.get(1)).doubleValue();
		double[] coord = new double[]{x, y, z};
		point = new double[]{x, y, z};
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double sw = ((Double)v.get(1)).doubleValue();
		double ew = 0.0;
		if (sw<0.0) {
			ew = Math.abs(sw);
			sw = ew;
		} else {
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			ew = ((Double)v.get(1)).doubleValue();
		}
		initWidth = sw;
		endWidth = ew;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double bulge = ((Double)v.get(1)).doubleValue();
		this.bulge = bulge;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double tandir = ((Double)v.get(1)).doubleValue();
		tangentDir = tandir;
		bitPos = readObjectTailV15(data, bitPos);
	}
	/**
	 * @return Returns the bulge.
	 */
	public double getBulge() {
		return bulge;
	}
	/**
	 * @param bulge The bulge to set.
	 */
	public void setBulge(double bulge) {
		this.bulge = bulge;
	}
	/**
	 * @return Returns the flags.
	 */
	public int getFlags() {
		return flags;
	}
	/**
	 * @param flags The flags to set.
	 */
	public void setFlags(int flags) {
		this.flags = flags;
	}
	/**
	 * @return Returns the point.
	 */
	public double[] getPoint() {
		return point;
	}
	/**
	 * @param point The point to set.
	 */
	public void setPoint(double[] point) {
		this.point = point;
	}
}
