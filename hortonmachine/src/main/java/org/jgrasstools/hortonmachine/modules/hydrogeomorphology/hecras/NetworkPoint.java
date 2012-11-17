/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * A Hecras point on the network.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NetworkPoint implements Comparable<NetworkPoint> {

    public Coordinate point;

    /**
     * Progressive distance of section on the reach.
     */
    public double progressiveDistance = -1;

    /**
     * Flag that defines if the point has also section data connected.
     */
    public boolean hasSection = false;

    /**
     * If section data are available, the point might have an index useful for proper ordering.
     */
    private int sectionId = -1;

    public LineString sectionGeometry = null;

    public List<Double> bankPositions = null;

    public List<Double> sectionProgressive = null;

    /**
     * Creates a {@link NetworkPoint}.
     * 
     * @param point the point on the mainstream.
     * @param progressiveDistance the cumulated distance from the most upstream point.
     * @param sectionGeometry optional section {@link LineString geometry}.
     *          The line has to be constructed of 3d {@link Coordinate}s that 
     *          go from left to right  looking downstream.
     */
    public NetworkPoint( Coordinate point, double progressiveDistance, LineString sectionGeometry ) {
        this.point = point;
        this.progressiveDistance = progressiveDistance;
        if (sectionGeometry != null) {
            this.sectionGeometry = sectionGeometry;

            Coordinate[] coordinates = sectionGeometry.getCoordinates();
            sectionProgressive = new ArrayList<Double>();
            for( int i = 0; i < coordinates.length; i++ ) {
                if (i == 0) {
                    sectionProgressive.add(0.0);
                } else {
                    double distance = sectionProgressive.get(i - 1) + coordinates[i - 1].distance(coordinates[i]);
                    sectionProgressive.add(distance);
                }
            }

            // default bank positions
            bankPositions = new ArrayList<Double>();
            bankPositions.add(Double.valueOf(0.0D));
            bankPositions.add(Double.valueOf(1.0D));
            bankPositions.add(Double.valueOf(sectionProgressive.get(0)));
            bankPositions.add(Double.valueOf(sectionProgressive.get(sectionProgressive.size() - 1)));

            hasSection = true;
        }
    }
    
    /**
     * Sets the section id for the current section.
     * 
     * <p>If not set the sectionId is -1.</p>
     * 
     * @param sectionId the sectionId to set.
     */
    public void setSectionId( int sectionId ) {
        this.sectionId = sectionId;
    }
    
    /**
     * Gets the section id for the current section.
     * 
     * <p>If not set the sectionId is -1.</p>
     * 
     * @return the sectionId.
     */
    public int getSectionId() {
        return sectionId;
    }

    public int compareTo( NetworkPoint o ) {
        if (progressiveDistance < o.progressiveDistance) {
            return -1;
        } else if (progressiveDistance > o.progressiveDistance) {
            return 1;
        }
        return 0;
    }
}
