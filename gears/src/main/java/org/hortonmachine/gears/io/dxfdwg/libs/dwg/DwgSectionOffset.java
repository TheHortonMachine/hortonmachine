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
 * The DwgSectionOffset class is useful to store the key of a DWG section with its seek
 * (or offset) and with its size
 * 
 * @author jmorell
 */
public class DwgSectionOffset {
	private String key;
	private int seek;
	private int size;
	
	/**
	 * Creates a new DwgSectionOffset object
	 * 
	 * @param key Section key
	 * @param seek Seeker or offset in the DWG file for this section
	 * @param size Size of this section
	 */
	public DwgSectionOffset(String key, int seek, int size) {
		this.key = key;
		this.seek = seek;
		this.size = size;
	}
    /**
     * @return Returns the key.
     */
    public String getKey() {
        return key;
    }
    /**
     * @param key The key to set.
     */
    public void setKey(String key) {
        this.key = key;
    }
    /**
     * @return Returns the seek.
     */
    public int getSeek() {
        return seek;
    }
    /**
     * @param seek The seek to set.
     */
    public void setSeek(int seek) {
        this.seek = seek;
    }
}
