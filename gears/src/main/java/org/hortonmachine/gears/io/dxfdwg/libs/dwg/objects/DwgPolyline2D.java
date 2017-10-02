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

import java.awt.geom.Point2D;
import java.util.Vector;

import org.hortonmachine.gears.io.dxfdwg.libs.dwg.DwgObject;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.DwgUtil;

/**
 * The DwgPolyline2D class represents a DWG Polyline2D
 * 
 * @author jmorell
 */
public class DwgPolyline2D extends DwgObject {
	private int flags;
	private int curveType;
	private double initWidth;
	private double endWidth;
	private double thickness;
	private double elevation;
	private double[] extrusion;
	private int firstVertexHandle;
	private int lastVertexHandle;
	private int seqendHandle;
	private Point2D[] pts;
	private double[] bulges;
	
	/**
	 * Read a Polyline2D in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgPolyline2DV15(int[] data, int offset) throws Exception {
		//System.out.println("readDwgPolyline2D executing ...");
		int bitPos = offset;
		bitPos = readObjectHeaderV15(data, bitPos);
		Vector v = DwgUtil.getBitShort(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int flags = ((Integer)v.get(1)).intValue();
		this.flags = flags;
		v = DwgUtil.getBitShort(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int ctype = ((Integer)v.get(1)).intValue();
		curveType = ctype;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double sw = ((Double)v.get(1)).doubleValue();
		initWidth = sw;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double ew = ((Double)v.get(1)).doubleValue();
		endWidth = ew;
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		boolean flag = ((Boolean)v.get(1)).booleanValue();
	    double th = 0.0;
	    if (!flag) {
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			th = ((Double)v.get(1)).doubleValue();
	    }
	    this.thickness = th;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double elev = ((Double)v.get(1)).doubleValue();
		elevation = elev;
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		flag = ((Boolean)v.get(1)).booleanValue();
	    double ex, ey, ez = 0.0;
	    if (flag) {
	    	ex = 0.0;
	    	ey = 0.0;
	    	ez = 1.0;
	    } else {
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			ex = ((Double)v.get(1)).doubleValue();
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			ey = ((Double)v.get(1)).doubleValue();
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			ez = ((Double)v.get(1)).doubleValue();
	    }
	    extrusion = new double[]{ex, ey, ez};
		bitPos = readObjectTailV15(data, bitPos);
		v = DwgUtil.getHandle(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int[] handle = new int[v.size()-1];
	    for (int i=1;i<v.size();i++) {
		    handle[i-1] = ((Integer)v.get(i)).intValue();
	    }
	    Vector handleVect = new Vector();
	    for (int i=0;i<handle.length;i++) {
	    	handleVect.add(new Integer(handle[i]));
	    }
	    firstVertexHandle = DwgUtil.handleBinToHandleInt(handleVect);
		v = DwgUtil.getHandle(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		handle = new int[v.size()-1];
	    for (int i=1;i<v.size();i++) {
		    handle[i-1] = ((Integer)v.get(i)).intValue();
	    }
	    handleVect = new Vector();
	    for (int i=0;i<handle.length;i++) {
	    	handleVect.add(new Integer(handle[i]));
	    }
	    lastVertexHandle = DwgUtil.handleBinToHandleInt(handleVect);
		v = DwgUtil.getHandle(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		handle = new int[v.size()-1];
	    for (int i=1;i<v.size();i++) {
		    handle[i-1] = ((Integer)v.get(i)).intValue();
	    }
	    handleVect = new Vector();
	    for (int i=0;i<handle.length;i++) {
	    	handleVect.add(new Integer(handle[i]));
	    }
	    seqendHandle = DwgUtil.handleBinToHandleInt(handleVect);
	}
	/**
	 * @return Returns the firstVertexHandle.
	 */
	public int getFirstVertexHandle() {
		return firstVertexHandle;
	}
	/**
	 * @param firstVertexHandle The firstVertexHandle to set.
	 */
	public void setFirstVertexHandle(int firstVertexHandle) {
		this.firstVertexHandle = firstVertexHandle;
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
	 * @return Returns the lastVertexHandle.
	 */
	public int getLastVertexHandle() {
		return lastVertexHandle;
	}
	/**
	 * @param lastVertexHandle The lastVertexHandle to set.
	 */
	public void setLastVertexHandle(int lastVertexHandle) {
		this.lastVertexHandle = lastVertexHandle;
	}
	/**
	 * @return Returns the pts.
	 */
	public Point2D[] getPts() {
		return pts;
	}
	/**
	 * @param pts The pts to set.
	 */
	public void setPts(Point2D[] pts) {
		this.pts = pts;
	}
	/**
	 * @return Returns the bulges.
	 */
	public double[] getBulges() {
		return bulges;
	}
	/**
	 * @param bulges The bulges to set.
	 */
	public void setBulges(double[] bulges) {
		this.bulges = bulges;
	}
	/**
	 * @return Returns the initWidth.
	 */
	public double getInitWidth() {
		return initWidth;
	}
	/**
	 * @param initWidth The initWidth to set.
	 */
	public void setInitWidth(double initWidth) {
		this.initWidth = initWidth;
	}
	/**
	 * @return Returns the seqendHandle.
	 */
	public int getSeqendHandle() {
		return seqendHandle;
	}
	/**
	 * @param seqendHandle The seqendHandle to set.
	 */
	public void setSeqendHandle(int seqendHandle) {
		this.seqendHandle = seqendHandle;
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
	 * @return Returns the curveType.
	 */
	public int getCurveType() {
		return curveType;
	}
	/**
	 * @param curveType The curveType to set.
	 */
	public void setCurveType(int curveType) {
		this.curveType = curveType;
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
	 * @return Returns the endWidth.
	 */
	public double getEndWidth() {
		return endWidth;
	}
	/**
	 * @param endWidth The endWidth to set.
	 */
	public void setEndWidth(double endWidth) {
		this.endWidth = endWidth;
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
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		DwgPolyline2D dwgPolyline2D = new DwgPolyline2D();
		dwgPolyline2D.setType(type);
		dwgPolyline2D.setHandle(handle);
		dwgPolyline2D.setVersion(version);
		dwgPolyline2D.setMode(mode);
		dwgPolyline2D.setLayerHandle(layerHandle);
		dwgPolyline2D.setColor(color);
		dwgPolyline2D.setNumReactors(numReactors);
		dwgPolyline2D.setNoLinks(noLinks);
		dwgPolyline2D.setLinetypeFlags(linetypeFlags);
		dwgPolyline2D.setPlotstyleFlags(plotstyleFlags);
		dwgPolyline2D.setSizeInBits(sizeInBits);
		dwgPolyline2D.setExtendedData(extendedData);
		dwgPolyline2D.setGraphicData(graphicData);
		//dwgPolyline2D.setInsideBlock(insideBlock);
		dwgPolyline2D.setFlags(flags);
		dwgPolyline2D.setCurveType(curveType);
		dwgPolyline2D.setInitWidth(initWidth);
		dwgPolyline2D.setEndWidth(endWidth);
		dwgPolyline2D.setThickness(thickness);
		dwgPolyline2D.setElevation(elevation);
		dwgPolyline2D.setExtrusion(extrusion);
		dwgPolyline2D.setFirstVertexHandle(firstVertexHandle);
		dwgPolyline2D.setLastVertexHandle(lastVertexHandle);
		dwgPolyline2D.setSeqendHandle(seqendHandle);
		dwgPolyline2D.setPts(pts);
		dwgPolyline2D.setBulges(bulges);
		return dwgPolyline2D;
	}
}
