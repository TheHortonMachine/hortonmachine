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
package eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core;

import java.util.HashMap;

import eu.hydrologis.jgrass.hortonmachine.libs.models.HMConstants;

/**
 * Utility class for handling of Hydrometers mappings and data retrival.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Hydrometers implements DischargeContributor {

    private final HashMap<String, Integer> hydrometer_pfaff2idMap;
    private final HashMap<Integer, Double> hydrometer_id2valuesMap;

    /**
     * Constructor.
     * 
     * @param hydrometer_pfaff2idMap {@link HashMap map} of pfafstetter numbers versus
     *                      hydrometers points id.
     * @param hydrometer_id2valuesMap map of hydrometer points id versus discharge value.
     */
    public Hydrometers( HashMap<String, Integer> hydrometer_pfaff2idMap,
            HashMap<Integer, Double> hydrometer_id2valuesMap ) {
        this.hydrometer_pfaff2idMap = hydrometer_pfaff2idMap;
        this.hydrometer_id2valuesMap = hydrometer_id2valuesMap;
    }

    public Double getDischarge( String pfafstetterNumber, double inputDischarge ) {
        Integer hydroId = hydrometer_pfaff2idMap.get(pfafstetterNumber);
        if (hydroId != null) {
            Double value = hydrometer_id2valuesMap.get(hydroId);
            if (value != null) {
                return value;
            }
        }
        return HMConstants.doubleNovalue;
    }

}
