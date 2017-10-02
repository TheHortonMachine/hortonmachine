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
 * The DwgBlockHeader class represents a DWG Block header
 * 
 * @author jmorell
 */
public class DwgBlockHeader extends DwgObject {
	private String name;
	private boolean flag64;
	private int xRefPlus;
	private boolean xdep;
	private boolean anonymous;
	private boolean hasAttrs;
	private boolean blkIsXRef;
	private boolean xRefOverLaid;
	private boolean loaded;
	private double[] basePoint;
	private String xRefPName;
	private String blockDescription;
	private int previewData;
	private int blockControlHandle;
	private int nullHandle;
	private int blockEntityHandle;
	private int firstEntityHandle;
	private int lastEntityHandle;
	private int endBlkEntityHandle;
	private Vector insertHandles;
	private int layoutHandle;
	private Vector objects;
	
	/**
	 * Create new DwgBlockHeader object
	 */
	public DwgBlockHeader() {
		objects = new Vector();
	}
	/**
	 * Read a Block header in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgBlockHeaderV15(int[] data, int offset) throws Exception {
		int bitPos = offset;
		Vector v = DwgUtil.getBitLong(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int numReactors = ((Integer)v.get(1)).intValue();
		setNumReactors(numReactors);
		v = DwgUtil.getTextString(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		String name = (String)v.get(1);
		this.name = name;
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		boolean flag = ((Boolean)v.get(1)).booleanValue();
		flag64 = flag;
		v = DwgUtil.getBitShort(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int xrefplus1 = ((Integer)v.get(1)).intValue();
		xRefPlus = xrefplus1;
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		boolean xdep = ((Boolean)v.get(1)).booleanValue();
		this.xdep = xdep;
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		boolean anon = ((Boolean)v.get(1)).booleanValue();
		anonymous = anon;
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		boolean hasatts = ((Boolean)v.get(1)).booleanValue();
		hasAttrs = hasatts;
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		boolean bxref = ((Boolean)v.get(1)).booleanValue();
		blkIsXRef = bxref;
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		boolean xover = ((Boolean)v.get(1)).booleanValue();
		xRefOverLaid = xover;
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		boolean loaded = ((Boolean)v.get(1)).booleanValue();
		this.loaded = loaded;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double bx = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double by = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double bz = ((Double)v.get(1)).doubleValue();
		double[] coord = new double[]{bx, by, bz};
		basePoint = coord;
		v = DwgUtil.getTextString(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		String pname = (String)v.get(1);
		xRefPName = pname;
		int icount = 0;
		while (true) {
			v = DwgUtil.getRawChar(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			int val = ((Integer)v.get(1)).intValue();
			if (val==0) {
				break;
			}
			icount++;
		}
		v = DwgUtil.getTextString(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		String desc = (String)v.get(1);
		blockDescription = desc;
		v = DwgUtil.getBitLong(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int pdsize = ((Integer)v.get(1)).intValue();
		if (pdsize>0) {
			int count = pdsize + icount;
			//int pdata = ((Integer)DwgUtil.getBits(data, count, bitPos)).intValue();
			//previewData = pdata;
			bitPos = bitPos + count;
		}
		v = DwgUtil.getHandle(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int[] handle = new int[v.size()-1];
	    for (int j=1;j<v.size();j++) {
		    handle[j-1] = ((Integer)v.get(j)).intValue();
	    }
	    Vector handleVect = new Vector();
	    for (int i=0;i<handle.length;i++) {
	    	handleVect.add(new Integer(handle[i]));
	    }
	    blockControlHandle = DwgUtil.handleBinToHandleInt(handleVect);
		for (int i=0;i<numReactors;i++) {
			v = DwgUtil.getHandle(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			handle = new int[v.size()-1];
		    for (int j=1;j<v.size();j++) {
			    handle[j-1] = ((Integer)v.get(j)).intValue();
		    }
		}
		v = DwgUtil.getHandle(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		handle = new int[v.size()-1];
	    for (int j=1;j<v.size();j++) {
		    handle[j-1] = ((Integer)v.get(j)).intValue();
	    }
		v = DwgUtil.getHandle(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		handle = new int[v.size()-1];
	    for (int j=1;j<v.size();j++) {
		    handle[j-1] = ((Integer)v.get(j)).intValue();
	    }
	    handleVect = new Vector();
	    for (int i=0;i<handle.length;i++) {
	    	handleVect.add(new Integer(handle[i]));
	    }
	    nullHandle = DwgUtil.handleBinToHandleInt(handleVect);
		v = DwgUtil.getHandle(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		handle = new int[v.size()-1];
	    for (int j=1;j<v.size();j++) {
		    handle[j-1] = ((Integer)v.get(j)).intValue();
	    }
	    handleVect = new Vector();
	    for (int i=0;i<handle.length;i++) {
	    	handleVect.add(new Integer(handle[i]));
	    }
	    blockEntityHandle = DwgUtil.handleBinToHandleInt(handleVect);
		if ((!bxref) && (!xover)) {
			v = DwgUtil.getHandle(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			handle = new int[v.size()-1];
		    for (int j=1;j<v.size();j++) {
			    handle[j-1] = ((Integer)v.get(j)).intValue();
		    }
		    handleVect = new Vector();
		    for (int i=0;i<handle.length;i++) {
		    	handleVect.add(new Integer(handle[i]));
		    }
		    firstEntityHandle = DwgUtil.handleBinToHandleInt(handleVect);
			v = DwgUtil.getHandle(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			handle = new int[v.size()-1];
		    for (int j=1;j<v.size();j++) {
			    handle[j-1] = ((Integer)v.get(j)).intValue();
		    }
		    handleVect = new Vector();
		    for (int i=0;i<handle.length;i++) {
		    	handleVect.add(new Integer(handle[i]));
		    }
		    lastEntityHandle = DwgUtil.handleBinToHandleInt(handleVect);
		}
		v = DwgUtil.getHandle(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		handle = new int[v.size()-1];
	    for (int j=1;j<v.size();j++) {
		    handle[j-1] = ((Integer)v.get(j)).intValue();
	    }
	    handleVect = new Vector();
	    for (int i=0;i<handle.length;i++) {
	    	handleVect.add(new Integer(handle[i]));
	    }
	    endBlkEntityHandle = DwgUtil.handleBinToHandleInt(handleVect);
		if (icount>0) {
			Vector handles = new Vector();
			for (int i=0;i<icount;i++) {
				v = DwgUtil.getHandle(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				handle = new int[v.size()-1];
			    for (int j=1;j<v.size();j++) {
				    handle[j-1] = ((Integer)v.get(j)).intValue();
			    }
				handles.add(handle);
			}
		    insertHandles = handles;
		}
		v = DwgUtil.getHandle(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		handle = new int[v.size()-1];
	    for (int j=1;j<v.size();j++) {
		    handle[j-1] = ((Integer)v.get(j)).intValue();
	    }
	    handleVect = new Vector();
	    for (int i=0;i<handle.length;i++) {
	    	handleVect.add(new Integer(handle[i]));
	    }
	    layoutHandle = DwgUtil.handleBinToHandleInt(handleVect);
	}
	/**
	 * @return Returns the basePoint.
	 */
	public double[] getBasePoint() {
		return basePoint;
	}
	/**
	 * @param basePoint The basePoint to set.
	 */
	public void setBasePoint(double[] basePoint) {
		this.basePoint = basePoint;
	}
	/**
	 * @return Returns the firstEntityHandle.
	 */
	public int getFirstEntityHandle() {
		return firstEntityHandle;
	}
	/**
	 * @param firstEntityHandle The firstEntityHandle to set.
	 */
	public void setFirstEntityHandle(int firstEntityHandle) {
		this.firstEntityHandle = firstEntityHandle;
	}
	/**
	 * @return Returns the lastEntityHandle.
	 */
	public int getLastEntityHandle() {
		return lastEntityHandle;
	}
	/**
	 * @param lastEntityHandle The lastEntityHandle to set.
	 */
	public void setLastEntityHandle(int lastEntityHandle) {
		this.lastEntityHandle = lastEntityHandle;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return Returns the blockEntityHandle.
	 */
	public int getBlockEntityHandle() {
		return blockEntityHandle;
	}
	/**
	 * @param blockEntityHandle The blockEntityHandle to set.
	 */
	public void setBlockEntityHandle(int blockEntityHandle) {
		this.blockEntityHandle = blockEntityHandle;
	}
	/**
	 * @return Returns the anonymous.
	 */
	public boolean isAnonymous() {
		return anonymous;
	}
	/**
	 * @param anonymous The anonymous to set.
	 */
	public void setAnonymous(boolean anonymous) {
		this.anonymous = anonymous;
	}
	/**
	 * @return Returns the blkIsXRef.
	 */
	public boolean isBlkIsXRef() {
		return blkIsXRef;
	}
	/**
	 * @param blkIsXRef The blkIsXRef to set.
	 */
	public void setBlkIsXRef(boolean blkIsXRef) {
		this.blkIsXRef = blkIsXRef;
	}
	/**
	 * @return Returns the blockControlHandle.
	 */
	public int getBlockControlHandle() {
		return blockControlHandle;
	}
	/**
	 * @param blockControlHandle The blockControlHandle to set.
	 */
	public void setBlockControlHandle(int blockControlHandle) {
		this.blockControlHandle = blockControlHandle;
	}
	/**
	 * @return Returns the blockDescription.
	 */
	public String getBlockDescription() {
		return blockDescription;
	}
	/**
	 * @param blockDescription The blockDescription to set.
	 */
	public void setBlockDescription(String blockDescription) {
		this.blockDescription = blockDescription;
	}
	/**
	 * @return Returns the endBlkEntityHandle.
	 */
	public int getEndBlkEntityHandle() {
		return endBlkEntityHandle;
	}
	/**
	 * @param endBlkEntityHandle The endBlkEntityHandle to set.
	 */
	public void setEndBlkEntityHandle(int endBlkEntityHandle) {
		this.endBlkEntityHandle = endBlkEntityHandle;
	}
	/**
	 * @return Returns the flag64.
	 */
	public boolean isFlag64() {
		return flag64;
	}
	/**
	 * @param flag64 The flag64 to set.
	 */
	public void setFlag64(boolean flag64) {
		this.flag64 = flag64;
	}
	/**
	 * @return Returns the hasAttrs.
	 */
	public boolean isHasAttrs() {
		return hasAttrs;
	}
	/**
	 * @param hasAttrs The hasAttrs to set.
	 */
	public void setHasAttrs(boolean hasAttrs) {
		this.hasAttrs = hasAttrs;
	}
	/**
	 * @return Returns the insertHandles.
	 */
	public Vector getInsertHandles() {
		return insertHandles;
	}
	/**
	 * @param insertHandles The insertHandles to set.
	 */
	public void setInsertHandles(Vector insertHandles) {
		this.insertHandles = insertHandles;
	}
	/**
	 * @return Returns the layoutHandle.
	 */
	public int getLayoutHandle() {
		return layoutHandle;
	}
	/**
	 * @param layoutHandle The layoutHandle to set.
	 */
	public void setLayoutHandle(int layoutHandle) {
		this.layoutHandle = layoutHandle;
	}
	/**
	 * @return Returns the loaded.
	 */
	public boolean isLoaded() {
		return loaded;
	}
	/**
	 * @param loaded The loaded to set.
	 */
	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}
	/**
	 * @return Returns the nullHandle.
	 */
	public int getNullHandle() {
		return nullHandle;
	}
	/**
	 * @param nullHandle The nullHandle to set.
	 */
	public void setNullHandle(int nullHandle) {
		this.nullHandle = nullHandle;
	}
	/**
	 * @return Returns the previewData.
	 */
	public int getPreviewData() {
		return previewData;
	}
	/**
	 * @param previewData The previewData to set.
	 */
	public void setPreviewData(int previewData) {
		this.previewData = previewData;
	}
	/**
	 * @return Returns the xdep.
	 */
	public boolean isXdep() {
		return xdep;
	}
	/**
	 * @param xdep The xdep to set.
	 */
	public void setXdep(boolean xdep) {
		this.xdep = xdep;
	}
	/**
	 * @return Returns the xRefOverLaid.
	 */
	public boolean isXRefOverLaid() {
		return xRefOverLaid;
	}
	/**
	 * @param refOverLaid The xRefOverLaid to set.
	 */
	public void setXRefOverLaid(boolean refOverLaid) {
		xRefOverLaid = refOverLaid;
	}
	/**
	 * @return Returns the xRefPlus.
	 */
	public int getXRefPlus() {
		return xRefPlus;
	}
	/**
	 * @param refPlus The xRefPlus to set.
	 */
	public void setXRefPlus(int refPlus) {
		xRefPlus = refPlus;
	}
	/**
	 * @return Returns the xRefPName.
	 */
	public String getXRefPName() {
		return xRefPName;
	}
	/**
	 * @param refPName The xRefPName to set.
	 */
	public void setXRefPName(String refPName) {
		xRefPName = refPName;
	}
	/**
	 * @return Returns the objects.
	 */
	public Vector getObjects() {
		return objects;
	}
	/**
	 * @param objects The objects to set.
	 */
	public void setObjects(Vector objects) {
		this.objects = objects;
	}
	/**
	 * Add a DWG object to the blockObjects vector
	 * 
	 * @param object DWG object
	 */
	public void addObject(DwgObject object) {
		this.objects.add(object);
	}
}
