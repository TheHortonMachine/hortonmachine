/*

 * GNU GPL v3 License
 *
 * Copyright 2015 Marialaura Bancheri
 *
 * This program is free software: you can redistribute it and/or modify
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
package org.hortonmachine.hmachine.modules.statistics.kriging.utils;

// TODO: Auto-generated Javadoc
/**
 * The Class MaxDistance.
 */
public class MaxDistance implements Model {

    /** The maxdist. */
    double maxdist;

    /** The distance vector. */
    double[] distanceVector;

    /**
     * Instantiates a new max distance.
     *
     * @param distanceVector the distance vector
     * @param maxdist the max distance
     */
    public MaxDistance( double[] distanceVector, double maxdist ) {

        this.distanceVector = distanceVector;
        this.maxdist = maxdist;

    }

    @Override
    public int numberOfStations() {
        // posDist is the number of the stations within the distance
        int posDist = distanceVector.length;
        for( int k = 0; k < distanceVector.length; k++ ) {
            if (distanceVector[k] > maxdist) {
                posDist = k;
                break;
            }
        }

        // in case there are no stations within the distance, the algorithm considers
        // at least the nearest 3
        posDist = (posDist == 1) ? posDist += 3 : posDist;
        return posDist;
    }

}
