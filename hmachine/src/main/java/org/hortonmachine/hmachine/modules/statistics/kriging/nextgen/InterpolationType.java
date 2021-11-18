package org.hortonmachine.hmachine.modules.statistics.kriging.nextgen;

public enum InterpolationType {
    /**
     * no data available, return no data in target points
     */
    NODATA(0),
    /**
     * all data are the same or there is just one data, use the raw
     */
    NOINTERPOLATION_USE_RAW_DATA(1),
    /**
     * only 2 stations available or only 2 values are non zero, use inverse weight distance
     */
    INTERPOLATION_IDW(2),
    INTERPOLATION_KRIGING(3);

    private int code;

    InterpolationType( int code ) {
        this.code = code;
    }
}
