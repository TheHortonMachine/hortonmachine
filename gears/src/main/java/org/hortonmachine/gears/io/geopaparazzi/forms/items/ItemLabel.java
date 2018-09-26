/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gears.io.geopaparazzi.forms.items;

/**
 * A label item.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ItemLabel implements Item{

    public static final String TYPE = "label";
    public static final String TYPE_WITHLINE = "labelwithline";
    private String description;
    private int size;
    private boolean doLine;

    public ItemLabel( String description, int size, boolean doLine ) {
        this.size = size;
        this.doLine = doLine;
        this.description = description;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("        {\n");
        // sb.append("             \"key\": \"").append(description).append("\",\n");
        sb.append("             \"value\": \"").append(description).append("\",\n");
        sb.append("             \"size\": \"").append(size).append("\",\n");
        sb.append("             \"type\": \"").append(doLine ? TYPE_WITHLINE : TYPE).append("\"\n");
        sb.append("        }\n");
        return sb.toString();
    }
    
    @Override
    public String getKey() {
        return null;
    }

    @Override
    public void setValue( String value ) {
    }
    
    @Override
    public String getValue() {
        return null;
    }
}
