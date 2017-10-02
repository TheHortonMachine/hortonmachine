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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core;

import java.util.HashMap;

import org.hortonmachine.gears.libs.modules.HMConstants;

/**
 * Utility class for handling of Hydrometers mappings and data retrival.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Hydrometers implements IDischargeContributor {

    private final HashMap<String, Integer> hydrometer_pfaff2idMap;
    private HashMap<Integer, double[]> hydrometer_id2valuesMap;

    /**
     * Constructor.
     * 
     * @param hydrometer_pfaff2idMap {@link HashMap map} of pfafstetter numbers versus
     *                      hydrometers points id.
     */
    public Hydrometers( HashMap<String, Integer> hydrometer_pfaff2idMap ) {
        this.hydrometer_pfaff2idMap = hydrometer_pfaff2idMap;
    }

    public Double getDischarge( String pfafstetterNumber ) {
        Integer hydroId = hydrometer_pfaff2idMap.get(pfafstetterNumber);
        if (hydroId != null) {
            double[] value = hydrometer_id2valuesMap.get(hydroId);
            if (value != null) {
                return value[0];
            }
        }
        return HMConstants.doubleNovalue;
    }

    public void setCurrentData( HashMap<Integer, double[]> currentDataMap ) {
        hydrometer_id2valuesMap = currentDataMap;
    }

    public double mergeWithDischarge( double contributorDischarge, double inputDischarge ) {
        return contributorDischarge;
    }

}
