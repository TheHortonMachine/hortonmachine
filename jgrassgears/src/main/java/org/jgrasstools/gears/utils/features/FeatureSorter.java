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
package org.jgrasstools.gears.utils.features;

import java.util.Comparator;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

/**
 * Sorts a {@link List} of features by one of the fields.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FeatureSorter implements Comparator<SimpleFeature> {
    private final String fieldName;

    public FeatureSorter( String fieldName ) {
        this.fieldName = fieldName;
    }

    public int compare( SimpleFeature f1, SimpleFeature f2 ) {
        if (fieldName == null) {
            throw new IllegalArgumentException("A fieldname for the sorting has to be defined.");
        }

        Object a1 = f1.getAttribute(fieldName);
        Object a2 = f2.getAttribute(fieldName);

        if (a1 instanceof Number && a2 instanceof Number) {
            double d1 = ((Number) a1).doubleValue();
            double d2 = ((Number) a2).doubleValue();
            if (d1 < d2) {
                return -1;
            } else if (d1 > d2) {
                return 1;
            } else {
                return 0;
            }
        }
        throw new IllegalArgumentException("Type not supported.");
    }

}
