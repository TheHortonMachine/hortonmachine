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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.adige.core;

import java.io.PrintStream;
import java.util.HashMap;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;

/**
 * Utility class for handling of Offtakes mappings and data retrival. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Offtakes implements DischargeContributor {

    private final HashMap<String, Integer> offtakes_pfaff2idMap;
    private HashMap<Integer, Double> offtakes_id2valuesQMap;
    private final IJGTProgressMonitor out;

    /**
     * Constructor.
     * 
     * @param offtakes_pfaff2idMap {@link HashMap map} of pfafstetter numbers versus
     *                      offtakes points id.
     * @param out {@link PrintStream} for warning handling.
     */
    public Offtakes( HashMap<String, Integer> offtakes_pfaff2idMap, IJGTProgressMonitor out ) {
        this.offtakes_pfaff2idMap = offtakes_pfaff2idMap;
        this.out = out;
    }

    public Double getDischarge( String pNum, double inputDischarge ) {
        Integer damId = offtakes_pfaff2idMap.get(pNum);
        if (damId != null) {
            Double discharge = offtakes_id2valuesQMap.get(damId);
            if (discharge != null) {
                if (inputDischarge >= discharge) {
                    return inputDischarge - discharge;
                } else {
                    out
                            .errorMessage("WARNING: offtake discharge at "
                                    + pNum
                                    + " is greater than the river discharge. Offtake discharge set to 0 to continue.");
                    return inputDischarge;
                }
            }
        }
        return JGTConstants.doubleNovalue;
    }

    public void setCurrentData( HashMap<Integer, Double> currentDataMap ) {
        offtakes_id2valuesQMap = currentDataMap;
    }

}
