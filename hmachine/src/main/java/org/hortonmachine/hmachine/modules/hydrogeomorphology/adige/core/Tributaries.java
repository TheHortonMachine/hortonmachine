package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core;

import java.text.MessageFormat;
import java.util.HashMap;

import org.hortonmachine.gears.libs.modules.HMConstants;

/**
 * Utility class for handling of tributary mappings and data retrieval. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Tributaries implements IDischargeContributor {

    private final HashMap<String, Integer> tributary_pfaff2idMap;
    private HashMap<Integer, double[]> tributary_id2valuesQMap;

    /**
     * Constructor.
     * 
     * @param tributary_pfaff2idMap {@link HashMap map} of pfafstetter numbers versus
     *                      tributary points id.
     * @param tributary_id2valuesQMap map of tributary points id versus discharge value.
     */
    public Tributaries( HashMap<String, Integer> tributary_pfaff2idMap ) {
        this.tributary_pfaff2idMap = tributary_pfaff2idMap;
    }

    public Double getDischarge( String pNum ) {
        Integer damId = tributary_pfaff2idMap.get(pNum);
        if (damId != null) {
            double[] discharges = tributary_id2valuesQMap.get(damId);
            return discharges[0];
        }
        return HMConstants.doubleNovalue;
    }

    public void setCurrentData( HashMap<Integer, double[]> currentDataMap ) {
        tributary_id2valuesQMap = currentDataMap;
    }

    public double mergeWithDischarge( double contributorDischarge, double inputDischarge ) {
        return inputDischarge + contributorDischarge;
    }

}
