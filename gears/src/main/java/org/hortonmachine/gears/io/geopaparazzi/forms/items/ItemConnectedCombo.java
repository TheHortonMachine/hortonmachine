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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.hortonmachine.gears.utils.StringUtilities;

import java.util.Set;

/**
 * A connected combos item.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ItemConnectedCombo implements Item {

    public static final String TYPE = "connectedstringcombo";
    private String description;
    private boolean isMandatory;
    private String defaultValue;
    private LinkedHashMap<String, List<String>> dataMap;
    private String key;

    public ItemConnectedCombo( String key, String description, LinkedHashMap<String, List<String>> dataMap,
            String[] defaultValues, boolean isMandatory ) {
        this.key = key;
        this.dataMap = dataMap;
        if (defaultValues == null) {
            defaultValue = "";
        } else {
            defaultValue = StringUtilities.joinStrings("#", defaultValues);
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
        } else {
            sb.append("             \"key\": \"").append(description).append("\",\n");
        }
        sb.append("             \"values\": {\n");

        StringBuilder tmp = new StringBuilder();
        Set<Entry<String, List<String>>> entrySet = dataMap.entrySet();
        for( Entry<String, List<String>> entry : entrySet ) {
            String itemName = entry.getKey();
            List<String> items = entry.getValue();

            tmp.append("                 \"" + itemName + "\": [\n");
            StringBuilder tmp2 = new StringBuilder();
            for( String itemString : items ) {
                tmp2.append("                     {\"item\": \"" + itemString + "\"},\n");
            }
            String substring = removeLastComma(tmp2);
            tmp.append(substring).append("\n");
            tmp.append("                 ],\n");
        }
        String substring = removeLastComma(tmp);
        sb.append(substring).append("\n");
        sb.append("             },\n");
        sb.append("             \"value\": \"").append(defaultValue).append("\",\n");
        sb.append("             \"type\": \"").append(TYPE).append("\",\n");
        sb.append("             \"mandatory\": \"").append(isMandatory ? "yes" : "no").append("\"\n");
        sb.append("        }\n");
        return sb.toString();
    }

    private String removeLastComma( StringBuilder tmp ) {
        String tmpStr = tmp.toString();
        int lastIndexOf = tmpStr.lastIndexOf(',');
        String substring = tmp.substring(0, lastIndexOf);
        return substring;
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
