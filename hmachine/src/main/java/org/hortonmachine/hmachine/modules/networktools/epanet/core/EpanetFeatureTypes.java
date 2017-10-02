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
package org.hortonmachine.hmachine.modules.networktools.epanet.core;

/**
 * The attributes for the creation feature.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class EpanetFeatureTypes {
    private static final String PIPES = "pipes";
    private static final String PIPES_SHP = "pipes.shp";
    private static final String PIPES_PRE = "PI";
    private static final String VALVES = "valves";
    private static final String VALVES_SHP = "valves.shp";
    private static final String VALVES_PRE = "V";
    private static final String PUMPS = "pumps";
    private static final String PUMPS_SHP = "pumps.shp";
    private static final String PUMPS_PRE = "PU";
    private static final String RESERVOIRS = "reservoirs";
    private static final String RESERVOIRS_SHP = "reservoirs.shp";
    private static final String RESERVOIRS_PRE = "R";
    private static final String TANKS = "tanks";
    private static final String TANKS_SHP = "tanks.shp";
    private static final String TANKS_PRE = "T";
    private static final String JUNCTIONS = "junctions";
    private static final String JUNCTIONS_SHP = "junctions.shp";
    private static final String JUNCTIONS_PRE = "J";

    private static final String ID_STR = "id";
    private static final String PATTERN_STR = "pattern_id";
    private static final String EMITTER_COEFFICIENT_STR = "emitt_coef";
    private static final String DEMAND_STR = "demand";
    private static final String ELEVATION_STR = "elev";
    private static final String VOLUME_CURVE_ID_STR = "vol_cur_id";
    private static final String MINIMUM_VOLUME_STR = "min_vol";
    private static final String DIAMETER_STR = "diam";
    private static final String LEAKCOEFF_STR = "leakcoeff";
    private static final String MAXIMUM_WATER_LEVEL_STR = "max_lev";
    private static final String MINIMUM_WATER_LEVEL_STR = "min_lev";
    private static final String INITIAL_WATER_LEVEL_STR = "init_lev";
    private static final String HEAD_ID_STR = "head_id";
    private static final String HEAD_STR = "head";
    private static final String POWER_STR = "power";
    private static final String MINORLOSS_STR = "min_loss";
    private static final String SETTING_STR = "setting";
    private static final String TYPE_STR = "type";
    private static final String LENGTH_STR = "length";
    private static final String STATUS_STR = "status";
    private static final String ROUGHNESS_STR = "rough";
    private static final String STARTNODE_STR = "startnode";
    private static final String ENDNODE_STR = "endnode";
    private static final String SPEED_STR = "speed";
    private static final String PRICE_STR = "price";
    private static final String PRICE_PATTERN_STR = "pri_pat_id";
    private static final String EFFICIENCY_ID_STR = "effic_id";
    private static final String DEPTH_STR = "DEPTH";

    /**
     * The {@link Junctions} attributes and classes.
     */
    public static enum Junctions implements IEpanetType {
        /**
         * Unique id of the junction. 
         */
        ID(ID_STR, String.class), //
        /**
         * The elevation of the junction.
         */
        ELEVATION(ELEVATION_STR, Double.class), //
        /**
         * The depth of the junction below the elev value.
         */
        DEPTH(DEPTH_STR, Double.class), //
        /**
         * Base demand flow.
         */
        DEMAND(DEMAND_STR, Double.class), //
        /**
         * Defines junction modeled as emitters (sprinklers or orificies).
         */
        EMITTER_COEFFICIENT(EMITTER_COEFFICIENT_STR, Double.class), //
        /**
         * Demand pattern id.
         */
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

        public String getPrefix() {
            return JUNCTIONS_PRE;
        }
    }

    /**
     * The {@link Tanks} attributes and classes.
     */
    public static enum Tanks implements IEpanetType {
        /**
         * Unique id of the junction. 
         */
        ID(ID_STR, String.class), //
        /**
         * The bottom elevation of the tank.
         * 
         *  <p>Water surface elevation = bottom elevation + water level.
         */
        BOTTOM_ELEVATION(ELEVATION_STR, Double.class), //
        /**
         * Initial water level.
         */
        INITIAL_WATER_LEVEL(INITIAL_WATER_LEVEL_STR, Double.class), //
        /**
         * Minimum water level.
         */
        MINIMUM_WATER_LEVEL(MINIMUM_WATER_LEVEL_STR, Double.class), //
        /**
         * Maximum water level.
         */
        MAXIMUM_WATER_LEVEL(MAXIMUM_WATER_LEVEL_STR, Double.class), //
        /**
         * Nominal diameter for cylindrical tanks.
         * 
         * <p>If a volume curve is supplied, the diameter value
         * can be any non zero number.
         */
        DIAMETER(DIAMETER_STR, Double.class), //
        /**
         * Minimum volume.
         * 
         * <p>Can be 0 for cylindrical tanks or if a 
         * volume curve is supplied.
         */
        MINIMUM_VOLUME(MINIMUM_VOLUME_STR, Double.class), //
        /**
         * Volume curve id.
         */
        VOLUME_CURVE_ID(VOLUME_CURVE_ID_STR, String.class);

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

        public String getPrefix() {
            return TANKS_PRE;
        }
    }

    /**
     * The {@link Reservoirs} attributes and classes.
     */
    public static enum Reservoirs implements IEpanetType {
        /**
         * Unique id of the junction. 
         */
        ID(ID_STR, String.class), //
        /**
         * The hydraulic head.
         * 
         * <p>Elevation + Pressure head of water in the reservoir.
         */
        HEAD(HEAD_STR, Double.class), //
        /**
         * Head pattern
         * 
         * <p>A head pattern can be used to make the reservoir 
         * head vary with time.
         */
        HEAD_PATTERN(PATTERN_STR, String.class);

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

        public String getPrefix() {
            return RESERVOIRS_PRE;
        }
    }

    /**
     * The {@link Pumps} attributes and classes.
     */
    public static enum Pumps implements IEpanetType {
        /**
         * Unique id of the junction. 
         */
        ID(ID_STR, String.class), //
        /**
         * Start node.
         */
        START_NODE(STARTNODE_STR, String.class), //
        /**
         * End node.
         */
        END_NODE(ENDNODE_STR, String.class), //
        /**
         * Power value for constant energy pump [KW].
         */
        POWER(POWER_STR, String.class), //
        /**
         * Id of curve that describes head vs. flow for the pump.
         */
        HEAD_ID(HEAD_ID_STR, String.class), //
        /**
         * Relative speed. 
         * 
         * <p>Normal is 1, 0 means pump is off.
         */
        SPEED(SPEED_STR, String.class), //
        /**
         * Id of time pattern that describes how speed varies with time.
         */
        SPEED_PATTERN(PATTERN_STR, String.class), //
        /**
         * The average cost per KWh.
         */
        PRICE(PRICE_STR, Double.class), //
        /**
         * The id of the time pattern for energy price.
         */
        PRICE_PATTERN(PRICE_PATTERN_STR, String.class), //
        /**
         * The id of the efficiency curve.
         */
        EFFICIENCY(EFFICIENCY_ID_STR, String.class);

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

        public String getPrefix() {
            return PUMPS_PRE;
        }
    }

    /**
     * The {@link Valves} attributes and classes.
     */
    public static enum Valves implements IEpanetType {
        /**
         * Unique id of the junction. 
         */
        ID(ID_STR, String.class), //
        /**
         * Start node.
         */
        START_NODE(STARTNODE_STR, String.class), //
        /**
         * End node.
         */
        END_NODE(ENDNODE_STR, String.class), //
        /**
         * Diameter.
         */
        DIAMETER(DIAMETER_STR, Double.class), //
        /**
         * The valve type.
         * 
         * <p>Can be one of:
         * <ul>
         * <li> {@link LinkTypes#EN_PRV}</li>
         * <li>{@link LinkTypes#EN_PSV}</li>
         * <li>{@link LinkTypes#EN_PBV}</li>
         * <li>{@link LinkTypes#EN_FCV}</li>
         * <li>{@link LinkTypes#EN_TCV}</li>
         * <li>{@link LinkTypes#EN_GPV}</li>
         * </ul>
         */
        TYPE(TYPE_STR, String.class), //
        /**
         * The valve setting.
         * 
         * <p>Can be one of:
         * <ul>
         * <li> {@link LinkTypes#EN_PRV#getSetting()}</li>
         * <li>{@link LinkTypes#EN_PSV#getSetting()}</li>
         * <li>{@link LinkTypes#EN_PBV#getSetting()}</li>
         * <li>{@link LinkTypes#EN_FCV#getSetting()}</li>
         * <li>{@link LinkTypes#EN_TCV#getSetting()}</li>
         * <li>{@link LinkTypes#EN_GPV#getSetting()}</li>
         * </ul>
         */
        SETTING(SETTING_STR, String.class), //
        /**
         * Minor loss coefficient.
         */
        MINORLOSS(MINORLOSS_STR, Double.class),
        /**
         * Status.
         * 
         * <p>Status can be: OPEN, CLOSED.
         */
        STATUS(STATUS_STR, String.class);

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

        public String getPrefix() {
            return VALVES_PRE;
        }
    }

    /**
     * The {@link Pipes} attributes and classes.
     */
    public static enum Pipes implements IEpanetType {
        /**
         * Unique id of the junction. 
         */
        ID(ID_STR, String.class), //
        /**
         * Start node.
         */
        START_NODE(STARTNODE_STR, String.class), //
        /**
         * End node.
         */
        END_NODE(ENDNODE_STR, String.class), //
        /**
         * Length.
         */
        LENGTH(LENGTH_STR, Double.class), //
        /**
         * Diameter.
         */
        DIAMETER(DIAMETER_STR, Double.class), //
        /**
         * Roughness coefficient.
         */
        ROUGHNESS(ROUGHNESS_STR, Double.class), //

        /**
         * Demand.
         */
        DEMAND(DEMAND_STR, Double.class), //

        /**
         * Id of Pattern.
         */
        PATTERN(PATTERN_STR, String.class), //

        /**
         * Leak Coefficient.
         */
        LEAKCOEFF(LEAKCOEFF_STR, Double.class), //

        /**
         * Minor loss coefficient.
         */
        MINORLOSS(MINORLOSS_STR, Double.class), //
        /**
         * Status.
         * 
         * <p>Status can be: OPEN, CLOSED or CV.
         * <p>Setting status to CV means that the pipe contains a
         * check valve restricting flow to one direction.
         */
        STATUS(STATUS_STR, String.class);

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

        public String getPrefix() {
            return PIPES_PRE;
        }
    }

}
