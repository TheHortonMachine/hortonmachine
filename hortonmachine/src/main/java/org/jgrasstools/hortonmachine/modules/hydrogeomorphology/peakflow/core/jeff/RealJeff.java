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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.peakflow.core.jeff;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Interval;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RealJeff {

    private double rain_timestep = 0f;
    private final List<DateTime> rainfallTimestampList;
    private final List<double[]> rainfallList;

    public RealJeff( List<DateTime> rainfallTimestampList, List<double[]> rainfallList ) {
        this.rainfallTimestampList = rainfallTimestampList;
        this.rainfallList = rainfallList;
        Interval interval = new Interval(rainfallTimestampList.get(0), rainfallTimestampList.get(1));
        rain_timestep = interval.toDuration().getStandardSeconds();
    }

    public Map<DateTime, Double> calculateJeff() {
        Map<DateTime, Double> jeffData = new LinkedHashMap<DateTime, Double>();
        /*
         * Jeff is returned in m/s instead of mm/h (which is the dimension
         * of rain height over timestep. Therefore let's do some conversion.
         */
        double converter = 1.0 / (1000.0 * 3600.0);

        for( int i = 0; i < rainfallTimestampList.size(); i++ ) {
            DateTime dateTime = rainfallTimestampList.get(i);
            // rainvalue is in mm/h
            double rainValue = rainfallList.get(i)[0];
            // need it in m/s
            double jeff = converter * rainValue;
            jeffData.put(dateTime, jeff);
        }

        return jeffData;
    }

    public double getRain_timestep() {
        return rain_timestep;
    }

    public DateTime getFirstDate() {
        return rainfallTimestampList.get(0);
    }

}
