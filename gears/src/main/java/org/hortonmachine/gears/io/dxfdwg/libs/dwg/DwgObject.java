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
package org.hortonmachine.gears.io.dxfdwg.libs.dwg;

import java.util.Vector;

/**
 * The DwgObject class represents a DWG object
 * 
 * @author jmorell
 */
public class DwgObject {
	protected int type;
	protected int handle;
	protected int layerHandleCode;
	protected String version;
	protected int mode;
	protected int layerHandle;
	protected int color;
	protected int numReactors;
	protected boolean noLinks;
	protected int linetypeFlags;
	protected int plotstyleFlags;
	protected int sizeInBits;
	protected Vector extendedData;
	protected int graphicData;
	protected int subEntityHandle;
	protected int xDicObjHandle;
	protected boolean graphicsFlag;
	
	/**
	 * Reads the header of an object in a DWG file Version 15 
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @return int New offset
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public int readObjectHeaderV15(int[] data, int offset) throws Exception {
		int bitPos = offset;
		Integer mode = (Integer)DwgUtil.getBits(data, 2, bitPos);
	    bitPos = bitPos + 2;
	    setMode(mode.intValue());
	    Vector v = DwgUtil.getBitLong(data, bitPos);
	    bitPos = ((Integer)v.get(0)).intValue();
	    int rnum = ((Integer)v.get(1)).intValue();
	    setNumReactors(rnum);
	    v = DwgUtil.testBit(data, bitPos);
	    bitPos = ((Integer)v.get(0)).intValue();
	    boolean nolinks = ((Boolean)v.get(1)).booleanValue();
	    setNoLinks(nolinks);
	    v = DwgUtil.getBitShort(data, bitPos);
	    bitPos = ((Integer)v.get(0)).intValue();
	    int color = ((Integer)v.get(1)).intValue();
	    setColor(color);
	    v = DwgUtil.getBitDouble(data, bitPos);
	    bitPos = ((Integer)v.get(0)).intValue();
	    float ltscale = ((Double)v.get(1)).floatValue();
	    Integer ltflag = (Integer)DwgUtil.getBits(data, 2, bitPos);
	    bitPos = bitPos + 2;
	    Integer psflag = (Integer)DwgUtil.getBits(data, 2, bitPos);
	    bitPos = bitPos + 2;
	    v = DwgUtil.getBitShort(data, bitPos);
	    bitPos = ((Integer)v.get(0)).intValue();
	    int invis = ((Integer)v.get(1)).intValue();
	    v = DwgUtil.getRawChar(data, bitPos);
	    bitPos = ((Integer)v.get(0)).intValue();
	    int weight = ((Integer)v.get(1)).intValue();
		return bitPos;
	}
	
	/**
	 * Reads the tail of an object in a DWG file Version 15 
	 * 
	 * @param data Array of bytes obtained from the DWG binary file
	 * @param offset Offset for this array of bytes
	 * @return int New offset
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public int readObjectTailV15(int[] data, int offset) throws Exception {
		int bitPos = offset;
		Vector v = null;
		if (getMode()==0x0) {
			v = DwgUtil.getHandle(data, bitPos);
		    bitPos = ((Integer)v.get(0)).intValue();
		    int[] sh = new int[v.size()-1];
		    for (int i=1;i<v.size();i++) {
			    sh[i-1] = ((Integer)v.get(i)).intValue();
		    }
		    Vector shv = new Vector();
		    for (int i=0;i<sh.length;i++) {
		    	shv.add(new Integer(sh[i]));
		    }
		    setSubEntityHandle(DwgUtil.handleBinToHandleInt(shv));
		}
		for (int i=0; i<getNumReactors(); i++) {
			v = DwgUtil.getHandle(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			int[] handle = new int[v.size()-1];
		    for (int j=1;j<v.size();j++) {
			    handle[j-1] = ((Integer)v.get(j)).intValue();
		    }
		}
		v = DwgUtil.getHandle(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int[] xh = new int[v.size()-1];
	    for (int i=1;i<v.size();i++) {
		    xh[i-1] = ((Integer)v.get(i)).intValue();
	    }
	    Vector xhv = new Vector();
	    for (int i=0;i<xh.length;i++) {
	    	xhv.add(new Integer(xh[i]));
	    }
	    setXDicObjHandle(DwgUtil.handleBinToHandleInt(xhv));
		v = DwgUtil.getHandle(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int[] lh = new int[v.size()-1];
	    for (int i=1;i<v.size();i++) {
		    lh[i-1] = ((Integer)v.get(i)).intValue();
	    }
	    setLayerHandleCode(lh[0]);
	    Vector lhv = new Vector();
	    for (int i=0;i<lh.length;i++) {
	    	lhv.add(new Integer(lh[i]));
	    }
	    setLayerHandle(DwgUtil.handleBinToHandleInt(lhv));
	    if (!isNoLinks()) {
			v = DwgUtil.getHandle(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			int[] prev = new int[v.size()-1];
		    for (int i=1;i<v.size();i++) {
			    prev[i-1] = ((Integer)v.get(i)).intValue();
		    }
		    //obj.setPrevious(prev);
			v = DwgUtil.getHandle(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			int[] next = new int[v.size()-1];
		    for (int i=1;i<v.size();i++) {
			    next[i-1] = ((Integer)v.get(i)).intValue();
		    }
		    //obj.setNext(next);
	    }
	    if (getLinetypeFlags()==0x3) {
			v = DwgUtil.getHandle(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			int[] lth = new int[v.size()-1];
		    for (int i=1;i<v.size();i++) {
			    lth[i-1] = ((Integer)v.get(i)).intValue();
		    }
		    //obj.setLinetype(lth);
	    }
	    if (getPlotstyleFlags()==0x3) {
			v = DwgUtil.getHandle(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			int[] pth = new int[v.size()-1];
		    for (int i=1;i<v.size();i++) {
			    pth[i-1] = ((Integer)v.get(i)).intValue();
		    }
		    //obj.setPlotstyle(pth);
	    }
	    return bitPos;
	}
	
	/**
	 * @return Returns the sizeInBits.
	 */
	public int getSizeInBits() {
		return sizeInBits;
	}
	/**
	 * @param sizeInBits The sizeInBits to set.
	 */
	public void setSizeInBits(int sizeInBits) {
		this.sizeInBits = sizeInBits;
	}
	/**
	 * @return Returns the extendedData.
	 */
	public Vector getExtendedData() {
		return extendedData;
	}
	/**
	 * @param extendedData The extendedData to set.
	 */
	public void setExtendedData(Vector extendedData) {
		this.extendedData = extendedData;
	}
	/**
	 * @return Returns the graphicData.
	 */
	public int getGraphicData() {
		return graphicData;
	}
	/**
	 * @param graphicData The graphicData to set.
	 */
	public void setGraphicData(int graphicData) {
		this.graphicData = graphicData;
	}
	/**
	 * @return Returns the version.
	 */
	public String getVersion() {
		return version;
	}
	/**
	 * @param linetypeFlags The linetypeFlags to set.
	 */
	public void setLinetypeFlags(int linetypeFlags) {
		this.linetypeFlags = linetypeFlags;
	}
	/**
	 * @param plotstyleFlags The plotstyleFlags to set.
	 */
	public void setPlotstyleFlags(int plotstyleFlags) {
		this.plotstyleFlags = plotstyleFlags;
	}
	/**
	 * @return Returns the subEntityHandle.
	 */
	public int getSubEntityHandle() {
		return subEntityHandle;
	}
	/**
	 * @param subEntityHandle The subEntityHandle to set.
	 */
	public void setSubEntityHandle(int subEntityHandle) {
		this.subEntityHandle = subEntityHandle;
	}
	/**
	 * @return Returns the xDicObjHandle.
	 */
	public int getXDicObjHandle() {
		return xDicObjHandle;
	}
	/**
	 * @param dicObjHandle The xDicObjHandle to set.
	 */
	public void setXDicObjHandle(int dicObjHandle) {
		xDicObjHandle = dicObjHandle;
	}
	/**
	 * @return Returns the layerHandleCode.
	 */
	public int getLayerHandleCode() {
		return layerHandleCode;
	}
	/**
	 * @param layerHandleCode The layerHandleCode to set.
	 */
	public void setLayerHandleCode(int layerHandleCode) {
		this.layerHandleCode = layerHandleCode;
	}
    /**
     * @return Returns the color.
     */
    public int getColor() {
        return color;
    }
    /**
     * @param color The color to set.
     */
    public void setColor(int color) {
        this.color = color;
    }
    /**
     * @return Returns the handle.
     */
    public int getHandle() {
        return handle;
    }
    /**
     * @param handle The handle to set.
     */
    public void setHandle(int handle) {
        this.handle = handle;
    }
    /**
     * @return Returns the layerHandle.
     */
    public int getLayerHandle() {
        return layerHandle;
    }
    /**
     * @param layerHandle The layerHandle to set.
     */
    public void setLayerHandle(int layerHandle) {
        this.layerHandle = layerHandle;
    }
    /**
     * @return Returns the mode.
     */
    public int getMode() {
        return mode;
    }
    /**
     * @param mode The mode to set.
     */
    public void setMode(int mode) {
        this.mode = mode;
    }
    /**
     * @return Returns the noLinks.
     */
    public boolean isNoLinks() {
        return noLinks;
    }
    /**
     * @param noLinks The noLinks to set.
     */
    public void setNoLinks(boolean noLinks) {
        this.noLinks = noLinks;
    }
    /**
     * @return Returns the numReactors.
     */
    public int getNumReactors() {
        return numReactors;
    }
    /**
     * @param numReactors The numReactors to set.
     */
    public void setNumReactors(int numReactors) {
        this.numReactors = numReactors;
    }
    /**
     * @return Returns the type.
     */
    public int getType() {
        return type;
    }
    /**
     * @param type The type to set.
     */
    public void setType(int type) {
        this.type = type;
    }
    /**
     * @return Returns the linetypeFlags.
     */
    public int getLinetypeFlags() {
        return linetypeFlags;
    }
    /**
     * @return Returns the plotstyleFlags.
     */
    public int getPlotstyleFlags() {
        return plotstyleFlags;
    }
    /**
     * @param version The version to set.
     */
    public void setVersion(String version) {
        this.version = version;
    }
    /**
     * @return Returns the graphicsFlag.
     */
    public boolean isGraphicsFlag() {
        return graphicsFlag;
    }
    /**
     * @param graphicsFlag The graphicsFlag to set.
     */
    public void setGraphicsFlag(boolean graphicsFlag) {
        this.graphicsFlag = graphicsFlag;
    }
}
