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
 * The DwgInsert class represents a DWG Insert
 * 
 * @author jmorell
 */
public class DwgInsert extends DwgObject {
	private double[] insertionPoint;
	private double[] scale;
	private double rotation;
	private double[] extrusion;
	private int blockHeaderHandle;
	private int firstAttribHandle;
	private int lastAttribHandle;
	private int seqendHandle;
	
	/**
	 * Read a Insert in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgInsertV15(int[] data, int offset) throws Exception {
		//System.out.println("readDwgInsert() executed ...");
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
		insertionPoint = coord;
		int dflag = ((Integer)DwgUtil.getBits(data, 2, bitPos)).intValue();
		bitPos = bitPos + 2;
		if (dflag==0x0) {
			v = DwgUtil.getRawDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			x = ((Double)v.get(1)).doubleValue();
			v = DwgUtil.getDefaultDouble(data, bitPos, x);
			bitPos = ((Integer)v.get(0)).intValue();
			y = ((Double)v.get(1)).doubleValue();
			v = DwgUtil.getDefaultDouble(data, bitPos, x);
			bitPos = ((Integer)v.get(0)).intValue();
			z = ((Double)v.get(1)).doubleValue();
		} else if (dflag==0x1) {
			x = 1.0;
			v = DwgUtil.getDefaultDouble(data, bitPos, x);
			bitPos = ((Integer)v.get(0)).intValue();
			y = ((Double)v.get(1)).doubleValue();
			v = DwgUtil.getDefaultDouble(data, bitPos, x);
			bitPos = ((Integer)v.get(0)).intValue();
			z = ((Double)v.get(1)).doubleValue();
		} else if (dflag==0x2) {
			v = DwgUtil.getRawDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			x = ((Double)v.get(1)).doubleValue();
			z = x;
			y = z;
		} else {
			z = 1.0;
			y = z;
			x = y;
		}
		coord = new double[]{x, y, z};
		scale = coord;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double rot = ((Double)v.get(1)).doubleValue();
		rotation = rot;
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
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		boolean hasattr = ((Boolean)v.get(1)).booleanValue();
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
	    blockHeaderHandle = DwgUtil.handleBinToHandleInt(handleVect);
		if (hasattr) {
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
		    firstAttribHandle = DwgUtil.handleBinToHandleInt(handleVect);
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
		    lastAttribHandle = DwgUtil.handleBinToHandleInt(handleVect);
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
	}
	/**
	 * @return Returns the blockHeaderHandle.
	 */
	public int getBlockHeaderHandle() {
		return blockHeaderHandle;
	}
	/**
	 * @param blockHeaderHandle The blockHeaderHandle to set.
	 */
	public void setBlockHeaderHandle(int blockHeaderHandle) {
		this.blockHeaderHandle = blockHeaderHandle;
	}
	/**
	 * @return Returns the firstAttribHandle.
	 */
	public int getFirstAttribHandle() {
		return firstAttribHandle;
	}
	/**
	 * @param firstAttribHandle The firstAttribHandle to set.
	 */
	public void setFirstAttribHandle(int firstAttribHandle) {
		this.firstAttribHandle = firstAttribHandle;
	}
	/**
	 * @return Returns the insertionPoint.
	 */
	public double[] getInsertionPoint() {
		return insertionPoint;
	}
	/**
	 * @param insertionPoint The insertionPoint to set.
	 */
	public void setInsertionPoint(double[] insertionPoint) {
		this.insertionPoint = insertionPoint;
	}
	/**
	 * @return Returns the lastAttribHandle.
	 */
	public int getLastAttribHandle() {
		return lastAttribHandle;
	}
	/**
	 * @param lastAttribHandle The lastAttribHandle to set.
	 */
	public void setLastAttribHandle(int lastAttribHandle) {
		this.lastAttribHandle = lastAttribHandle;
	}
	/**
	 * @return Returns the rotation.
	 */
	public double getRotation() {
		return rotation;
	}
	/**
	 * @param rotation The rotation to set.
	 */
	public void setRotation(double rotation) {
		this.rotation = rotation;
	}
	/**
	 * @return Returns the scale.
	 */
	public double[] getScale() {
		return scale;
	}
	/**
	 * @param scale The scale to set.
	 */
	public void setScale(double[] scale) {
		this.scale = scale;
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
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		DwgInsert dwgInsert = new DwgInsert();
		dwgInsert.setType(type);
		dwgInsert.setHandle(handle);
		dwgInsert.setVersion(version);
		dwgInsert.setMode(mode);
		dwgInsert.setLayerHandle(layerHandle);
		dwgInsert.setColor(color);
		dwgInsert.setNumReactors(numReactors);
		dwgInsert.setNoLinks(noLinks);
		dwgInsert.setLinetypeFlags(linetypeFlags);
		dwgInsert.setPlotstyleFlags(plotstyleFlags);
		dwgInsert.setSizeInBits(sizeInBits);
		dwgInsert.setExtendedData(extendedData);
		dwgInsert.setGraphicData(graphicData);
		//dwgInsert.setInsideBlock(insideBlock);
		dwgInsert.setInsertionPoint(insertionPoint);
		dwgInsert.setScale(scale);
		dwgInsert.setRotation(rotation);
		dwgInsert.setExtrusion(extrusion);
		dwgInsert.setBlockHeaderHandle(blockHeaderHandle);
		dwgInsert.setFirstAttribHandle(firstAttribHandle);
		dwgInsert.setLastAttribHandle(lastAttribHandle);
		dwgInsert.setSeqendHandle(seqendHandle);
		return dwgInsert;
	}
}
