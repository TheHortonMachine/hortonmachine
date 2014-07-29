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
//    static final String BRIDGE_LENGTH = "LENGHT";
    static final String SLOPE = "slope";
    static final String AVGSLOPE = "slopeavg";
    
    static final String VOLUME = "volume";
    static final String MEDIAN = "median";
}
