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
 * The DwgSolid class represents a DWG Solid
 * 
 * @author jmorell
 */
public class DwgSolid extends DwgObject {
	private double thickness;
	private double elevation;
	private double[] corner1;
	private double[] corner2;
	private double[] corner3;
	private double[] corner4;
	private double[] extrusion;
	
	/**
	 * Read a Solid in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgSolidV15(int[] data, int offset) throws Exception {
		int bitPos = offset;
		bitPos = readObjectHeaderV15(data, bitPos);
		Vector v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		boolean flag = ((Boolean)v.get(1)).booleanValue();
	    double val;
		if (flag) {
			val=0.0;
		} else {
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			val = ((Double)v.get(1)).doubleValue();
		}
		thickness = val;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		val = ((Double)v.get(1)).doubleValue();
	    elevation = val;
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double x = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double y = ((Double)v.get(1)).doubleValue();
		double[] coord = new double[]{x, y, val};
		corner1 = coord;
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		x = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		y = ((Double)v.get(1)).doubleValue();
		coord = new double[]{x, y, val};
		corner2 = coord;
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		x = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		y = ((Double)v.get(1)).doubleValue();
		coord = new double[]{x, y, val};
		corner3 = coord;
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		x = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		y = ((Double)v.get(1)).doubleValue();
		coord = new double[]{x, y, val};
		corner4 = coord;
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		flag = ((Boolean)v.get(1)).booleanValue();
		double z;
	    if (flag) {
			 x = y = 0.0;
			 z = 1.0;
		} else {
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			x = ((Double)v.get(1)).doubleValue();
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			y = ((Double)v.get(1)).doubleValue();
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			z = ((Double)v.get(1)).doubleValue();
		}
		coord = new double[]{x, y, z};
		extrusion = coord;
		bitPos = readObjectTailV15(data, bitPos);
	}
	/**
	 * @return Returns the corner1.
	 */
	public double[] getCorner1() {
		return corner1;
	}
	/**
	 * @param corner1 The corner1 to set.
	 */
	public void setCorner1(double[] corner1) {
		this.corner1 = corner1;
	}
	/**
	 * @return Returns the corner2.
	 */
	public double[] getCorner2() {
		return corner2;
	}
	/**
	 * @param corner2 The corner2 to set.
	 */
	public void setCorner2(double[] corner2) {
		this.corner2 = corner2;
	}
	/**
	 * @return Returns the corner3.
	 */
	public double[] getCorner3() {
		return corner3;
	}
	/**
	 * @param corner3 The corner3 to set.
	 */
	public void setCorner3(double[] corner3) {
		this.corner3 = corner3;
	}
	/**
	 * @return Returns the corner4.
	 */
	public double[] getCorner4() {
		return corner4;
	}
	/**
	 * @param corner4 The corner4 to set.
	 */
	public void setCorner4(double[] corner4) {
		this.corner4 = corner4;
	}
    /**
     * @return Returns the elevation.
     */
    public double getElevation() {
        return elevation;
    }
    /**
     * @param elevation The elevation to set.
     */
    public void setElevation(double elevation) {
        this.elevation = elevation;
    }
    /**
     * @return Returns the extrusion.
     */
    public double[] getExtrusion() {
        return extrusion;
    }
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		DwgSolid dwgSolid = new DwgSolid();
		dwgSolid.setType(type);
		dwgSolid.setHandle(handle);
		dwgSolid.setVersion(version);
		dwgSolid.setMode(mode);
		dwgSolid.setLayerHandle(layerHandle);
		dwgSolid.setColor(color);
		dwgSolid.setNumReactors(numReactors);
		dwgSolid.setNoLinks(noLinks);
		dwgSolid.setLinetypeFlags(linetypeFlags);
		dwgSolid.setPlotstyleFlags(plotstyleFlags);
		dwgSolid.setSizeInBits(sizeInBits);
		dwgSolid.setExtendedData(extendedData);
		dwgSolid.setGraphicData(graphicData);
		//dwgSolid.setInsideBlock(insideBlock);
		dwgSolid.setThickness(thickness);
		dwgSolid.setElevation(elevation);
		dwgSolid.setCorner1(corner1);
		dwgSolid.setCorner2(corner2);
		dwgSolid.setCorner3(corner3);
		dwgSolid.setCorner4(corner4);
		dwgSolid.setExtrusion(extrusion);
		return dwgSolid;
	}
	/**
	 * @return Returns the thickness.
	 */
	public double getThickness() {
		return thickness;
	}
	/**
	 * @param thickness The thickness to set.
	 */
	public void setThickness(double thickness) {
		this.thickness = thickness;
	}
	/**
	 * @param extrusion The extrusion to set.
	 */
	public void setExtrusion(double[] extrusion) {
		this.extrusion = extrusion;
	}
}
