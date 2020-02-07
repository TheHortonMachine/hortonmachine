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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.jeff;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.Interval;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RealJeff {

    private double rainTimestep = 0f;
    private final HashMap<DateTime, double[]> rainfallMap;

    private DateTime first;
    private DateTime second;

    /*
     * Jeff is returned in m/s instead of mm/h (which is the dimension
     * of rain height over timestep. Therefore let's do some conversion.
     */
    private double converter = 1.0 / (1000.0 * 3600.0);

    /**
     * @param rainfallMap the sorted map of rainfall values in time. <b>This has to be a sorted map.</b>
     */
    public RealJeff( HashMap<DateTime, double[]> rainfallMap ) {
        this.rainfallMap = rainfallMap;

        Set<Entry<DateTime, double[]>> entrySet = rainfallMap.entrySet();
        for( Entry<DateTime, double[]> entry : entrySet ) {
            if (first == null) {
                first = entry.getKey();
            } else if (second == null) {
                second = entry.getKey();
                break;
            }
        }

        Interval interval = new Interval(first, second);
        rainTimestep = interval.toDuration().getStandardSeconds();
    }

    public Map<DateTime, Double> calculateJeff() {
        Map<DateTime, Double> jeffData = new LinkedHashMap<DateTime, Double>();

        Set<Entry<DateTime, double[]>> entrySet = rainfallMap.entrySet();
        for( Entry<DateTime, double[]> entry : entrySet ) {
            DateTime dateTime = entry.getKey();
            // rainvalue is in mm/h
            double rainValue = entry.getValue()[0];
            // need it in m/s
            double jeff = converter * rainValue;
            jeffData.put(dateTime, jeff);
        }

        return jeffData;
    }

    public double getRain_timestep() {
        return rainTimestep;
    }

    public DateTime getFirstDate() {
        return first;
    }

}
