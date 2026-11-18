/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears.io.opendap;

import java.util.List;
import java.util.Map;

/**
 * A variable of an OPeNDAP dataset, as described by its DMR.
 *
 * @param name the name of the variable, without the leading slash.
 * @param type the DAP4 type, as Float32 or Int16.
 * @param dimensions the names of the dimensions, without the leading slash, empty for a scalar.
 * @param sizes the sizes of the dimensions.
 * @param attributes the attributes, with the values of multi valued attributes separated by commas.
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record OpendapVariable( String name, String type, List<String> dimensions, List<Integer> sizes,
        Map<String, String> attributes ) {

    /**
     * @param attributeName the name of the attribute.
     * @return the value of the attribute, or <code>null</code>.
     */
    public String attribute( String attributeName ) {
        return attributes.get(attributeName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(type).append(" ").append(name).append("(");
        for( int i = 0; i < dimensions.size(); i++ ) {
            if (i > 0)
                sb.append(", ");
            sb.append(dimensions.get(i)).append("=").append(sizes.get(i));
        }
        sb.append(")");
        String units = attribute("units");
        if (units != null)
            sb.append(" [").append(units).append("]");
        String longName = attribute("long_name");
        if (longName != null)
            sb.append(" ").append(longName);
        return sb.toString();
    }
}
