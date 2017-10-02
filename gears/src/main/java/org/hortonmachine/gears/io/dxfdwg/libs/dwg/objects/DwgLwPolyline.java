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
 * The DwgLwPolyline class represents a DWG LwPolyline
 * 
 * @author jmorell
 */
public class DwgLwPolyline extends DwgObject {
	private int flag;
	private double constWidth;
	private double elevation;
	private double thickness;
	private double[] normal;
	private Point2D[] vertices;
	private double[] bulges;
	private double[][] widths;
	
	/**
	 * Read a LwPolyline in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgLwPolylineV15(int[] data, int offset) throws Exception {
		//System.out.println("DwgLwPolyline.readDwgLwPolyline() executed ...");
		int bitPos = offset;
		bitPos = readObjectHeaderV15(data, bitPos);
		Vector v = DwgUtil.getBitShort(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int flag = ((Integer)v.get(1)).intValue();
		this.flag = flag;
		// Condici�n emp�rica. Si flag es menor que cero no se trata de LwPolylines ...
		if (flag>=0) {
			double constWidth = 0.0;
			if ((flag & 0x4)>0) {
				v = DwgUtil.getBitDouble(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				constWidth = ((Double)v.get(1)).doubleValue();
			}
			this.constWidth = constWidth;
			double elev = 0.0;
			if ((flag & 0x8)>0) {
				v = DwgUtil.getBitDouble(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				elev = ((Double)v.get(1)).doubleValue();
			}
			elevation = elev;
			double thickness = 0.0;
			if ((flag & 0x2)>0) {
				v = DwgUtil.getBitDouble(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				thickness = ((Double)v.get(1)).doubleValue();
			}
			this.thickness = thickness;
			double nx = 0.0, ny = 0.0, nz = 0.0;
			if ((flag & 0x1)>0) {
				v = DwgUtil.getBitDouble(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				nx = ((Double)v.get(1)).doubleValue();
				v = DwgUtil.getBitDouble(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				ny = ((Double)v.get(1)).doubleValue();
				v = DwgUtil.getBitDouble(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				nz = ((Double)v.get(1)).doubleValue();
			}
			normal = new double[]{nx, ny, nz};
			v = DwgUtil.getBitLong(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			int np = ((Integer)v.get(1)).intValue();
			// TODO: Condici�n emp�rica. Si hay m�s de 10000 puntos no se trata de LwPolylines.
		    // Este tema hay que revisarlo porque si pueden existir LwPolylines con m�s de
		    // 10000 v�rtices ...
            // Se han encontrado lwplines con np = 0. Estas lwplines tambin ser ignoradas.
			if (np>0 && np<10000) {
				long nb = 0;
				if ((flag & 0x10)>0) {
					v = DwgUtil.getBitLong(data, bitPos);
					bitPos = ((Integer)v.get(0)).intValue();
					nb = ((Integer)v.get(1)).intValue();
				}
				long nw = 0;
				if ((flag & 0x20)>0) {
					v = DwgUtil.getBitLong(data, bitPos);
					bitPos = ((Integer)v.get(0)).intValue();
					nw = ((Integer)v.get(1)).intValue();
				}
                //System.out.println("np = " + np);
				Point2D[] vertices = new Point2D[np];
				v = DwgUtil.getRawDouble(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				double vx = ((Double)v.get(1)).doubleValue();
				v = DwgUtil.getRawDouble(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				double vy = ((Double)v.get(1)).doubleValue();
				vertices[0] = new Point2D.Double(vx, vy);
				for (int i=1; i<(np); i++) {
					v = DwgUtil.getDefaultDouble(data, bitPos, vx);
					bitPos = ((Integer)v.get(0)).intValue();
					double x = ((Double)v.get(1)).doubleValue();
					v = DwgUtil.getDefaultDouble(data, bitPos, vy);
					bitPos = ((Integer)v.get(0)).intValue();
					double y = ((Double)v.get(1)).doubleValue();
					vertices[i] = new Point2D.Double(x, y);
					vx = x;
					vy = y;
				}
				this.vertices = vertices;
				double[] bulges = new double[0];
				if (nb>0) {
					bulges = new double[(int)nb];
					for (int i=0; i<nb; i++) {
						v = DwgUtil.getRawDouble(data, bitPos);
						bitPos = ((Integer)v.get(0)).intValue();
						double bulge = ((Double)v.get(1)).doubleValue();
						bulges[i] = bulge;
					}
				} else if (nb==0) {
					bulges = new double[(int)np];
					for (int i=0;i<(int)np; i++) {
					    bulges[i] = 0;
					}
				}
				this.bulges = bulges;
				if (nw>0) {
					double[][] widths = new double[(int)nw][2];
					for (int i=0; i<nw; i++) {
						v = DwgUtil.getBitDouble(data, bitPos);
						bitPos = ((Integer)v.get(0)).intValue();
						double sw = ((Double)v.get(1)).doubleValue();
						v = DwgUtil.getBitDouble(data, bitPos);
						bitPos = ((Integer)v.get(0)).intValue();
						double ew = ((Double)v.get(1)).doubleValue();
						widths[i][0] = sw;
						widths[i][1] = ew;
					}
					this.widths = widths;
				}
				bitPos = readObjectTailV15(data, bitPos);
			}
		}
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
	 * @return Returns the flag.
	 */
	public int getFlag() {
		return flag;
	}
	/**
	 * @param flag The flag to set.
	 */
	public void setFlag(int flag) {
		this.flag = flag;
	}
	/**
	 * @return Returns the vertices.
	 */
	public Point2D[] getVertices() {
		return vertices;
	}
	/**
	 * @param vertices The vertices to set.
	 */
	public void setVertices(Point2D[] vertices) {
		this.vertices = vertices;
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
     * @return Returns the normal.
     */
    public double[] getNormal() {
        return normal;
    }
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		DwgLwPolyline dwgLwPolyline = new DwgLwPolyline();
		dwgLwPolyline.setType(type);
		dwgLwPolyline.setHandle(handle);
		dwgLwPolyline.setVersion(version);
		dwgLwPolyline.setMode(mode);
		dwgLwPolyline.setLayerHandle(layerHandle);
		dwgLwPolyline.setColor(color);
		dwgLwPolyline.setNumReactors(numReactors);
		dwgLwPolyline.setNoLinks(noLinks);
		dwgLwPolyline.setLinetypeFlags(linetypeFlags);
		dwgLwPolyline.setPlotstyleFlags(plotstyleFlags);
		dwgLwPolyline.setSizeInBits(sizeInBits);
		dwgLwPolyline.setExtendedData(extendedData);
		dwgLwPolyline.setGraphicData(graphicData);
		//dwgLwPolyline.setInsideBlock(insideBlock);
		dwgLwPolyline.setFlag(flag);
		dwgLwPolyline.setElevation(elevation);
		dwgLwPolyline.setConstWidth(constWidth);
		dwgLwPolyline.setThickness(thickness);
		dwgLwPolyline.setNormal(normal);
		dwgLwPolyline.setVertices(vertices);
		dwgLwPolyline.setBulges(bulges);
		dwgLwPolyline.setWidths(widths);
		return dwgLwPolyline;
	}
	/**
	 * @return Returns the constWidth.
	 */
	public double getConstWidth() {
		return constWidth;
	}
	/**
	 * @param constWidth The constWidth to set.
	 */
	public void setConstWidth(double constWidth) {
		this.constWidth = constWidth;
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
	 * @return Returns the widths.
	 */
	public double[][] getWidths() {
		return widths;
	}
	/**
	 * @param widths The widths to set.
	 */
	public void setWidths(double[][] widths) {
		this.widths = widths;
	}
	/**
	 * @param normal The normal to set.
	 */
	public void setNormal(double[] normal) {
		this.normal = normal;
	}
}
