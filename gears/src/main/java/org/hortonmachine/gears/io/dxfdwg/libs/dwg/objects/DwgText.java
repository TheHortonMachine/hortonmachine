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
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.utils.TextToUnicodeConverter;

/**
 * The DwgText class represents a DWG Text
 * 
 * @author jmorell
 */
public class DwgText extends DwgObject {
	private int dataFlag;
	private double elevation;
	private Point2D insertionPoint;
	private Point2D alignmentPoint;
	private double[] extrusion;
	private double thickness;
	private double obliqueAngle;
	private double rotationAngle;
	private double height;
	private double widthFactor;
	private String text;
	private int generation;
	private int halign;
	private int valign;
	
	/**
	 * Read a Text in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgTextV15(int[] data, int offset) throws Exception {
		int bitPos = offset;
		bitPos = readObjectHeaderV15(data, bitPos);
		Vector v = DwgUtil.getRawChar(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int dflag = ((Integer)v.get(1)).intValue();
		dataFlag = dflag;
		if ((dflag & 0x1)==0) {
			v = DwgUtil.getRawDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			double elev = ((Double)v.get(1)).doubleValue();
			elevation = elev;
		}
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double x1 = ((Double)v.get(1)).doubleValue();
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double y1 = ((Double)v.get(1)).doubleValue();
		insertionPoint = new Point2D.Double(x1, y1);
		if ((dflag & 0x2)==0) {
			v = DwgUtil.getDefaultDouble(data, bitPos, x1);
			bitPos = ((Integer)v.get(0)).intValue();
			double xa = ((Double)v.get(1)).doubleValue();
			v = DwgUtil.getDefaultDouble(data, bitPos, y1);
			bitPos = ((Integer)v.get(0)).intValue();
			double ya = ((Double)v.get(1)).doubleValue();
			alignmentPoint = new Point2D.Double(xa, ya);
		}
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		boolean flag = ((Boolean)v.get(1)).booleanValue();
		double x, y, z;
		if (flag) {
			x = 0.0;
			y = 0.0;
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
		double[] coord = new double[]{x, y, z};
		extrusion = coord;
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		flag = ((Boolean)v.get(1)).booleanValue();
	    double th;
		if (flag) {
			th=0.0;
		} else {
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			th = ((Double)v.get(1)).doubleValue();
		}
		thickness = th;
		if ((dflag & 0x4) == 0) {
			v = DwgUtil.getRawDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			double oblique = ((Double)v.get(1)).doubleValue();
			obliqueAngle = oblique;
		}
		if ((dflag & 0x8) == 0) {
			v = DwgUtil.getRawDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			double rot = ((Double)v.get(1)).doubleValue();
			rotationAngle = rot;
		}
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double height = ((Double)v.get(1)).doubleValue();
		this.height = height;
		if ((dflag & 0x10) == 0) {
			v = DwgUtil.getRawDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			double width = ((Double)v.get(1)).doubleValue();
			widthFactor = width;
		}
		v = DwgUtil.getTextString(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		String text = (String)v.get(1);
		text = TextToUnicodeConverter.convertText(text);
		this.text = text;
		if ((dflag & 0x20) == 0) {
			v = DwgUtil.getBitShort(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			int gen = ((Integer)v.get(1)).intValue();
		    generation = gen;
		}
		if ((dflag & 0x40) == 0) {
			v = DwgUtil.getBitShort(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			int halign = ((Integer)v.get(1)).intValue();
		    this.halign = halign;
		}
		if ((dflag & 0x80) == 0) {
			v = DwgUtil.getBitShort(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			int valign = ((Integer)v.get(1)).intValue();
		    this.valign = valign;
		}
		bitPos = readObjectTailV15(data, bitPos);
	}
	/**
	 * @return Returns the dataFlag.
	 */
	public int getDataFlag() {
		return dataFlag;
	}
	/**
	 * @param dataFlag The dataFlag to set.
	 */
	public void setDataFlag(int dataFlag) {
		this.dataFlag = dataFlag;
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
	public Point2D getInsertionPoint() {
		return insertionPoint;
	}
	/**
	 * @param insertionPoint The insertionPoint to set.
	 */
	public void setInsertionPoint(Point2D insertionPoint) {
		this.insertionPoint = insertionPoint;
	}
	/**
	 * @return Returns the rotationAngle.
	 */
	public double getRotationAngle() {
		return rotationAngle;
	}
	/**
	 * @param rotationAngle The rotationAngle to set.
	 */
	public void setRotationAngle(double rotationAngle) {
		this.rotationAngle = rotationAngle;
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
     * @return Returns the extrusion.
     */
    public double[] getExtrusion() {
        return extrusion;
    }
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		DwgText dwgText = new DwgText();
		dwgText.setType(type);
		dwgText.setHandle(handle);
		dwgText.setVersion(version);
		dwgText.setMode(mode);
		dwgText.setLayerHandle(layerHandle);
		dwgText.setColor(color);
		dwgText.setNumReactors(numReactors);
		dwgText.setNoLinks(noLinks);
		dwgText.setLinetypeFlags(linetypeFlags);
		dwgText.setPlotstyleFlags(plotstyleFlags);
		dwgText.setSizeInBits(sizeInBits);
		dwgText.setExtendedData(extendedData);
		dwgText.setGraphicData(graphicData);
		//dwgText.setInsideBlock(insideBlock);
		dwgText.setDataFlag(dataFlag);
		dwgText.setElevation(elevation);
		dwgText.setInsertionPoint(insertionPoint);
		dwgText.setAlignmentPoint(alignmentPoint);
		dwgText.setExtrusion(extrusion);
		dwgText.setThickness(thickness);
		dwgText.setObliqueAngle(obliqueAngle);
		dwgText.setRotationAngle(rotationAngle);
		dwgText.setHeight(height);
		dwgText.setWidthFactor(widthFactor);
		dwgText.setText(text);
		dwgText.setGeneration(generation);
		dwgText.setHalign(halign);
		dwgText.setValign(valign);
		return dwgText;
	}
	/**
	 * @return Returns the alignmentPoint.
	 */
	public Point2D getAlignmentPoint() {
		return alignmentPoint;
	}
	/**
	 * @param alignmentPoint The alignmentPoint to set.
	 */
	public void setAlignmentPoint(Point2D alignmentPoint) {
		this.alignmentPoint = alignmentPoint;
	}
	/**
	 * @return Returns the generation.
	 */
	public int getGeneration() {
		return generation;
	}
	/**
	 * @param generation The generation to set.
	 */
	public void setGeneration(int generation) {
		this.generation = generation;
	}
	/**
	 * @return Returns the halign.
	 */
	public int getHalign() {
		return halign;
	}
	/**
	 * @param halign The halign to set.
	 */
	public void setHalign(int halign) {
		this.halign = halign;
	}
	/**
	 * @return Returns the obliqueAngle.
	 */
	public double getObliqueAngle() {
		return obliqueAngle;
	}
	/**
	 * @param obliqueAngle The obliqueAngle to set.
	 */
	public void setObliqueAngle(double obliqueAngle) {
		this.obliqueAngle = obliqueAngle;
	}
	/**
	 * @return Returns the valign.
	 */
	public int getValign() {
		return valign;
	}
	/**
	 * @param valign The valign to set.
	 */
	public void setValign(int valign) {
		this.valign = valign;
	}
	/**
	 * @return Returns the widthFactor.
	 */
	public double getWidthFactor() {
		return widthFactor;
	}
	/**
	 * @param widthFactor The widthFactor to set.
	 */
	public void setWidthFactor(double widthFactor) {
		this.widthFactor = widthFactor;
	}
	/**
	 * @param extrusion The extrusion to set.
	 */
	public void setExtrusion(double[] extrusion) {
		this.extrusion = extrusion;
	}
}
