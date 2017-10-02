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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.debrisflow;

public class SlopeProbability implements Comparable<SlopeProbability> {
    public double slope;
    public int fromRow;
    public int fromCol;
    public double fromElev;
    public int toRow;
    public int toCol;
    public double toElev;
    public double probability;

    public int compareTo( SlopeProbability o ) {
        if (slope < o.slope) {
            return -1;
        } else if (slope > o.slope) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "SlopeProbability [slope=" + slope + ", fromRow=" + fromRow + ", fromCol=" + fromCol + ", fromElev=" + fromElev
                + ", toRow=" + toRow + ", toCol=" + toCol + ", toElev=" + toElev + ", probability=" + probability + "]";
    }

}
