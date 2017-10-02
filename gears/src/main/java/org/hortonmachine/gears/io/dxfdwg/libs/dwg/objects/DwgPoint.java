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
 * The DwgPoint class represents a DWG Point
 * 
 * @author jmorell
 */
public class DwgPoint extends DwgObject {
	private double[] point;
	private double thickness;
	private double[] extrusion;
	private double xAxisAngle;
	
	/**
	 * Read a Point in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgPointV15(int[] data, int offset) throws Exception {
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
		point = coord;
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
	    xAxisAngle = val;
		bitPos = readObjectTailV15(data, bitPos);
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
	/**
	 * @return Returns the xAxisAngle.
	 */
	public double getXAxisAngle() {
		return xAxisAngle;
	}
	/**
	 * @param axisAngle The xAxisAngle to set.
	 */
	public void setXAxisAngle(double axisAngle) {
		xAxisAngle = axisAngle;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		DwgPoint dwgPoint = new DwgPoint();
		dwgPoint.setType(type);
		dwgPoint.setHandle(handle);
		dwgPoint.setVersion(version);
		dwgPoint.setMode(mode);
		dwgPoint.setLayerHandle(layerHandle);
		dwgPoint.setColor(color);
		dwgPoint.setNumReactors(numReactors);
		dwgPoint.setNoLinks(noLinks);
		dwgPoint.setLinetypeFlags(linetypeFlags);
		dwgPoint.setPlotstyleFlags(plotstyleFlags);
		dwgPoint.setSizeInBits(sizeInBits);
		dwgPoint.setExtendedData(extendedData);
		dwgPoint.setGraphicData(graphicData);
		//dwgPoint.setInsideBlock(insideBlock);
		dwgPoint.setPoint(point);
		dwgPoint.setThickness(thickness);
		dwgPoint.setXAxisAngle(xAxisAngle);
		dwgPoint.setExtrusion(extrusion);
		return dwgPoint;
	}
}
