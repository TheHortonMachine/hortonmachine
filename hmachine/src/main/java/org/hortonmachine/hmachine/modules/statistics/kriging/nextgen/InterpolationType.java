package org.hortonmachine.hmachine.modules.statistics.kriging.nextgen;

public enum InterpolationType {
    NODATA(0),
    NOINTERPOLATION_USE_RAW_DATA(1),
    INTERPOLATION_IDW(2),
    INTERPOLATION_KRIGING(3);

    private int code;

    InterpolationType( int code ) {
        this.code = code;
    }
}
