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
package org.hortonmachine.hmachine.modules.networktools.trento_p.utils;

import org.hortonmachine.hmachine.modules.networktools.trento_p.net.Pipe;

/**
 * The attributes for the creation feature.
 * 
 * @author Daniele Andreis (www.hydrologis.com)
 * 
 */
@SuppressWarnings("nls")
public class TrentoPFeatureType {
    public final static String PIPE = "Pipe";
    public final static String JUNCTION = "Junction";
    public final static String AREA = "Area";

    public final static String ID_STR = "ID";
    /**
     * The field of the ID where this pipe drains in the inPipes
     * featureCollections.
     */
    public final static String ID_PIPE_WHERE_DRAIN_STR = "IDdrain";
 
    /**
     * The field of the Area which drains in this pipe, in the inPipes
     * featureCollections.
     */
    public final static String DRAIN_AREA_STR = "Area";
    
    /**
     * The field of the elevation of the junction.
     */
    public final static String ELEVATION_STR = "elev";
    
    /**
     * The field of the depth of the junction.
     */
    public final static String DEPTH_STR = "depth";

    /**
     * The field of the forced area of the areas.
     */
    public final static String FORCEAREA_STR = "forcearea";
    
    /**
     * The field of the runoff coefficent in the inPipes featureCollections.
     */

    public final static String RUNOFF_COEFFICIENT_STR = "Runoff";
    /**
     * The field of the average residence time in the inPipes
     * featureCollections.
     */

    public final static String AVERAGE_RESIDENCE_TIME_STR = "AveResT";
    /**
     * The field of the G.S. coefficent the inPipes featureCollections.
     */

    public final static String KS_STR = "ks";
    /**
     * The field of the minimum pipe slope in the inPipes featureCollections.
     */

    public final static String MINIMUM_PIPE_SLOPE_STR = "MinSlope";
    /**
     * The field of the geometry section type in the inPipes featureCollections.
     */
    public final static String PIPE_SECTION_TYPE_STR = "Section";
    /**
     * The field of the average slope in the inPipes featureCollections.
     */

    public final static String AVERAGE_SLOPE_STR = "AveSlope";
    /**
     * The field of diameter to verify, only for verify mode, the inPipes
     * featureCollections.
     */

    public final static String DIAMETER_TO_VERIFY_STR = "D_Verify";
    /**
     * The field of slope to verify, only for verify mode, the inPipes
     * featureCollections.
     */

    public final static String VERIFY_PIPE_SLOPE_STR = "Slope_ver";
    /**
     * The field of the height of the free surface at the begin of the pipe.
     */
    public final static String INITIAL_FREE_SURFACE_STR = "DoFreeSurf";
    /**
     * The field of the height of the free surface at the end of the pipe.
     */
    public final static String FINAL_FREE_SURFACE_STR = "UpFreeSurf";
    /**
     * The field of Empty degree of the pipe.
     */
    public final static String EMPTYDEGREE_STR = "empty_deg";

