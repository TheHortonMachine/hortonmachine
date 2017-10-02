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
 * The DwgBlockControl class represents a DWG Block control
 * 
 * @author jmorell
 */
public class DwgBlockControl extends DwgObject {
	private int nullHandle;
	private Vector code2Handles;
	private int modelSpaceHandle;
	private int paperSpaceHandle;
	
	/**
	 * Read a Block control in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgBlockControlV15(int[] data, int offset) throws Exception {
		//System.out.println("readDwgBlockControl() executed ...");
		int bitPos = offset;
		Vector v = DwgUtil.getBitLong(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int numReactors = ((Integer)v.get(1)).intValue();
		setNumReactors(numReactors);
		v = DwgUtil.getBitShort(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int enumsz = ((Integer)v.get(1)).intValue();
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
	    nullHandle = DwgUtil.handleBinToHandleInt(handleVect);
		v = DwgUtil.getHandle(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		handle = new int[v.size()-1];
	    for (int i=1;i<v.size();i++) {
		    handle[i-1] = ((Integer)v.get(i)).intValue();
	    }
		if (enumsz>0) {
			Vector handles = new Vector();
			for (int i=0;i<enumsz;i++) {
				v = DwgUtil.getHandle(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				handle = new int[v.size()-1];
			    for (int j=1;j<v.size();j++) {
				    handle[j-1] = ((Integer)v.get(j)).intValue();
			    }
				handles.add(handle);
			}
			code2Handles = handles;
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
	    modelSpaceHandle = DwgUtil.handleBinToHandleInt(handleVect);
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
	    paperSpaceHandle = DwgUtil.handleBinToHandleInt(handleVect);
	}
}
