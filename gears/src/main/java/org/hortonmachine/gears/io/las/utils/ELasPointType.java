package org.hortonmachine.gears.io.las.utils;

public enum ELasPointType {
    UNCLASSIFIED(0, "UNCLASSIFIED"), //
    UNASSIGNED(1, "UNASSIGNED"), //
    GROUND(2, "GROUND"), //
    VEGETATION_MIN(3, "LOW VEGETATION"), //
    VEGETATION_MED(4, "MEDIUM VEGETATION"), //
    VEGETATION_MAX(5, "HIGH VEGETATION"), //
    BUILDING(6, "BUILDING"), //
    LOW_POINT(7, "LOW POINT (NOISE)"), //
    MASS_POINT(8, "MODEL KEY-POINT (MASS)"), //
    WATER(9, "WATER"), //
    RAIL(10, "RAIL"), //
    ROAD_SURFACE(11, "ROAD SURFACE"), //
    OVERLAP(12, "OVERLAP"),//
    WIRE_GUARD(13, "WIRE - GUARD");

    private String label;
    private int value;

    ELasPointType( int value, String label ) {
        this.value = value;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public int getValue() {
        return value;
    }
}