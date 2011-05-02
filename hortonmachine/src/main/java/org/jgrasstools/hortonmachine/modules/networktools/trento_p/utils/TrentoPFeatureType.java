package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;

import org.jgrasstools.hortonmachine.modules.networktools.trento_p.net.Pipe;

/**
 * The attributes for the creation feature.
 * 
 * @author Daniele Andreis (www.hydrologis.com)
 * 
 */
@SuppressWarnings("nls")
public class TrentoPFeatureType {
    public final static String PIPE = "Pipe";

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
     * The field of the initial elevation of this pipe this pipe, in the inPipes
     * featureCollections.
     */

    public final static String INITIAL_ELEVATION_STR = "initialZ";
    /**
     * The field of the final elevation of this pipe, in the inPipes
     * featureCollections.
     */

    public final static String FINAL_ELEVATION_STR = "finalZ";
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

    public final static String DIAMETER_TO_VERIFY_STR = "D Verify";
    /**
     * The field of slope to verify, only for verify mode, the inPipes
     * featureCollections.
     */

    public final static String VERIFY_PIPE_SLOPE_STR = "Slope ver";
    /**
     * The field of the height of the free surface at the begin of the pipe.
     */
    public final static String INITIAL_FREE_SURFACE_STR = "InFreeSurf";
    /**
     * The field of the height of the free surface at the end of the pipe.
     */
    public final static String FINAL_FREE_SURFACE_STR = "FFreeSurf";
    /**
     * The field of Empty degree of the pipe.
     */
    public final static String EMPTYDEGREE_STR = "empty deg";

    /**
     * The field of the depth of the dig at the end of the pipe.
     */
    public final static String DEPTH_FINAL_PIPE_STR = "fidepth";
    /**
     * The field of the depth of the dig at the begin of the pipe.
     */
    public final static String DEPTH_INITIAL_PIPE_STR = "in depth";
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
    public final static String T_QMAX_STR = "t Qmax";
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
    public final static String MEAN_SPEED_STR = "mean speed";
    /**
     * Residence time.
     */
    public final static String RESIDENCE_TIME_STR = "resTime";

    public final static String T_P_STR = "tP";

    /**
     * The {@link Pipe} attributes and classes.
     */
    public static enum PipesTrentoP implements ITrentoPType {
        ID(ID_STR, String.class),
        ID_PIPE_WHERE_DRAIN(ID_PIPE_WHERE_DRAIN_STR, String.class),
        DRAIN_AREA(DRAIN_AREA_STR, Float.class),
        INITIAL_ELEVATION(INITIAL_ELEVATION_STR, Float.class),
        FINAL_ELEVATION (FINAL_ELEVATION_STR, Float.class),
        RUNOFF_COEFFICIENT(RUNOFF_COEFFICIENT_STR, Float.class),
        AVERAGE_RESIDENCE_TIME (AVERAGE_RESIDENCE_TIME_STR, Float.class),
        KS  (KS_STR, Float.class),
        MINIMUM_PIPE_SLOPE  (MINIMUM_PIPE_SLOPE_STR, Float.class),
        PIPE_SECTION_TYPE(PIPE_SECTION_TYPE_STR, Integer.class),
        AVERAGE_SLOPE(AVERAGE_SLOPE_STR, Float.class), 
        DIAMETER_TO_VERIFY   (DIAMETER_TO_VERIFY_STR, Float.class), 
        VERIFY_PIPE_SLOPE (VERIFY_PIPE_SLOPE_STR, Float.class),
        DISCHARGE(DISCHARGE_STR, Float.class), 
        COEFF_UDOMETRICO(COEFF_UDOMETRICO_STR, Float.class), 
        RESIDENCE_TIME(RESIDENCE_TIME_STR, Float.class), 
        T_P(T_P_STR, Float.class), 
        T_QMAX(T_QMAX_STR, Float.class), 
        MEAN_SPEED (MEAN_SPEED_STR, Float.class), 
        PIPE_SLOPE(PIPE_SLOPE_STR, Float.class),
        DIAMETER (DIAMETER_STR, Float.class),
        EMPTYDEGREE(EMPTYDEGREE_STR, Float.class),
        DEPTH_INITIAL_PIPE(DEPTH_INITIAL_PIPE_STR, Float.class),
        DEPTH_FINAL_PIPE (DEPTH_FINAL_PIPE_STR, Float.class),
        INITIAL_FREE_SURFACE(INITIAL_FREE_SURFACE_STR, Float.class),
        FINAL_FREE_SURFACE (FINAL_FREE_SURFACE_STR, Float.class); 

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

}
