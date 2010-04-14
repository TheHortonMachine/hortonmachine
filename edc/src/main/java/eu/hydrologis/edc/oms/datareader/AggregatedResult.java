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

import static java.lang.Double.NaN;

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
     * The mean of the aggregated data.
     */
    private double mean = NaN;

    /**
     * The variance of the aggregated data.
     */
    private double variance = NaN;

    /**
     * The 10-Quantile. 
     */
    private double quantile10 = NaN;

    /**
     * The 25-Quantile. 
     */
    private double quantile25 = NaN;

    /**
     * The 75-Quantile. 
     */
    private double quantile75 = NaN;

    /**
     * The 90-Quantile. 
     */
    private double quantile90 = NaN;

    public AggregatedResult( LinkedHashMap<DateTime, Double> timestamp2ValueMap,
            List<Integer> validDataNumber ) {
        this.timestamp2ValueMap = timestamp2ValueMap;
        this.validDataNumber = validDataNumber;

        /*
         * TODO calculate mean etc
         */
    }

    public LinkedHashMap<DateTime, Double> getTimestamp2ValueMap() {
        return timestamp2ValueMap;
    }

    public List<Integer> getValidDataNumber() {
        return validDataNumber;
    }

    public double getMean() {
        return mean;
    }

    public double getVariance() {
        return variance;
    }

    public double getQuantile10() {
        return quantile10;
    }

    public double getQuantile25() {
        return quantile25;
    }

    public double getQuantile75() {
        return quantile75;
    }

    public double getQuantile90() {
        return quantile90;
    }

}
