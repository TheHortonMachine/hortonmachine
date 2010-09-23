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
package eu.hydrologis.edc.oms.datareader;

import java.util.LinkedHashMap;
import java.util.List;

import org.joda.time.DateTime;

/**
 * Class grouping resulting data from timeseries aggregation.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class AggregatedResult {

    /**
     * A sorted map of pairs of timestamp and value. 
     */
    private LinkedHashMap<DateTime, Double> timestamp2ValueMap = null;

    /**
     * A list of number of data on which the {@link #timestamp2ValueMap} was calculated.
     */
    private List<Integer> validDataNumber = null;

    /**
     * List of variances.
     */
    private final List<Double> varList;

    /**
     * List of quantiles [q10, q25, q50, q75, q90]
     */
    private final List<double[]> quantilesList;

    public AggregatedResult( LinkedHashMap<DateTime, Double> timestamp2ValueMap,
            List<Integer> validDataNumber, List<Double> varList, List<double[]> quantilesList ) {
        this.timestamp2ValueMap = timestamp2ValueMap;
        this.validDataNumber = validDataNumber;
        this.varList = varList;
        this.quantilesList = quantilesList;
    }

    /**
     * Returns the map of aggregated data in time.
     * 
     * @return the map of aggregated data.
     */
    public LinkedHashMap<DateTime, Double> getTimestamp2ValueMap() {
        return timestamp2ValueMap;
    }

    /**
     * Returns the number of valid data that were used for aggregation of a timeframe.
     * 
     * @return number of valid data.
     */
    public List<Integer> getValidDataNumber() {
        return validDataNumber;
    }
    
    /**
     * Returns the list of variance ordered the same way as the {@link #getTimestamp2ValueMap()}.
     * 
     * @return the list of variances.
     */
    public List<Double> getVariance() {
        return varList;
    }
    
    /**
     * Returns the list of quantiles ordered the same way as the {@link #getTimestamp2ValueMap()}.
     * 
     * The array of quantiles is ordered as follows: [q10, q25, q50, q75, q90].
     * 
     * @return the list of quantiles.
     */
    public List<double[]> getQuantiles() {
        return quantilesList;
    }
    
}
