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
 * The DwgArc class represents a DWG Arc
 * 
 * @author jmorell
 */
public class DwgArc extends DwgObject {
	private double[] center;
	private double radius;
	private double thickness;
	private double[] extrusion;
	private double initAngle;
	private double endAngle;
	
	/**
	 * Read an Arc in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgArcV15(int[] data, int offset) throws Exception {
		//System.out.println("readDwgArc() executed ...");
		int bitPos = offset;
		bitPos = readObjectHeaderV15(data, bitPos);
		Vector v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double x = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double y = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double z = ((Double)v.get(1)).doubleValue();
		double[] coord = new double[]{x, y, z};
		center = coord;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double val = ((Double)v.get(1)).doubleValue();
		radius = val;
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		boolean flag = ((Boolean)v.get(1)).booleanValue();
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
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		val = ((Double)v.get(1)).doubleValue();
	    initAngle = val;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		val = ((Double)v.get(1)).doubleValue();
	    endAngle = val;
		bitPos = readObjectTailV15(data, bitPos);
	}
	/**
	 * @return Returns the center.
	 */
	public double[] getCenter() {
		return center;
	}
	/**
	 * @param center The center to set.
	 */
	public void setCenter(double[] center) {
		this.center = center;
	}
	/**
	 * @return Returns the endAngle.
	 */
	public double getEndAngle() {
		return endAngle;
	}
	/**
	 * @param endAngle The endAngle to set.
	 */
	public void setEndAngle(double endAngle) {
		this.endAngle = endAngle;
	}
	/**
	 * @return Returns the initAngle.
	 */
	public double getInitAngle() {
		return initAngle;
	}
	/**
	 * @param initAngle The initAngle to set.
	 */
	public void setInitAngle(double initAngle) {
		this.initAngle = initAngle;
	}
	/**
	 * @return Returns the radius.
	 */
	public double getRadius() {
		return radius;
	}
	/**
	 * @param radius The radius to set.
	 */
	public void setRadius(double radius) {
		this.radius = radius;
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
		DwgArc dwgArc = new DwgArc();
		dwgArc.setType(type);
		dwgArc.setHandle(handle);
		dwgArc.setVersion(version);
		dwgArc.setMode(mode);
		dwgArc.setLayerHandle(layerHandle);
		dwgArc.setColor(color);
		dwgArc.setNumReactors(numReactors);
		dwgArc.setNoLinks(noLinks);
		dwgArc.setLinetypeFlags(linetypeFlags);
		dwgArc.setPlotstyleFlags(plotstyleFlags);
		dwgArc.setSizeInBits(sizeInBits);
		dwgArc.setExtendedData(extendedData);
		dwgArc.setGraphicData(graphicData);
		//dwgArc.setInsideBlock(insideBlock);
		dwgArc.setCenter(center);
		dwgArc.setRadius(radius);
		dwgArc.setThickness(thickness);
		dwgArc.setExtrusion(extrusion);
		dwgArc.setInitAngle(initAngle);
		dwgArc.setEndAngle(endAngle);
		return dwgArc;
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
