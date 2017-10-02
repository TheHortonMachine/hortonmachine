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
 * The DwgEllipse class represents a DWG Ellipse
 * 
 * @author jmorell
 */
public class DwgEllipse extends DwgObject {
	private double[] center;
	private double[] majorAxisVector;
	private double[] extrusion;
	private double axisRatio;
	private double initAngle;
	private double endAngle;
	
	/**
	 * Read a Ellipse in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgEllipseV15(int[] data, int offset) throws Exception {
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
		x = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		y = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		z = ((Double)v.get(1)).doubleValue();
		coord = new double[]{x, y, z};
		majorAxisVector = coord;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		x = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		y = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		z = ((Double)v.get(1)).doubleValue();
		coord = new double[]{x, y, z};
		extrusion = coord;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double val = ((Double)v.get(1)).doubleValue();
		axisRatio = val;
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
     * @return Returns the axisRatio.
     */
    public double getAxisRatio() {
        return axisRatio;
    }
    /**
     * @param axisRatio The axisRatio to set.
     */
    public void setAxisRatio(double axisRatio) {
        this.axisRatio = axisRatio;
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
     * @return Returns the majorAxisVector.
     */
    public double[] getMajorAxisVector() {
        return majorAxisVector;
    }
    /**
     * @param majorAxisVector The majorAxisVector to set.
     */
    public void setMajorAxisVector(double[] majorAxisVector) {
        this.majorAxisVector = majorAxisVector;
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
		DwgEllipse dwgEllipse = new DwgEllipse();
		dwgEllipse.setType(type);
		dwgEllipse.setHandle(handle);
		dwgEllipse.setVersion(version);
		dwgEllipse.setMode(mode);
		dwgEllipse.setLayerHandle(layerHandle);
		dwgEllipse.setColor(color);
		dwgEllipse.setNumReactors(numReactors);
		dwgEllipse.setNoLinks(noLinks);
		dwgEllipse.setLinetypeFlags(linetypeFlags);
		dwgEllipse.setPlotstyleFlags(plotstyleFlags);
		dwgEllipse.setSizeInBits(sizeInBits);
		dwgEllipse.setExtendedData(extendedData);
		dwgEllipse.setGraphicData(graphicData);
		//dwgEllipse.setInsideBlock(insideBlock);
		dwgEllipse.setCenter(center);
		dwgEllipse.setMajorAxisVector(majorAxisVector);
		dwgEllipse.setExtrusion(extrusion);
		dwgEllipse.setAxisRatio(axisRatio);
		dwgEllipse.setInitAngle(initAngle);
		dwgEllipse.setEndAngle(endAngle);
		return dwgEllipse;
	}
	/**
	 * @param extrusion The extrusion to set.
	 */
	public void setExtrusion(double[] extrusion) {
		this.extrusion = extrusion;
	}
}
