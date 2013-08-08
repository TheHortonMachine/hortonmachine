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
package org.jgrasstools.gears.io.las.utils;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Point;

/**
 * Utilities for Las handling classes.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasUtils {
    public enum POINTTYPE {
        UNCLASSIFIED(1, "UNCLASSIFIED"), //
        GROUND(2, "GROUND"), //
        VEGETATION_MIN(3, "LOW VEGETATION"), //
        VEGETATION_MED(4, "MEDIUM VEGETATION"), //
        VEGETATION_MAX(5, "HIGH VEGETATION"), //
        BUILDING(6, "BUILDING"), //
        LOW_POINT(7, "LOW POINT (NOISE)"), //
        MASS_POINT(8, "MODEL KEY-POINT (MASS)"), //
        WATER(9, "WATER"), //
        OVERLAP(12, "OVERLAP");

        private String label;
        private int value;

        POINTTYPE( int value, String label ) {
            this.value = value;
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Creates a builder for las data.
     * 
     * The attributes are:
     * 
     * <ul>
     *   <li>the_geom:  a point geometry</li>
     *   <li>elev</li>
     *   <li>intensity</li>
     *   <li>classification</li>
     *   <li>impulse</li>
     *   <li>numimpulse</li>
     * </ul>
     * 
     * 
     * @param crs the {@link CoordinateReferenceSystem}.
     * @return the {@link SimpleFeatureBuilder builder}.
     */
    public static SimpleFeatureBuilder getLasFeatureTypeBuilder( CoordinateReferenceSystem crs ) {
        final SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("lasdata");
        b.setCRS(crs);
        b.add("the_geom", Point.class);
        b.add("elev", Double.class);
        b.add("intensity", Double.class);
        b.add("classification", Integer.class);
        b.add("impulse", Double.class);
        b.add("numimpulse", Double.class);
        final SimpleFeatureType featureType = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);

        return builder;
    }

}
