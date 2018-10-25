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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

/**
 * A river point with all section information.
 * 
 * <p>RiverPoints are comparable by progressive distance.
 * 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
public class RiverPoint implements Comparable<RiverPoint> {

    /**
     * Coordinate of the section on the river.
     */
    public Coordinate point;

    /**
     * Progressive distance of section on the river.
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

    private List<Double> sectionProgressive = null;

    private Double sectionGauklerStrickler;

    private Coordinate[] sectionCoordinates = null;

    /**
     * The min elevation of the section.
     */
    private double minElevation = Double.POSITIVE_INFINITY;
    /**
     * The max elevation of the section.
     */
    private double maxElevation = Double.NEGATIVE_INFINITY;

    private int startNodeIndex = 0;
    private int endNodeIndex = 0;

    /**
     * Creates a {@link RiverPoint}.
     * 
     * @param point the point on the mainstream.
     * @param progressiveDistance the cumulated distance from the most upstream point.
     * @param sectionGeometry optional section {@link LineString geometry}.
     *          The line has to be constructed of 3d {@link Coordinate}s that 
     *          go from left to right  looking downstream.
     * @param sectionGauklerStrickler the option KS value. If null, default is assigned.         
     */
    public RiverPoint( Coordinate point, double progressiveDistance, LineString sectionGeometry,
            Double sectionGauklerStrickler ) {
        this.point = point;
        this.progressiveDistance = progressiveDistance;

        if (sectionGeometry != null) {
            this.sectionGeometry = sectionGeometry;

            sectionCoordinates = sectionGeometry.getCoordinates();
            sectionProgressive = new ArrayList<Double>();
            for( int i = 0; i < sectionCoordinates.length; i++ ) {
                minElevation = Math.min(minElevation, sectionCoordinates[i].z);
                maxElevation = Math.max(maxElevation, sectionCoordinates[i].z);
                if (i == 0) {
                    sectionProgressive.add(0.0);
                } else {
                    double distance = sectionProgressive.get(i - 1) + sectionCoordinates[i - 1].distance(sectionCoordinates[i]);
                    sectionProgressive.add(distance);
                }
            }

            // default bank positions
            bankPositions = new ArrayList<Double>();
            bankPositions.add(0.0);
            bankPositions.add(1.0);
            bankPositions.add(sectionProgressive.get(0));
            bankPositions.add(sectionProgressive.get(sectionProgressive.size() - 1));

            // TODO make this better, for now it takes the whole section
            startNodeIndex = 0;
            endNodeIndex = sectionProgressive.size() - 1;

            if (sectionGauklerStrickler != null) {
                this.sectionGauklerStrickler = sectionGauklerStrickler;
            } else {
            	this.sectionGauklerStrickler = 30.0;
            }

            hasSection = true;
        }
    }

    public Coordinate[] getSectionCoordinates() {
        return sectionCoordinates;
    }

    public List<Double> getSectionProgressive() {
        return sectionProgressive;
    }

    public double getSectionGauklerStrickler() {
        return sectionGauklerStrickler;
    }

    public boolean hasGauklerStrickler() {
        return sectionGauklerStrickler != null;
    }

    /**
     * @return the xyz coordinate of the talweg.
     */
    public Coordinate getTalWeg() {
        return point;
    }

    /**
     * @return the progressive distance of the section along the river.
     */
    public double getProgressiveDistance() {
        return progressiveDistance;
    }

    public double getMinElevation() {
        return minElevation;
    }

    public double getMaxElevation() {
        return maxElevation;
    }

    public int getStartNodeIndex() {
        return startNodeIndex;
    }

    public int getEndNodeIndex() {
        return endNodeIndex;
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
     * Sets the section ks for the current section.
     * 
     * <p>If not set the sectionId is -1.</p>
     * 
     * @param sectionId the sectionId to set.
     */
    public void setSectionGaukler( double sectionGauklerStrickler ) {
        this.sectionGauklerStrickler = sectionGauklerStrickler;
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

    public int compareTo( RiverPoint o ) {
        if (progressiveDistance < o.progressiveDistance) {
            return -1;
        } else if (progressiveDistance > o.progressiveDistance) {
            return 1;
        }
        return 0;
    }
}
