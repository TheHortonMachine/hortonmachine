/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gears.utils.math;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * A simple statistics class that wraps DescriptiveStatistics.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SimpleStats {
    private DescriptiveStatistics stats = new DescriptiveStatistics();

    public void addValue( double v ) {
        stats.addValue(v);
    }

    public double getMean() {
        return stats.getMean();
    }

    public double getSum() {
        return stats.getSum();
    }

    public double getMin() {
        return stats.getMin();
    }

    public double getMax() {
        return stats.getMax();
    }

    public double getStandardDeviation() {
        return stats.getStandardDeviation();
    }

    public double getVariance() {
        return stats.getVariance();
    }

    /**
     * @param p the percentile between 0 and 100.
     * @return the precentile.
     */
    public double getPercentile( double p ) {
        return stats.getPercentile(p);
    }

    public long getCount() {
        return stats.getN();
    }
}