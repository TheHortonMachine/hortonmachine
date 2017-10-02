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
 * The form item interface.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface Item {
    String getKey();
    void setValue( String value );
    String getValue();

    default String getRangeString( double[] range, boolean[] rangeInclusiveness ) {
        double min = range[0];
        double max = range[1];
        boolean ri1 = rangeInclusiveness[0];
        boolean ri2 = rangeInclusiveness[1];
        return getRangeString(min, max, ri1, ri2);
    }

    default String getRangeString( int[] range, boolean[] rangeInclusiveness ) {
        double min = range[0];
        double max = range[1];
        boolean ri1 = rangeInclusiveness[0];
        boolean ri2 = rangeInclusiveness[1];
        return getRangeString(min, max, ri1, ri2);
    }
    
    default String getRangeString( double min, double max, boolean ri1, boolean ri2 ) {
        StringBuilder rangeSb = new StringBuilder();
        rangeSb.append("\"range\":\"");
        if (ri1) {
            rangeSb.append("[");
        } else {
            rangeSb.append("(");
        }
        rangeSb.append(min).append(",").append(max);
        if (ri2) {
            rangeSb.append("]\"");
        } else {
            rangeSb.append(")\"");
        }
        String rangeString = rangeSb.toString();
        return rangeString;
    }
}
