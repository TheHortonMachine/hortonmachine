package org.jgrasstools.hortonmachine.externals.epanet.core;

@SuppressWarnings("nls")
public enum TimeParameterCodes {
    EN_DURATION(0, "Simulation duration "), //
    EN_HYDSTEP(1, "Hydraulic time step "), //
    EN_QUALSTEP(2, "Water quality time step "), //
    EN_PATTERNSTEP(3, "Time pattern time step "), //
    EN_PATTERNSTART(4, "Time pattern start time "), //
    EN_REPORTSTEP(5, "Reporting time step "), //
    EN_REPORTSTART(6, "Report starting time "), //
    EN_RULESTEP(7, "Time step for evaluating rule-based controls "), //
    EN_STATISTIC(8, "Type of time series post-processing to use "), //
    EN_PERIODS(9, "Number of reporting periods saved to binary output file"), //

    EN_STATISTIC_EN_NONE(0, "none "), //
    EN_STATISTIC_EN_AVERAGE(1, "averaged "), //
    EN_STATISTIC_EN_MINIMUM(2, "minimums "), //
    EN_STATISTIC_EN_MAXIMUM(3, "maximums "), //
    EN_STATISTIC_EN_RANGE(4, "ranges ");

    private int code;
    private String description;
    TimeParameterCodes( int code, String description ) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TimeParameterCodes forCode( int i ) {
        TimeParameterCodes[] values = values();
        for( TimeParameterCodes type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }
}
