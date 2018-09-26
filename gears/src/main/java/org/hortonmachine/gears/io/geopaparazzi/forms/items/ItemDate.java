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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A date item.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ItemDate implements Item {

    public static final String TYPE = "date";
    private String description;
    private boolean isMandatory;
    private String defaultValueStr;

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    private String key;

    public ItemDate( String key, String description, Date defaultValue, boolean isMandatory ) {
        this.key = key;
        if (defaultValue == null) {
            defaultValueStr = "";
        } else {
            this.defaultValueStr = dateFormatter.format(defaultValue);
        }
        this.description = description;
        this.isMandatory = isMandatory;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("        {\n");
        if (key != null && key.trim().length() > 0) {
            sb.append("             \"key\": \"").append(key).append("\",\n");
            sb.append("             \"label\": \"").append(description).append("\",\n");
        }else{
            sb.append("             \"key\": \"").append(description).append("\",\n");
        }
        sb.append("             \"value\": \"").append(defaultValueStr).append("\",\n");
        sb.append("             \"type\": \"").append(TYPE).append("\",\n");
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
