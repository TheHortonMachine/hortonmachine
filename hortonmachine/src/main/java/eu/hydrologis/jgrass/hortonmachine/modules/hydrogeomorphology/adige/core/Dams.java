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
 * Utility class for handling of Dams mappings and data retrival. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Dams implements DischargeContributor {

    private final HashMap<String, Integer> dams_pfaff2idMap;
    private final HashMap<Integer, Double> dams_id2valuesQMap;

    /**
     * Constructor.
     * 
     * @param dams_pfaff2idMap {@link HashMap map} of pfafstetter numbers versus
     *                      dams points id.
     * @param dams_id2valuesQMap map of dams points id versus discharge value.
     */
    public Dams( HashMap<String, Integer> dams_pfaff2idMap,
            HashMap<Integer, Double> dams_id2valuesQMap ) {
        this.dams_pfaff2idMap = dams_pfaff2idMap;
        this.dams_id2valuesQMap = dams_id2valuesQMap;
    }

    public Double getDischarge( String pNum, double inputDischarge ) {
        Integer damId = dams_pfaff2idMap.get(pNum);
        if (damId != null) {
            Double discharge = dams_id2valuesQMap.get(damId);
            if (discharge != null) {
                return discharge;
            }
        }
        return HMConstants.doubleNovalue;
    }

}
