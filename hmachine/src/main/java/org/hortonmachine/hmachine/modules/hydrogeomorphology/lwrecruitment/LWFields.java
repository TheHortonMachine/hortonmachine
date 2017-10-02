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

public interface LWFields {
    static final String LINKID = "linkid";
    static final String PFAF = "pfaf";
    static final String GAUKLER = "ks";

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

    static final String VEG_VOL = "volume";
    static final String VEG_H = "height";
    static final String VEG_DBH = "dbh";

    static final String FIELD_ISCRITIC_LOCAL_FOR_HEIGHT = "iscritl_h";
    static final String FIELD_ISCRITIC_GLOBAL_FOR_HEIGHT = "iscritg_h";
    static final String FIELD_CRITIC_SOURCE_FOR_HEIGHT = "critsrc_h";
    
    static final String FIELD_ISCRITIC_LOCAL_FOR_DIAMETER = "iscritl_d";
    static final String FIELD_ISCRITIC_GLOBAL_FOR_DIAMETER = "iscritg_d";
    static final String FIELD_CRITIC_SOURCE_FOR_DIAMETER = "critsrc_d";

    static final String FIELD_WATER_LEVEL = "b_wlevel";
    static final String FIELD_DISCHARGE = "b_disch";
    static final String FIELD_WATER_VELOCITY = "b_wvel";
    static final String FIELD_WATER_LEVEL2 = "b2_wlevel";
    static final String FIELD_DISCHARGE2 = "b2_disch";
    static final String FIELD_WATER_VELOCITY2 = "b2_wvel";
    
    static final String FIELD_ELEV = "elev";
}
