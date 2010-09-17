/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrasstools.hortonmachine.externals.epanet.core;

/**
 * The attributes for the creation feature.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class EpanetFeatureTypes {
    public static final String PIPES = "pipes";
    public static final String PIPES_SHP = "pipes.shp";
    public static final String VALVES = "valves";
    public static final String VALVES_SHP = "valves.shp";
    public static final String PUMPS = "pumps";
    public static final String PUMPS_SHP = "pumps.shp";
    public static final String RESERVOIRS = "reservoirs";
    public static final String RESERVOIRS_SHP = "reservoirs.shp";
    public static final String TANKS = "tanks";
    public static final String TANKS_SHP = "tanks.shp";
    public static final String JUNCTIONS = "junctions";
    public static final String JUNCTIONS_SHP = "junctions.shp";

    public static final String DC_ID_STR = "dc_id";
    public static final String INSTALLATI_STR = "installati";
    public static final String PATTERN_STR = "pattern";
    public static final String EMITTERCOE_STR = "emittercoe";
    public static final String DEMAND_STR = "demand";
    public static final String RESULT_PRE_STR = "result_pre";
    public static final String RESULT_HEA_STR = "result_hea";
    public static final String RESULT_DEM_STR = "result_dem";
    public static final String RESULT_VEL_STR = "result_vel";
    public static final String RESULT_FLO_STR = "result_flo";
    public static final String ELEVATION_STR = "elevation";
    public static final String BITCODEZON_STR = "bitcodezon";
    public static final String DCSUBTYPE_STR = "dcsubtype";
    public static final String ABANDON_DA_STR = "abandon_da";
    public static final String VOLUMECURV_STR = "volumecurv";
    public static final String MINIMUMVOL_STR = "minimumvol";
    public static final String DIAMETER_STR = "diameter";
    public static final String MAXIMUMLEV_STR = "maximumlev";
    public static final String MINIMUMLEV_STR = "minimumlev";
    public static final String INITIALLEV_STR = "initiallev";
    public static final String HEAD_STR = "head";
    public static final String POWER_KW_STR = "power_kw";
    public static final String PROPERTIES_STR = "properties";
    public static final String MINORLOSS_STR = "minorloss";
    public static final String SETTING_STR = "setting";
    public static final String TYPE_STR = "type";
    public static final String LENGTH_STR = "length";
    public static final String STATUS_STR = "status";
    public static final String ROUGHNESS_STR = "roughness";
    public static final String NODE2_STR = "node2";
    public static final String NODE1_STR = "node1";

    /**
     * The {@link Junctions} attributes and classes.
     */
    public static enum Junctions implements IEpanetType {
        DC_ID(DC_ID_STR, String.class), //
        INSTALLATI(INSTALLATI_STR, String.class), //
        ABANDON_DA(ABANDON_DA_STR, String.class), //
        DCSUBTYPE(DCSUBTYPE_STR, Integer.class), //
        BITCODEZON(BITCODEZON_STR, Integer.class), //
        ELEVATION(ELEVATION_STR, Double.class), //
        RESULT_DEM(RESULT_DEM_STR, Double.class), //
        RESULT_HEA(RESULT_HEA_STR, Double.class), //
        RESULT_PRE(RESULT_PRE_STR, Double.class), //
        DEMAND(DEMAND_STR, Double.class), //
        EMITTERCOE(EMITTERCOE_STR, Double.class), //
        PATTERN(PATTERN_STR, String.class);

        private Class< ? > clazz;
        private String attributeName;

        Junctions( String attributeName, Class< ? > clazz ) {
            this.attributeName = attributeName;
            this.clazz = clazz;
        }

        public Class< ? > getClazz() {
            return clazz;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public String getShapefileName() {
            return JUNCTIONS_SHP;
        }

        public String getName() {
            return JUNCTIONS;
        }
    }

    /**
     * The {@link Tanks} attributes and classes.
     */
    public static enum Tanks implements IEpanetType {
        DC_ID("dc_id", String.class), //
        INSTALLATI(INSTALLATI_STR, String.class), //
        ABANDON_DA(ABANDON_DA_STR, String.class), //
        DCSUBTYPE(DCSUBTYPE_STR, Integer.class), //
        BITCODEZON(BITCODEZON_STR, Integer.class), //
        ELEVATION(ELEVATION_STR, Double.class), //
        RESULT_DEM(RESULT_DEM_STR, Double.class), //
        RESULT_HEA(RESULT_HEA_STR, Double.class), //
        RESULT_PRE(RESULT_PRE_STR, Double.class), //
        INITIALLEV(INITIALLEV_STR, Double.class), //
        MINIMUMLEV(MINIMUMLEV_STR, Double.class), //
        MAXIMUMLEV(MAXIMUMLEV_STR, Double.class), //
        DIAMETER(DIAMETER_STR, Double.class), //
        MINIMUMVOL(MINIMUMVOL_STR, Double.class), //
        VOLUMECURV(VOLUMECURV_STR, String.class);

        private Class< ? > clazz;
        private String attributeName;

        Tanks( String attributeName, Class< ? > clazz ) {
            this.attributeName = attributeName;
            this.clazz = clazz;
        }

        public Class< ? > getClazz() {
            return clazz;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public String getShapefileName() {
            return TANKS_SHP;
        }

        public String getName() {
            return TANKS;
        }
    }

    /**
     * The {@link Reservoirs} attributes and classes.
     */
    public static enum Reservoirs implements IEpanetType {
        DC_ID(DC_ID_STR, String.class), //
        INSTALLATI(INSTALLATI_STR, String.class), //
        ABANDON_DA(ABANDON_DA_STR, String.class), //
        DCSUBTYPE(DCSUBTYPE_STR, Integer.class), //
        BITCODEZON(BITCODEZON_STR, Integer.class), //
        ELEVATION(ELEVATION_STR, Double.class), //
        RESULT_DEM(RESULT_DEM_STR, Double.class), //
        RESULT_HEA(RESULT_HEA_STR, Double.class), //
        RESULT_PRE(RESULT_PRE_STR, Double.class), //
        HEAD(HEAD_STR, Double.class), //
        PATTERN(PATTERN_STR, String.class);

        private Class< ? > clazz;
        private String attributeName;

        Reservoirs( String attributeName, Class< ? > clazz ) {
            this.attributeName = attributeName;
            this.clazz = clazz;
        }

        public Class< ? > getClazz() {
            return clazz;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public String getShapefileName() {
            return RESERVOIRS_SHP;
        }

        public String getName() {
            return RESERVOIRS;
        }
    }

    /**
     * The {@link Pumps} attributes and classes.
     */
    public static enum Pumps implements IEpanetType {
        DC_ID(DC_ID_STR, String.class), //
        INSTALLATI(INSTALLATI_STR, String.class), //
        ABANDON_DA(ABANDON_DA_STR, String.class), //
        DCSUBTYPE(DCSUBTYPE_STR, Integer.class), //
        BITCODEZON(BITCODEZON_STR, Integer.class), //
        ELEVATION(ELEVATION_STR, Double.class), //
        RESULT_DEM(RESULT_DEM_STR, Double.class), //
        RESULT_HEA(RESULT_HEA_STR, Double.class), //
        RESULT_PRE(RESULT_PRE_STR, Double.class), //
        RESULT_FLO(RESULT_FLO_STR, Double.class), //
        RESULT_VEL(RESULT_VEL_STR, Double.class), //
        PROPERTIES(PROPERTIES_STR, String.class), //
        POWER_KW(POWER_KW_STR, Integer.class);

        private Class< ? > clazz;
        private String attributeName;

        Pumps( String attributeName, Class< ? > clazz ) {
            this.attributeName = attributeName;
            this.clazz = clazz;
        }

        public Class< ? > getClazz() {
            return clazz;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public String getShapefileName() {
            return PUMPS_SHP;
        }

        public String getName() {
            return PUMPS;
        }
    }

    /**
     * The {@link Valves} attributes and classes.
     */
    public static enum Valves implements IEpanetType {
        DC_ID(DC_ID_STR, String.class), //
        INSTALLATI(INSTALLATI_STR, String.class), //
        ABANDON_DA(ABANDON_DA_STR, String.class), //
        DCSUBTYPE(DCSUBTYPE_STR, Integer.class), //
        BITCODEZON(BITCODEZON_STR, Integer.class), //
        ELEVATION(ELEVATION_STR, Double.class), //
        RESULT_DEM(RESULT_DEM_STR, Double.class), //
        RESULT_HEA(RESULT_HEA_STR, Double.class), //
        RESULT_PRE(RESULT_PRE_STR, Double.class), //
        RESULT_FLO(RESULT_FLO_STR, Double.class), //
        RESULT_VEL(RESULT_VEL_STR, Double.class), //
        DIAMETER(DIAMETER_STR, Integer.class), //
        TYPE(TYPE_STR, String.class), //
        SETTING(SETTING_STR, String.class), //
        MINORLOSS(MINORLOSS_STR, Double.class);

        private Class< ? > clazz;
        private String attributeName;

        Valves( String attributeName, Class< ? > clazz ) {
            this.attributeName = attributeName;
            this.clazz = clazz;
        }

        public Class< ? > getClazz() {
            return clazz;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public String getShapefileName() {
            return VALVES_SHP;
        }

        public String getName() {
            return VALVES;
        }
    }

    /**
     * The {@link Pipes} attributes and classes.
     */
    public static enum Pipes implements IEpanetType {
        DC_ID(DC_ID_STR, String.class), //
        INSTALLATI(INSTALLATI_STR, String.class), //
        ABANDON_DA(ABANDON_DA_STR, String.class), //
        DCSUBTYPE(DCSUBTYPE_STR, Integer.class), //
        BITCODEZON(BITCODEZON_STR, Integer.class), //
        DIAMETER(DIAMETER_STR, Integer.class), //
        NODE1(NODE1_STR, String.class), //
        NODE2(NODE2_STR, String.class), //
        ROUGHNESS(ROUGHNESS_STR, Double.class), //
        MINORLOSS(MINORLOSS_STR, Double.class), //
        STATUS(STATUS_STR, String.class), //
        RESULT_FLOW(RESULT_FLO_STR, Double.class), //
        RESULT_VELO(RESULT_VEL_STR, Double.class), //
        RESULT_HEA(RESULT_HEA_STR, Double.class), //
        LENGTH(LENGTH_STR, Double.class);

        private Class< ? > clazz;
        private String attributeName;

        Pipes( String attributeName, Class< ? > clazz ) {
            this.attributeName = attributeName;
            this.clazz = clazz;
        }

        public Class< ? > getClazz() {
            return clazz;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public String getShapefileName() {
            return PIPES_SHP;
        }

        public String getName() {
            return PIPES;
        }
    }

}
