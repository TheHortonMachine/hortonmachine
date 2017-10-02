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

/**
 * The DwgObjectOffset class is useful to store the handle of an object with its
 * offset in the DWG file
 * 
 * @author jmorell
 */
public class DwgObjectOffset {
	private int handle;
	private int offset;
	
	/**
	 * Create a new DwgObjectOffset object
	 * 
	 * @param handle Handle of the object
	 * @param offset Offset in the DWG file of the object
	 */
	public DwgObjectOffset(int handle, int offset) {
		this.handle = handle;
		this.offset = offset;
	}
    /**
     * @return Returns the handle.
     */
    public int getHandle() {
        return handle;
    }
    /**
     * @return Returns the offset.
     */
    public int getOffset() {
        return offset;
    }
}