    /**
     * The field of the depth of the dig at the end of the pipe.
     */
    public final static String DEPTH_FINAL_PIPE_STR = "upDepth";
    /**
     * The field of the depth of the dig at the begin of the pipe.
     */
    public final static String DEPTH_INITIAL_PIPE_STR = "DownDepth";
    /**
     * The field of the diameter of the pipe.
     */
    public final static String DIAMETER_STR = "diameter";
    /**
     * The field of the actually slope.
     */
    public final static String PIPE_SLOPE_STR = "pipeslope";
    /**
     * The field of the time at the maximum discharge.
     */
    public final static String T_QMAX_STR = "t_Qmax";
    /**
     * The field of the discharge.
     */
    public final static String DISCHARGE_STR = "discharge";
    /**
     * The field of the udometric coefficient.
     */
    public final static String COEFF_UDOMETRICO_STR = "cUdomet";
    /**
     * The field of the average speed into the pipe.
     */
    public final static String MEAN_SPEED_STR = "mean_speed";
    /**
     * Residence time.
     */
    public final static String RESIDENCE_TIME_STR = "resTime";
    /**
     * The time at the the maximum discharge.
     */
    public final static String T_P_STR = "tP";
    /**
     * Is the total area of the sub network which have as a outlet this pipe.
     */
    public final static String TOTAL_SUB_NET_AREA_STR = "totArea";
    /**
     * The total lenght of the upstream pipes.
     */
    public final static String TOTAL_SUB_NET_LENGTH_STR = "totlength";
    /**
     * Mean length of the pipes into the sub network.
     */
    public final static String MEAN_LENGTH_SUBNET_STR = "meanlength";
    /**
     * The variance in the mean length calculation.
     */
    public final static String VARIANCE_LENGTH_SUBNET_STR = "varLength";
    /**
     * The percentage of dry area.
     */
    public final static String PERCENTAGE_OF_DRY_AREA = "dryArea";

    
    /**
     * The {@link Pipe} attributes and classes.
     */
    public static enum PipesTrentoP implements ITrentoPType {
        ID(ID_STR, Integer.class), //
        ID_PIPE_WHERE_DRAIN(ID_PIPE_WHERE_DRAIN_STR, Integer.class), //
        RUNOFF_COEFFICIENT(RUNOFF_COEFFICIENT_STR, Double.class), //
        AVERAGE_RESIDENCE_TIME(AVERAGE_RESIDENCE_TIME_STR, Double.class), //
        KS(KS_STR, Double.class), //
        MINIMUM_PIPE_SLOPE(MINIMUM_PIPE_SLOPE_STR, Double.class), //
        PIPE_SECTION_TYPE(PIPE_SECTION_TYPE_STR, Integer.class), //
        AVERAGE_SLOPE(AVERAGE_SLOPE_STR, Double.class), //
        PER_AREA(PERCENTAGE_OF_DRY_AREA,Double.class ), //
        DISCHARGE(DISCHARGE_STR, Double.class), //
        COEFF_UDOMETRICO(COEFF_UDOMETRICO_STR, Double.class), //
        RESIDENCE_TIME(RESIDENCE_TIME_STR, Double.class), //
        T_P(T_P_STR, Double.class), //
        T_QMAX(T_QMAX_STR, Double.class), //
        MEAN_SPEED(MEAN_SPEED_STR, Double.class), //
        PIPE_SLOPE(PIPE_SLOPE_STR, Double.class), //
        DIAMETER(DIAMETER_STR, Double.class), //
        EMPTYDEGREE(EMPTYDEGREE_STR, Double.class), //
        DEPTH_INITIAL_PIPE(DEPTH_INITIAL_PIPE_STR, Double.class), //
        DEPTH_FINAL_PIPE(DEPTH_FINAL_PIPE_STR, Double.class), //
        INITIAL_FREE_SURFACE(INITIAL_FREE_SURFACE_STR, Double.class), //
        FINAL_FREE_SURFACE(FINAL_FREE_SURFACE_STR, Double.class), //
        TOTAL_SUB_NET_AREA(TOTAL_SUB_NET_AREA_STR, Double.class), //
        TOTAL_SUB_NET_LENGTH(TOTAL_SUB_NET_LENGTH_STR, Double.class),//
        MEAN_LENGTH_SUBNET(MEAN_LENGTH_SUBNET_STR, Double.class), //
        VARIANCE_LENGTH_SUBNET(VARIANCE_LENGTH_SUBNET_STR, Double.class);

        private Class< ? > clazz;

        private String attributeName;

        PipesTrentoP( String attributeName, Class< ? > clazz ) {
            this.attributeName = attributeName;
            this.clazz = clazz;
        }

        public Class< ? > getClazz() {
            return clazz;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public String getName() {
            return PIPE;
        }

    }

    /**
     * The junctions attributes and classes.
     */
    public static enum JunctionsTrentoP implements ITrentoPType {
        ID(ID_STR, Integer.class), //
        ELEVATION(ELEVATION_STR, Double.class), //
        DEPTH(DEPTH_STR, Double.class); //
        
        private Class< ? > clazz;
        
        private String attributeName;
        
        JunctionsTrentoP( String attributeName, Class< ? > clazz ) {
            this.attributeName = attributeName;
            this.clazz = clazz;
        }
        
        public Class< ? > getClazz() {
            return clazz;
        }
        
        public String getAttributeName() {
            return attributeName;
        }
        
        public String getName() {
            return JUNCTION;
        }
        
    }

    /**
     * The areas attributes and classes.
     */
    public static enum AreasTrentoP implements ITrentoPType {
        ID(ID_STR, Integer.class), //
        FORCEAREA(FORCEAREA_STR, Double.class); //
        
        private Class< ? > clazz;
        
        private String attributeName;
        
        AreasTrentoP( String attributeName, Class< ? > clazz ) {
            this.attributeName = attributeName;
            this.clazz = clazz;
        }
        
        public Class< ? > getClazz() {
            return clazz;
        }
        
        public String getAttributeName() {
            return attributeName;
        }
        
        public String getName() {
            return AREA;
        }
        
    }

}
