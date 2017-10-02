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
 * The DwgLayer class represents a DWG Layer
 * 
 * @author jmorell
 */
public class DwgLayer extends DwgObject {
	private String name;
	private boolean flag64;
	private int xRefPlus;
	private boolean xdep;
	private int flags;
	private int color;
	private int layerControlHandle;
	private int nullHandle;
	private int plotstyleHandle;
	private int linetypeHandle;
	
	/**
	 * Read a Layer in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgLayerV15(int[] data, int offset) throws Exception {
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
		v = DwgUtil.getBitShort(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int flags = ((Integer)v.get(1)).intValue();
		this.flags = flags;
		v = DwgUtil.getBitShort(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int color = ((Integer)v.get(1)).intValue();
		this.color = color;
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
	    layerControlHandle = DwgUtil.handleBinToHandleInt(handleVect);
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
	    plotstyleHandle = DwgUtil.handleBinToHandleInt(handleVect);
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
	    linetypeHandle = DwgUtil.handleBinToHandleInt(handleVect);
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
}
