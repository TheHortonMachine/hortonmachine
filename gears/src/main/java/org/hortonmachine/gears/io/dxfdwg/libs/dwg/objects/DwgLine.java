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
 * The DwgLine class represents a DWG Line
 * 
 * @author jmorell
 */
public class DwgLine extends DwgObject {
	private double[] p1;
	private double[] p2;
	private double thickness;
	private double[] extrusion;
	private boolean zflag = false;
	
	/**
	 * Read a Line in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgLineV15(int[] data, int offset) throws Exception {
		int bitPos = offset;
		bitPos = readObjectHeaderV15(data, bitPos);
		Vector v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		zflag = ((Boolean)v.get(1)).booleanValue();
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double x1 = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getDefaultDouble(data, bitPos, x1);
		bitPos = ((Integer)v.get(0)).intValue();
		double x2 = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double y1 = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getDefaultDouble(data, bitPos, y1);
		bitPos = ((Integer)v.get(0)).intValue();
		double y2 = ((Double)v.get(1)).doubleValue();
		double[] p1;
		double[] p2;
	    if (!zflag) {
			v = DwgUtil.getRawDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			double z1 = ((Double)v.get(1)).doubleValue();
			v = DwgUtil.getDefaultDouble(data, bitPos, z1);
			bitPos = ((Integer)v.get(0)).intValue();
			double z2 = ((Double)v.get(1)).doubleValue();
			p1 = new double[]{x1, y1, z1};
			p2 = new double[]{x2, y2, z2};
		} else {
			p1 = new double[]{x1, y1};
			p2 = new double[]{x2, y2};
		}
	    this.p1 = p1;
	    this.p2 = p2;
		v = DwgUtil.testBit(data, bitPos);
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
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		flag = ((Boolean)v.get(1)).booleanValue();
		double x, y, z;
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
		double[] coord = new double[]{x, y, z};
		extrusion = coord;
		bitPos = readObjectTailV15(data, bitPos);
	}
	/**
	 * @return Returns the p1.
	 */
	public double[] getP1() {
		return p1;
	}
	/**
	 * @param p1 The p1 to set.
	 */
	public void setP1(double[] p1) {
		this.p1 = p1;
	}
	/**
	 * @return Returns the p2.
	 */
	public double[] getP2() {
		return p2;
	}
	/**
	 * @param p2 The p2 to set.
	 */
	public void setP2(double[] p2) {
		this.p2 = p2;
	}
	/**
	 * @return Returns the extrusion.
	 */
	public double[] getExtrusion() {
		return extrusion;
	}
	/**
	 * @param extrusion The extrusion to set.
	 */
	public void setExtrusion(double[] extrusion) {
		this.extrusion = extrusion;
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
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		DwgLine dwgLine = new DwgLine();
		dwgLine.setType(type);
		dwgLine.setHandle(handle);
		dwgLine.setVersion(version);
		dwgLine.setMode(mode);
		dwgLine.setLayerHandle(layerHandle);
		dwgLine.setColor(color);
		dwgLine.setNumReactors(numReactors);
		dwgLine.setNoLinks(noLinks);
		dwgLine.setLinetypeFlags(linetypeFlags);
		dwgLine.setPlotstyleFlags(plotstyleFlags);
		dwgLine.setSizeInBits(sizeInBits);
		dwgLine.setExtendedData(extendedData);
		dwgLine.setGraphicData(graphicData);
		//dwgLine.setInsideBlock(insideBlock);
		dwgLine.setP1(p1);
		dwgLine.setP2(p2);
		dwgLine.setThickness(thickness);
		dwgLine.setExtrusion(extrusion);
		return dwgLine;
	}
    /**
     * @return Returns the zflag.
     */
    public boolean isZflag() {
        return zflag;
    }
    /**
     * @param zflag The zflag to set.
     */
    public void setZflag(boolean zflag) {
        this.zflag = zflag;
    }
}
