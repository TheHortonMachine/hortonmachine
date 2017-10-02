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
 * The DwgMText class represents a DWG MText
 * 
 * @author jmorell
 */
public class DwgMText extends DwgObject {
	private double[] insertionPoint;
	private double[] extrusion;
	private double[] xAxisDirection;
	private double width;
	private double height;
	private int attachment;
	private int drawingDir;
	private double extHeight;
	private double extWidth;
	private String text;
	private int lineSpacingStyle;
	private double lineSpacingFactor;
	private int styleHandle;
	
	/**
	 * Read a MText in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgMTextV15(int[] data, int offset) throws Exception {
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
		x = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		y = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		z = ((Double)v.get(1)).doubleValue();
		coord = new double[]{x, y, z};
		xAxisDirection = coord;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double val = ((Double)v.get(1)).doubleValue();
		width = val;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		val = ((Double)v.get(1)).doubleValue();
		height = val;
		v = DwgUtil.getBitShort(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int ival = ((Integer)v.get(1)).intValue();
		attachment = ival;
		v = DwgUtil.getBitShort(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		ival = ((Integer)v.get(1)).intValue();
		drawingDir = ival;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		val = ((Double)v.get(1)).doubleValue();
		extHeight = val;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		val = ((Double)v.get(1)).doubleValue();
		extWidth = val;
		v = DwgUtil.getTextString(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		String text = (String)v.get(1);
		this.text = text;
		v = DwgUtil.getBitShort(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		ival = ((Integer)v.get(1)).intValue();
		lineSpacingStyle = ival;
		v = DwgUtil.getBitDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		val = ((Double)v.get(1)).doubleValue();
		lineSpacingFactor = val;
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		boolean flag = ((Boolean)v.get(1)).booleanValue();
		bitPos = readObjectTailV15(data, bitPos);
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
	    styleHandle = DwgUtil.handleBinToHandleInt(handleVect);
	}
	/**
	 * @return Returns the height.
	 */
	public double getHeight() {
		return height;
	}
	/**
	 * @param height The height to set.
	 */
	public void setHeight(double height) {
		this.height = height;
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
	 * @return Returns the width.
	 */
	public double getWidth() {
		return width;
	}
	/**
	 * @param width The width to set.
	 */
	public void setWidth(double width) {
		this.width = width;
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
		DwgMText dwgMText = new DwgMText();
		dwgMText.setType(type);
		dwgMText.setHandle(handle);
		dwgMText.setVersion(version);
		dwgMText.setMode(mode);
		dwgMText.setLayerHandle(layerHandle);
		dwgMText.setColor(color);
		dwgMText.setNumReactors(numReactors);
		dwgMText.setNoLinks(noLinks);
		dwgMText.setLinetypeFlags(linetypeFlags);
		dwgMText.setPlotstyleFlags(plotstyleFlags);
		dwgMText.setSizeInBits(sizeInBits);
		dwgMText.setExtendedData(extendedData);
		dwgMText.setGraphicData(graphicData);
		//dwgMText.setInsideBlock(insideBlock);
		dwgMText.setInsertionPoint(insertionPoint);
		dwgMText.setXAxisDirection(xAxisDirection);
		dwgMText.setExtrusion(extrusion);
		dwgMText.setWidth(width);
		dwgMText.setHeight(height);
		dwgMText.setAttachment(attachment);
		dwgMText.setDrawingDir(drawingDir);
		dwgMText.setExtHeight(extHeight);
		dwgMText.setExtWidth(extWidth);
		dwgMText.setText(text);
		dwgMText.setLineSpacingStyle(lineSpacingStyle);
		dwgMText.setLineSpacingFactor(lineSpacingFactor);
		dwgMText.setStyleHandle(styleHandle);
		return dwgMText;
	}
	/**
	 * @return Returns the attachment.
	 */
	public int getAttachment() {
		return attachment;
	}
	/**
	 * @param attachment The attachment to set.
	 */
	public void setAttachment(int attachment) {
		this.attachment = attachment;
	}
	/**
	 * @return Returns the drawingDir.
	 */
	public int getDrawingDir() {
		return drawingDir;
	}
	/**
	 * @param drawingDir The drawingDir to set.
	 */
	public void setDrawingDir(int drawingDir) {
		this.drawingDir = drawingDir;
	}
	/**
	 * @return Returns the extHeight.
	 */
	public double getExtHeight() {
		return extHeight;
	}
	/**
	 * @param extHeight The extHeight to set.
	 */
	public void setExtHeight(double extHeight) {
		this.extHeight = extHeight;
	}
	/**
	 * @return Returns the extWidth.
	 */
	public double getExtWidth() {
		return extWidth;
	}
	/**
	 * @param extWidth The extWidth to set.
	 */
	public void setExtWidth(double extWidth) {
		this.extWidth = extWidth;
	}
	/**
	 * @return Returns the lineSpacingFactor.
	 */
	public double getLineSpacingFactor() {
		return lineSpacingFactor;
	}
	/**
	 * @param lineSpacingFactor The lineSpacingFactor to set.
	 */
	public void setLineSpacingFactor(double lineSpacingFactor) {
		this.lineSpacingFactor = lineSpacingFactor;
	}
	/**
	 * @return Returns the lineSpacingStyle.
	 */
	public int getLineSpacingStyle() {
		return lineSpacingStyle;
	}
	/**
	 * @param lineSpacingStyle The lineSpacingStyle to set.
	 */
	public void setLineSpacingStyle(int lineSpacingStyle) {
		this.lineSpacingStyle = lineSpacingStyle;
	}
	/**
	 * @return Returns the styleHandle.
	 */
	public int getStyleHandle() {
		return styleHandle;
	}
	/**
	 * @param styleHandle The styleHandle to set.
	 */
	public void setStyleHandle(int styleHandle) {
		this.styleHandle = styleHandle;
	}
	/**
	 * @return Returns the xAxisDirection.
	 */
	public double[] getXAxisDirection() {
		return xAxisDirection;
	}
	/**
	 * @param axisDirection The xAxisDirection to set.
	 */
	public void setXAxisDirection(double[] axisDirection) {
		xAxisDirection = axisDirection;
	}
	/**
	 * @param extrusion The extrusion to set.
	 */
	public void setExtrusion(double[] extrusion) {
		this.extrusion = extrusion;
	}
}
