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

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.HashMap;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

/**
 * Utility class for handling of Offtakes mappings and data retrival. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Offtakes implements IDischargeContributor {

    private final HashMap<String, Integer> offtakes_pfaff2idMap;
    private HashMap<Integer, double[]> offtakes_id2valuesQMap;
    private final IHMProgressMonitor out;
    private String pNum;

    /**
     * Constructor.
     * 
     * @param offtakes_pfaff2idMap {@link HashMap map} of pfafstetter numbers versus
     *                      offtakes points id.
     * @param out {@link PrintStream} for warning handling.
     */
    public Offtakes( HashMap<String, Integer> offtakes_pfaff2idMap, IHMProgressMonitor out ) {
        this.offtakes_pfaff2idMap = offtakes_pfaff2idMap;
        this.out = out;
    }

    public Double getDischarge( String pNum ) {
        this.pNum = pNum;
        Integer damId = offtakes_pfaff2idMap.get(pNum);
        if (damId != null) {
            double[] discharges = offtakes_id2valuesQMap.get(damId);
            return discharges[0];
        }
        return HMConstants.doubleNovalue;
    }

    public void setCurrentData( HashMap<Integer, double[]> currentDataMap ) {
        offtakes_id2valuesQMap = currentDataMap;
    }

    public double mergeWithDischarge( double contributorDischarge, double inputDischarge ) {
        if (inputDischarge >= contributorDischarge) {
            return inputDischarge - contributorDischarge;
        } else {
            out.errorMessage(MessageFormat
                    .format("WARNING: offtake discharge at {0} is greater than the river discharge. Offtake discharge set to 0 to continue.",
                            pNum));
            return inputDischarge;
        }
    }

}
