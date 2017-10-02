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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment;

import java.util.Comparator;

import org.opengis.feature.simple.SimpleFeature;

public class NetIndexComparator implements Comparator<SimpleFeature>, LWFields{

    //establish the position of each net point respect to the others
    
    @Override
    public int compare( SimpleFeature f1, SimpleFeature f2 ) {
        int linkid1 = (Integer) f1.getAttribute(LINKID);
        int linkid2 = (Integer) f2.getAttribute(LINKID);

        if (linkid1 < linkid2) {
            return -1;
        } else if (linkid1 > linkid2) {
            return 1;
        } else {
            return 0;
        }
    }

}
