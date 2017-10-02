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
package org.hortonmachine.gears.io.las.utils;


/**
 * Object to hold some info about the las file.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasStats {

    private int[] classifications = new int[20];
    private int[] impulses = new int[20];

    private double[] intensityRange = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};

    public void addClassification( int classificationType ) {
        classifications[classificationType] = classifications[classificationType] + 1;
    }

    public void addImpulse( int impulseId ) {
        impulses[impulseId] = impulses[impulseId] + 1;
    }

    public void addIntensity( double intensity ) {
        intensityRange[0] = Math.min(intensityRange[0], intensity);
        intensityRange[1] = Math.max(intensityRange[1], intensity);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Classifications contained\n");
        for( int i = 0; i < classifications.length; i++ ) {
            if (classifications[i] != 0) {
                sb.append(i).append(" = ").append(classifications[i]).append("\n");
            }
        }
        sb.append("Impulses contained\n");
        for( int i = 0; i < impulses.length; i++ ) {
            if (impulses[i] != 0) {
                sb.append(i).append(" = ").append(impulses[i]).append("\n");
            }
        }
        sb.append("Intensity range\n");
        sb.append("min = ").append(intensityRange[0]).append("\n");
        sb.append("max = ").append(intensityRange[1]).append("\n");

        return sb.toString();
    }

}
