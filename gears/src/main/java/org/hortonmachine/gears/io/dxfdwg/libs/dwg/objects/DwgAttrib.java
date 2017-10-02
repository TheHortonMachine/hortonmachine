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
 * The DwgAttrib class represents a DWG Attrib
 * 
 * @author jmorell
 */
public class DwgAttrib extends DwgObject {
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
	private String tag;
	private int fieldLength;
	private int flags;
	private String prompt;
	private int styleHandle;
	
	/**
	 * Read an Attrib in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgAttribV15(int[] data, int offset) throws Exception {
		//System.out.println("readDwgAttdef() executed ...");
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
		double x=0, y=0, z=0;
		if ((dflag & 0x2)==0) {
			v = DwgUtil.getDefaultDouble(data, bitPos, x1);
			bitPos = ((Integer)v.get(0)).intValue();
			x = ((Double)v.get(1)).doubleValue();
			v = DwgUtil.getDefaultDouble(data, bitPos, y1);
			bitPos = ((Integer)v.get(0)).intValue();
			y = ((Double)v.get(1)).doubleValue();
		}
		alignmentPoint = new Point2D.Double(x, y);
		v = DwgUtil.testBit(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		boolean flag = ((Boolean)v.get(1)).booleanValue();
		if (flag) {
			y = 0.0;
			x = y;
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
		extrusion = new double[]{x, y, z};
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
		if ((dflag & 0x4)==0) {
			v = DwgUtil.getRawDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			double oblique = ((Double)v.get(1)).doubleValue();
			obliqueAngle = oblique;
		}
		if ((dflag & 0x8)==0) {
			v = DwgUtil.getRawDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			double rot = ((Double)v.get(1)).doubleValue();
			rotationAngle = rot;
		}
		v = DwgUtil.getRawDouble(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		double height = ((Double)v.get(1)).doubleValue();
		this.height = height;
		if ((dflag & 0x10)==0) {
			v = DwgUtil.getRawDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			double width = ((Double)v.get(1)).doubleValue();
			widthFactor = width;
		}
		v = DwgUtil.getTextString(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		String text = (String)v.get(1);
		this.text = text;
		if ((dflag & 0x20)==0) {
			v = DwgUtil.getBitShort(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			int gen = ((Integer)v.get(1)).intValue();
			generation = gen;
		}
		if ((dflag & 0x40)==0) {
			v = DwgUtil.getBitShort(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			int halign = ((Integer)v.get(1)).intValue();
			this.halign = halign;
		}
		if ((dflag & 0x80)==0) {
			v = DwgUtil.getBitShort(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			int valign = ((Integer)v.get(1)).intValue();
			this.valign = valign;
		}
		v = DwgUtil.getTextString(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		String tag = (String)v.get(1);
		this.tag = tag;
		v = DwgUtil.getBitShort(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int fl = ((Integer)v.get(1)).intValue();
		fieldLength = fl;
		v = DwgUtil.getRawChar(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int flags = ((Integer)v.get(1)).intValue();
		this.flags = flags;
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
     * @return Returns the extrusion.
     */
    public double[] getExtrusion() {
        return extrusion;
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
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		DwgAttrib dwgAttrib = new DwgAttrib();
		dwgAttrib.setType(type);
		dwgAttrib.setHandle(handle);
		dwgAttrib.setVersion(version);
		dwgAttrib.setMode(mode);
		dwgAttrib.setLayerHandle(layerHandle);
		dwgAttrib.setColor(color);
		dwgAttrib.setNumReactors(numReactors);
		dwgAttrib.setNoLinks(noLinks);
		dwgAttrib.setLinetypeFlags(linetypeFlags);
		dwgAttrib.setPlotstyleFlags(plotstyleFlags);
		dwgAttrib.setSizeInBits(sizeInBits);
		dwgAttrib.setExtendedData(extendedData);
		dwgAttrib.setGraphicData(graphicData);
		//dwgAttrib.setInsideBlock(insideBlock);
		dwgAttrib.setDataFlag(dataFlag);
		dwgAttrib.setElevation(elevation);
		dwgAttrib.setInsertionPoint(insertionPoint);
		dwgAttrib.setAlignmentPoint(alignmentPoint);
		dwgAttrib.setExtrusion(extrusion);
		dwgAttrib.setThickness(thickness);
		dwgAttrib.setObliqueAngle(obliqueAngle);
		dwgAttrib.setRotationAngle(rotationAngle);
		dwgAttrib.setHeight(height);
		dwgAttrib.setWidthFactor(widthFactor);
		dwgAttrib.setText(text);
		dwgAttrib.setGeneration(generation);
		dwgAttrib.setHalign(halign);
		dwgAttrib.setValign(valign);
		dwgAttrib.setTag(tag);
		dwgAttrib.setFieldLength(fieldLength);
		dwgAttrib.setFlags(flags);
		dwgAttrib.setPrompt(prompt);
		dwgAttrib.setStyleHandle(styleHandle);
		return dwgAttrib;
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
	 * @return Returns the fieldLength.
	 */
	public int getFieldLength() {
		return fieldLength;
	}
	/**
	 * @param fieldLength The fieldLength to set.
	 */
	public void setFieldLength(int fieldLength) {
		this.fieldLength = fieldLength;
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
	 * @return Returns the prompt.
	 */
	public String getPrompt() {
		return prompt;
	}
	/**
	 * @param prompt The prompt to set.
	 */
	public void setPrompt(String prompt) {
		this.prompt = prompt;
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
	 * @return Returns the tag.
	 */
	public String getTag() {
		return tag;
	}
	/**
	 * @param tag The tag to set.
	 */
	public void setTag(String tag) {
		this.tag = tag;
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
