/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.io.geopaparazzi.forms.items;

/**
 * A combo item.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ItemCombo implements Item{

    private String description;
    private boolean isMandatory;
    private String defaultValue;
    private String[] items;

    public ItemCombo( String description, String[] items, String defaultValue, boolean isMandatory ) {
        this.items = items;
        if (defaultValue == null) {
            defaultValue = "";
        }
        this.description = description;
        this.defaultValue = defaultValue;
        this.isMandatory = isMandatory;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("        {\n");
        sb.append("             \"key\": \"").append(description).append("\",\n");
        sb.append("             \"values\": {\n");
        sb.append("                 \"items\": [\n");
        sb.append("                     {\"item\": \"\"},\n");
        StringBuilder tmp = new StringBuilder();
        for( String itemString : items ) {
            tmp.append("                     {\"item\": \"" + itemString + "\"},\n");
        }
        String tmpStr = tmp.toString();
        int lastIndexOf = tmpStr.lastIndexOf(',');
        String substring = tmp.substring(0, lastIndexOf);
        sb.append(substring);
        sb.append("                 ]\n");
        sb.append("             },\n");
        sb.append("             \"value\": \"").append(defaultValue).append("\",\n");
        sb.append("             \"type\": \"").append("stringcombo").append("\",\n");
        sb.append("             \"mandatory\": \"").append(isMandatory ? "yes" : "no").append("\"\n");
        sb.append("        }\n");
        return sb.toString();
    }
    
    @Override
    public String getKey() {
        return description;
    }

    @Override
    public void setValue( String value ) {
        defaultValue = value;
    }
    
    @Override
    public String getValue() {
        return defaultValue;
    }
}
