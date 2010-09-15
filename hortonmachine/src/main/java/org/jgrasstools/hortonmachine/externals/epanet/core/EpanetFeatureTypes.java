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
 */
@SuppressWarnings("nls")
public class EpanetFeatureTypes {
    public interface EpanetTypes {
        public Class< ? > getClazz();
        public String getAttributeName();
        public String getShapefileName();
        public String getName();
    }
    
    /**
     * The {@link Junctions} attributes and classes.
     */
    public static enum Junctions implements EpanetTypes {
        DC_ID("dc_id", String.class), //
        INSTALLATI("installati", String.class), //
        ABANDON_DA("abandon_da", String.class), //
        DCSUBTYPE("dcsubtype", Integer.class), //
        BITCODEZON("bitcodezon", Integer.class), //
        ELEVATION("elevation", Double.class), //
        RESULT_DEM("result_dem", Double.class), //
        RESULT_HEA("result_hea", Double.class), //
        RESULT_PRE("result_pre", Double.class), //
        DEMAND("demand", Double.class), //
        EMITTERCOE("emittercoe", Double.class), //
        PATTERN("pattern", String.class);

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
            return "junctions.shp";
        }

        public String getName() {
            return "junctions";
        }
    }

    /**
     * The {@link Tanks} attributes and classes.
     */
    public static enum Tanks implements EpanetTypes {
        DC_ID("dc_id", String.class), //
        INSTALLATI("installati", String.class), //
        ABANDON_DA("abandon_da", String.class), //
        DCSUBTYPE("dcsubtype", Integer.class), //
        BITCODEZON("bitcodezon", Integer.class), //
        ELEVATION("elevation", Double.class), //
        RESULT_DEM("result_dem", Double.class), //
        RESULT_HEA("result_hea", Double.class), //
        RESULT_PRE("result_pre", Double.class), //
        INITIALLEV("initiallev", Double.class), //
        MINIMUMLEV("minimumlev", Double.class), //
        MAXIMUMLEV("maximumlev", Double.class), //
        DIAMETER("diameter", Double.class), //
        MINIMUMVOL("minimumvol", Double.class), //
        VOLUMECURV("volumecurv", String.class);

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
            return "tanks.shp";
        }

        public String getName() {
            return "tanks";
        }
    }

    /**
     * The {@link Reservoirs} attributes and classes.
     */
    public static enum Reservoirs implements EpanetTypes {
        DC_ID("dc_id", String.class), //
        INSTALLATI("installati", String.class), //
        ABANDON_DA("abandon_da", String.class), //
        DCSUBTYPE("dcsubtype", Integer.class), //
        BITCODEZON("bitcodezon", Integer.class), //
        ELEVATION("elevation", Double.class), //
        RESULT_DEM("result_dem", Double.class), //
        RESULT_HEA("result_hea", Double.class), //
        RESULT_PRE("result_pre", Double.class), //
        HEAD("head", Double.class), //
        PATTERN("pattern", String.class);

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
            return "reservoirs.shp";
        }

        public String getName() {
            return "reservoirs";
        }
    }

    /**
     * The {@link Pumps} attributes and classes.
     */
    public static enum Pumps implements EpanetTypes {
        DC_ID("dc_id", String.class), //
        INSTALLATI("installati", String.class), //
        ABANDON_DA("abandon_da", String.class), //
        DCSUBTYPE("dcsubtype", Integer.class), //
        BITCODEZON("bitcodezon", Integer.class), //
        ELEVATION("elevation", Double.class), //
        RESULT_DEM("result_dem", Double.class), //
        RESULT_HEA("result_hea", Double.class), //
        RESULT_PRE("result_pre", Double.class), //
        RESULT_FLO("result_flo", Double.class), //
        RESULT_VEL("result_vel", Double.class), //
        PROPERTIES("properties", String.class), //
        POWER_KW("power_kw", Integer.class);

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
            return "pumps.shp";
        }

        public String getName() {
            return "pumps";
        }
    }

    /**
     * The {@link Valves} attributes and classes.
     */
    public static enum Valves implements EpanetTypes {
        DC_ID("dc_id", String.class), //
        INSTALLATI("installati", String.class), //
        ABANDON_DA("abandon_da", String.class), //
        DCSUBTYPE("dcsubtype", Integer.class), //
        BITCODEZON("bitcodezon", Integer.class), //
        ELEVATION("elevation", Double.class), //
        RESULT_DEM("result_dem", Double.class), //
        RESULT_HEA("result_hea", Double.class), //
        RESULT_PRE("result_pre", Double.class), //
        RESULT_FLO("result_flo", Double.class), //
        RESULT_VEL("result_vel", Double.class), //
        DIAMETER("diameter", Integer.class), //
        TYPE("type", String.class), //
        SETTING("setting", String.class), //
        MINORLOSS("minorloss", Double.class);

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
            return "valves.shp";
        }

        public String getName() {
            return "valves";
        }
    }

    /**
     * The {@link Pipes} attributes and classes.
     */
    public static enum Pipes implements EpanetTypes {
        DC_ID("dc_id", String.class), //
        INSTALLATI("installati", String.class), //
        ABANDON_DA("abandon_da", String.class), //
        DCSUBTYPE("dcsubtype", Integer.class), //
        BITCODEZON("bitcodezon", Integer.class), //
        DIAMETER("diameter", Integer.class), //
        NODE1("node1", String.class), //
        NODE2("node2", String.class), //
        ROUGHNESS("roughness", Double.class), //
        MINORLOSS("minorloss", Double.class), //
        STATUS("status", String.class), //
        RESULT_FLOW("result_flow", Double.class), //
        RESULT_VELO("result_velo", Double.class), //
        RESULT_HEA("result_hea", Double.class), //
        LENGTH("length", Double.class);

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
            return "pipes.shp";
        }

        public String getName() {
            return "pipes";
        }
    }

}
