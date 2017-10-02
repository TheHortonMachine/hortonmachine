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

package org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo;

import java.util.ArrayList;
import java.util.List;

/**
 * A river section.
 * 
 * @author Silvia Franceschi (www.hydrologis.com)
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Section implements Comparable<Section> {
    private int id = -1;
    private int numNodes = -1;
    private double progressiveAlongReach = -1.0;
    private List<Double> sectionProgressive = null;
    private List<Double> sectionElevation = null;
    private int startNode = -1;
    private int endNode = -1;
    private List<Double> stricklerCoeff = null;
    private double minElevation = Double.MAX_VALUE;
    private double maxElevation = Double.MIN_VALUE;

    private List<Integer> qDeltaPointsIds = new ArrayList<Integer>();
    private boolean hasQDeltas = false;

    public Section( int id, double progressiveAlongReach, int startNode, int endNode, List<Double> sectionX,
            List<Double> sectionY, List<Double> stricklerCoeff ) {
        this.id = id;
        this.progressiveAlongReach = progressiveAlongReach;
        this.startNode = startNode;
        this.endNode = endNode;
        this.stricklerCoeff = stricklerCoeff;
        this.sectionProgressive = sectionX;
        this.sectionElevation = sectionY;

        numNodes = 0;
        for( double y : sectionY ) {
            numNodes++;
            if (y > maxElevation)
                maxElevation = y;
            if (y < minElevation)
                minElevation = y;
        }
    }

    public int getId() {
        return id;
    }

    public int getNodesNumber() {
        return numNodes;
    }

    public double getProgressiveAlongReach() {
        return progressiveAlongReach;
    }

    public void setProgressiveAlongReach( double newProgressive ) {
        progressiveAlongReach = newProgressive;
    }

    public int getStartNodeIndex() {
        return startNode;
    }

    public int getEndNodeIndex() {
        return endNode;
    }

    public double getStricklerCoeffAt( int index ) {
        return stricklerCoeff.get(index);
    }

    public double getProgressiveAt( int index ) {
        return sectionProgressive.get(index);
    }

    public double getElevationAt( int index ) {
        return sectionElevation.get(index);
    }

    public double getMinElevation() {
        return minElevation;
    }

    public double getMaxElevation() {
        return maxElevation;
    }

    public void addQDeltaPointId( int id ) {
        qDeltaPointsIds.add(id);
        hasQDeltas = true;
    }

    public List<Integer> getQDeltaPointsIds() {
        return qDeltaPointsIds;
    }

    public boolean hasQDeltas() {
        return hasQDeltas;
    }

    @SuppressWarnings("nls")
    public String toString() {
        String retValue = "Section \n( " + "numNodes = " + this.numNodes + "\n" + "progressiveAlongReach = "
                + this.progressiveAlongReach + "\n" + "sectionX[0] = " + this.sectionProgressive.get(0) + "\n" + "sectionY[0] = "
                + this.sectionElevation.get(0) + "\n" + "startNode = " + this.startNode + "\n" + "endNode = " + this.endNode
                + "\n" + "stricklerCoeff[0] = " + this.stricklerCoeff.get(0) + "\n" + "minY = " + this.minElevation + "\n"
                + "maxY = " + this.maxElevation + "\n" + ")";

        return retValue;
    }

    public int compareTo( Section section ) {
        double thisProg = this.getProgressiveAlongReach();
        double nextProg = section.getProgressiveAlongReach();

        if (thisProg == nextProg) {
            return 0;
        } else if (thisProg < nextProg) {
            return -1;
        } else if (thisProg > nextProg) {
            return 1;
        } else
            return 0;
    }
}