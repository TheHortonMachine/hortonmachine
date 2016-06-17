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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment;

public interface LWFields {
    static final String LINKID = "linkid";
    static final String PFAF = "pfaf";

    static final String WIDTH = "w";
    static final String WIDTH2 = "w2";
    /**
     * Defines where the width measure comes from.
     * 
     * <p>Values:
     * <ul>
     *  <li>0 = from channeledit shapefile</li>
     *  <li>1 = from dams shapefile</li>
     *  <li>2 = from bridges shapefile</li>
     * </ul>
     * 
     */
    static final String WIDTH_FROM = "w_from";
    static final double WIDTH_FROM_CHANNELEDIT = 0;
    static final double WIDTH_FROM_DAMS = 1;
    static final double WIDTH_FROM_BRIDGES = 2;

    static final String NOTES = "notes";
    // static final String BRIDGE_LENGTH = "LENGHT";
    static final String SLOPE = "slope";
    static final String AVGSLOPE = "slopeavg";

    static final String VOLUME = "volume";
    static final String MEDIAN = "median";

    static final String FIELD_WIDTH = "w2"; // TODO add logical check on width to use
    static final String FIELD_MEDIAN = "median";
    static final String FIELD_ISCRITIC_LOCAL = "iscriticl";
    static final String FIELD_ISCRITIC_GLOBAL = "iscriticg";
    static final String FIELD_CRITIC_SOURCE = "critsource";

    static final String FIELD_WATER_LEVEL = "b_wlevel";
    static final String FIELD_DISCHARGE = "b_disch";
    static final String FIELD_WATER_VELOCITY = "b_wvel";
    
    
    
}
