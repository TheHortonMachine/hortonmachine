package org.jgrasstools.hortonmachine.externals.epanet.core;

@SuppressWarnings("nls")
public enum TimeParameterCodesStatistic {
    STATISTIC_NONE(0, "NONE", ""), //
    STATISTIC_AVERAGE(1, "AVERAGED", ""), //
    STATISTIC_MINIMUM(2, "MINIMUMS", ""), //
    STATISTIC_MAXIMUM(3, "MAXIMUMS", ""), //
    STATISTIC_RANGE(4, "RANGES", "");

    private int code;
    private String key;
    private String description;
    TimeParameterCodesStatistic( int code, String key, String description ) {
        this.code = code;
        this.key = key;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public static TimeParameterCodesStatistic forCode( int i ) {
        TimeParameterCodesStatistic[] values = values();
        for( TimeParameterCodesStatistic type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }

    public static TimeParameterCodesStatistic forKey( String key ) {
        TimeParameterCodesStatistic[] values = values();
        for( TimeParameterCodesStatistic type : values ) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given key: " + key);
    }
}
