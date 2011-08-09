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
package org.jgrasstools.gears.utils;

import java.util.List;

/**
 * An utilities class for handling strings and similar.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.0
 */
public class StringUtilities {

    /**
     * Checks if the list of strings supplied contains the supplied string.
     *
     * <p>If the string is contained it changes the name by adding a number.
     * <p>The spaces are trimmed away before performing name equality.
     *
     * @param strings the list of existing strings.
     * @param string the proposed new string, to be changed if colliding.
     * @return the new non-colliding name for the string.
     */
    @SuppressWarnings("nls")
    public static String checkSameName( List<String> strings, String string ) {
        int index = 1;
        for( int i = 0; i < strings.size(); i++ ) {
            if (index == 10000) {
                // something odd is going on
                throw new RuntimeException();
            }
            String existingString = strings.get(i);
            existingString = existingString.trim();
            if (existingString.trim().equals(string.trim())) {
                // name exists, change the name of the entering
                if (string.endsWith(")")) {
                    string = string.trim().replaceFirst("\\([0-9]+\\)$", "(" + (index++) + ")");
                } else {
                    string = string + " (" + (index++) + ")";
                }
                // start again
                i = 0;
            }
        }
        return string;
    }

    /**
     * Join strings through {@link StringBuilder}.
     * 
     * @param separator separator to use or <code>null</code>.
     * @param strings strings to join.
     * @return the joined string.
     */
    public static String joinStrings( String separator, String... strings ) {
        if (separator == null) {
            separator = "";
        }
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < strings.length; i++ ) {
            sb.append(strings[i]);
            if (i < strings.length - 1) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }
}
