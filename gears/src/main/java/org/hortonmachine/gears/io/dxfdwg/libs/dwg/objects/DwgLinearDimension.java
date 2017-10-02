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
 * The DwgLinearDimension class represents a DWG Linear dimension
 * 
 * @author jmorell
 */
public class DwgLinearDimension extends DwgObject {
	private double[] extrusion;
	private Point2D textMidpoint;
	private double elevation;
	private int flags;
	private String text;
	private double rotation;
	private double horizDir;
	private double[] insScale;
	private double insRotation;
	private int attachmentPoint;
	private int linespaceStyle;
	private double linespaceFactor;
	private double actualMeasurement;
	private Point2D pt12;
	private double[] pt10;
	private double[] pt13;
	private double[] pt14;
	private double extRotation;
	private double dimensionRotation;
	private int dimstyleHandle;
	private int anonBlockHandle;
	
	/**
	 * Read a Linear dimension in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgLinearDimensionV15(int[] data, int offset) throws Exception {
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
		extrusion = new double[]{x, y, z};
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		x = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		y = ((Double)v.get(1)).doubleValue();
		textMidpoint = new Point2D.Double(x, y);
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double val = ((Double)v.get(1)).doubleValue();
		elevation = val;
		v = DwgUtil.getRawChar(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int flags = ((Integer)v.get(1)).intValue();
		this.flags = flags;
		v = DwgUtil.getTextString(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		String text = (String)v.get(1);
		this.text = text;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		val = ((Double)v.get(1)).doubleValue();
		rotation = val;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		val = ((Double)v.get(1)).doubleValue();
		horizDir = val;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		x = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		y = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		z = ((Double)v.get(1)).doubleValue();
		insScale = new double[]{x, y, z};
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		val = ((Double)v.get(1)).doubleValue();
		insRotation = val;
		v = DwgUtil.getBitShort(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int ap = ((Integer)v.get(1)).intValue();
		attachmentPoint = ap;
		v = DwgUtil.getBitShort(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int lss = ((Integer)v.get(1)).intValue();
		linespaceStyle = lss;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		val = ((Double)v.get(1)).doubleValue();
		linespaceFactor = val;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		val = ((Double)v.get(1)).doubleValue();
		actualMeasurement = val;
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		x = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		val = ((Double)v.get(1)).doubleValue();
		pt12 = new Point2D.Double(x, y);
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		x = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		y = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		z = ((Double)v.get(1)).doubleValue();
		pt10 = new double[]{x, y, z};
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		x = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		y = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		z = ((Double)v.get(1)).doubleValue();
		pt13 = new double[]{x, y, z};
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		x = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		y = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		z = ((Double)v.get(1)).doubleValue();
		pt14 = new double[]{x, y, z};
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		val = ((Double)v.get(1)).doubleValue();
		extRotation = val;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		val = ((Double)v.get(1)).doubleValue();
		dimensionRotation = val;
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
	    dimstyleHandle = DwgUtil.handleBinToHandleInt(handleVect);
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
	    anonBlockHandle = DwgUtil.handleBinToHandleInt(handleVect);
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
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		DwgLinearDimension dwgLinearDimension = new DwgLinearDimension();
		dwgLinearDimension.setType(type);
		dwgLinearDimension.setHandle(handle);
		dwgLinearDimension.setVersion(version);
		dwgLinearDimension.setMode(mode);
		dwgLinearDimension.setLayerHandle(layerHandle);
		dwgLinearDimension.setColor(color);
		dwgLinearDimension.setNumReactors(numReactors);
		dwgLinearDimension.setNoLinks(noLinks);
		dwgLinearDimension.setLinetypeFlags(linetypeFlags);
		dwgLinearDimension.setPlotstyleFlags(plotstyleFlags);
		dwgLinearDimension.setSizeInBits(sizeInBits);
		dwgLinearDimension.setExtendedData(extendedData);
		dwgLinearDimension.setGraphicData(graphicData);
		//dwgLinearDimension.setInsideBlock(insideBlock);
		dwgLinearDimension.setTextMidpoint(textMidpoint);
		dwgLinearDimension.setElevation(elevation);
		dwgLinearDimension.setFlags(flags);
		dwgLinearDimension.setText(text);
		dwgLinearDimension.setRotation(rotation);
		dwgLinearDimension.setHorizDir(horizDir);
		dwgLinearDimension.setInsScale(insScale);
		dwgLinearDimension.setInsRotation(insRotation);
		dwgLinearDimension.setAttachmentPoint(attachmentPoint);
		dwgLinearDimension.setLinespaceStyle(linespaceStyle);
		dwgLinearDimension.setLinespaceFactor(linespaceFactor);
		dwgLinearDimension.setActualMeasurement(actualMeasurement);
		dwgLinearDimension.setPt12(pt12);
		dwgLinearDimension.setPt10(pt10);
		dwgLinearDimension.setPt13(pt13);
		dwgLinearDimension.setPt14(pt14);
		dwgLinearDimension.setExtRotation(extRotation);
		dwgLinearDimension.setDimensionRotation(dimensionRotation);
		dwgLinearDimension.setDimstyleHandle(dimstyleHandle);
		dwgLinearDimension.setAnonBlockHandle(anonBlockHandle);
		return dwgLinearDimension;
	}
	/**
	 * @return Returns the actualMeasurement.
	 */
	public double getActualMeasurement() {
		return actualMeasurement;
	}
	/**
	 * @param actualMeasurement The actualMeasurement to set.
	 */
	public void setActualMeasurement(double actualMeasurement) {
		this.actualMeasurement = actualMeasurement;
	}
	/**
	 * @return Returns the anonBlockHandle.
	 */
	public int getAnonBlockHandle() {
		return anonBlockHandle;
	}
	/**
	 * @param anonBlockHandle The anonBlockHandle to set.
	 */
	public void setAnonBlockHandle(int anonBlockHandle) {
		this.anonBlockHandle = anonBlockHandle;
	}
	/**
	 * @return Returns the attachmentPoint.
	 */
	public int getAttachmentPoint() {
		return attachmentPoint;
	}
	/**
	 * @param attachmentPoint The attachmentPoint to set.
	 */
	public void setAttachmentPoint(int attachmentPoint) {
		this.attachmentPoint = attachmentPoint;
	}
	/**
	 * @return Returns the dimensionRotation.
	 */
	public double getDimensionRotation() {
		return dimensionRotation;
	}
	/**
	 * @param dimensionRotation The dimensionRotation to set.
	 */
	public void setDimensionRotation(double dimensionRotation) {
		this.dimensionRotation = dimensionRotation;
	}
	/**
	 * @return Returns the dimstyleHandle.
	 */
	public int getDimstyleHandle() {
		return dimstyleHandle;
	}
	/**
	 * @param dimstyleHandle The dimstyleHandle to set.
	 */
	public void setDimstyleHandle(int dimstyleHandle) {
		this.dimstyleHandle = dimstyleHandle;
	}
	/**
	 * @return Returns the extRotation.
	 */
	public double getExtRotation() {
		return extRotation;
	}
	/**
	 * @param extRotation The extRotation to set.
	 */
	public void setExtRotation(double extRotation) {
		this.extRotation = extRotation;
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
	 * @return Returns the horizDir.
	 */
	public double getHorizDir() {
		return horizDir;
	}
	/**
	 * @param horizDir The horizDir to set.
	 */
	public void setHorizDir(double horizDir) {
		this.horizDir = horizDir;
	}
	/**
	 * @return Returns the insRotation.
	 */
	public double getInsRotation() {
		return insRotation;
	}
	/**
	 * @param insRotation The insRotation to set.
	 */
	public void setInsRotation(double insRotation) {
		this.insRotation = insRotation;
	}
	/**
	 * @return Returns the insScale.
	 */
	public double[] getInsScale() {
		return insScale;
	}
	/**
	 * @param insScale The insScale to set.
	 */
	public void setInsScale(double[] insScale) {
		this.insScale = insScale;
	}
	/**
	 * @return Returns the linespaceFactor.
	 */
	public double getLinespaceFactor() {
		return linespaceFactor;
	}
	/**
	 * @param linespaceFactor The linespaceFactor to set.
	 */
	public void setLinespaceFactor(double linespaceFactor) {
		this.linespaceFactor = linespaceFactor;
	}
	/**
	 * @return Returns the linespaceStyle.
	 */
	public int getLinespaceStyle() {
		return linespaceStyle;
	}
	/**
	 * @param linespaceStyle The linespaceStyle to set.
	 */
	public void setLinespaceStyle(int linespaceStyle) {
		this.linespaceStyle = linespaceStyle;
	}
	/**
	 * @return Returns the pt10.
	 */
	public double[] getPt10() {
		return pt10;
	}
	/**
	 * @param pt10 The pt10 to set.
	 */
	public void setPt10(double[] pt10) {
		this.pt10 = pt10;
	}
	/**
	 * @return Returns the pt12.
	 */
	public Point2D getPt12() {
		return pt12;
	}
	/**
	 * @param pt12 The pt12 to set.
	 */
	public void setPt12(Point2D pt12) {
		this.pt12 = pt12;
	}
	/**
	 * @return Returns the pt13.
	 */
	public double[] getPt13() {
		return pt13;
	}
	/**
	 * @param pt13 The pt13 to set.
	 */
	public void setPt13(double[] pt13) {
		this.pt13 = pt13;
	}
	/**
	 * @return Returns the pt14.
	 */
	public double[] getPt14() {
		return pt14;
	}
	/**
	 * @param pt14 The pt14 to set.
	 */
	public void setPt14(double[] pt14) {
		this.pt14 = pt14;
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
	 * @return Returns the text.
	 */
	public String getText() {
		return text;
	}
	/**
	 * @param text The text to set.
	 */
	public void setText(String text) {
		this.text = text;
	}
	/**
	 * @return Returns the textMidpoint.
	 */
	public Point2D getTextMidpoint() {
		return textMidpoint;
	}
	/**
	 * @param textMidpoint The textMidpoint to set.
	 */
	public void setTextMidpoint(Point2D textMidpoint) {
		this.textMidpoint = textMidpoint;
	}
}
