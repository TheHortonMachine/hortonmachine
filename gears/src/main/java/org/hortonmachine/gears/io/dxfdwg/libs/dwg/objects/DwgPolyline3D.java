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
 * The DwgPolyline3D class represents a DWG Polyline3D
 * 
 * @author jmorell
 */
public class DwgPolyline3D extends DwgObject {
	private int splineFlags;
	private int closedFlags;
	private int firstVertexHandle;
	private int lastVertexHandle;
	private int seqendHandle;
	private double[][] pts;
	private double[] bulges;
	
	/**
	 * Read a Polyline3D in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgPolyline3DV15(int[] data, int offset) throws Exception {
		int bitPos = offset;
		bitPos = readObjectHeaderV15(data, bitPos);
		Vector v = DwgUtil.getRawChar(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int sflags = ((Integer)v.get(1)).intValue();
		splineFlags = sflags;
		v = DwgUtil.getRawChar(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int cflags = ((Integer)v.get(1)).intValue();
		closedFlags = cflags;
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
	 * @return Returns the closedFlags.
	 */
	public int getClosedFlags() {
		return closedFlags;
	}
	/**
	 * @param closedFlags The closedFlags to set.
	 */
	public void setClosedFlags(int closedFlags) {
		this.closedFlags = closedFlags;
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
	public double[][] getPts() {
		return pts;
	}
	/**
	 * @param pts The pts to set.
	 */
	public void setPts(double[][] pts) {
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
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		DwgPolyline3D dwgPolyline3D = new DwgPolyline3D();
		dwgPolyline3D.setType(type);
		dwgPolyline3D.setHandle(handle);
		dwgPolyline3D.setVersion(version);
		dwgPolyline3D.setMode(mode);
		dwgPolyline3D.setLayerHandle(layerHandle);
		dwgPolyline3D.setColor(color);
		dwgPolyline3D.setNumReactors(numReactors);
		dwgPolyline3D.setNoLinks(noLinks);
		dwgPolyline3D.setLinetypeFlags(linetypeFlags);
		dwgPolyline3D.setPlotstyleFlags(plotstyleFlags);
		dwgPolyline3D.setSizeInBits(sizeInBits);
		dwgPolyline3D.setExtendedData(extendedData);
		dwgPolyline3D.setGraphicData(graphicData);
		//dwgPolyline3D.setInsideBlock(insideBlock);
		dwgPolyline3D.setSplineFlags(splineFlags);
		dwgPolyline3D.setClosedFlags(closedFlags);
		dwgPolyline3D.setFirstVertexHandle(firstVertexHandle);
		dwgPolyline3D.setLastVertexHandle(lastVertexHandle);
		dwgPolyline3D.setSeqendHandle(seqendHandle);
		dwgPolyline3D.setPts(pts);
		dwgPolyline3D.setBulges(bulges);
		return dwgPolyline3D;
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
	 * @return Returns the splineFlags.
	 */
	public int getSplineFlags() {
		return splineFlags;
	}
	/**
	 * @param splineFlags The splineFlags to set.
	 */
	public void setSplineFlags(int splineFlags) {
		this.splineFlags = splineFlags;
	}
}
