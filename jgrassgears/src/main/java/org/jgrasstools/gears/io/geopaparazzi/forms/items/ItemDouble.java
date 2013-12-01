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
 * A double item.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ItemDouble implements Item {

    private String description;
    private boolean isMandatory;
    private String defaultValueStr;
    private boolean isLabel;

    public ItemDouble( String description, Double defaultValue, boolean isMandatory, boolean isLabel ) {
        this.isLabel = isLabel;
        if (defaultValue == null) {
            defaultValueStr = "";
        } else {
            this.defaultValueStr = defaultValue.toString();
        }
        this.description = description;
        this.isMandatory = isMandatory;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("        {\n");
        sb.append("             \"key\": \"").append(description).append("\",\n");
        sb.append("             \"value\": \"").append(defaultValueStr).append("\",\n");
        if (isLabel)
            sb.append("             \"islabel\": \"").append("true").append("\",\n");
        sb.append("             \"type\": \"").append("double").append("\",\n");
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
        defaultValueStr = value;
    }

    @Override
    public String getValue() {
        return defaultValueStr;
    }
}
