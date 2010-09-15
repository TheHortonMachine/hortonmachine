package org.jgrasstools.hortonmachine.externals.epanet.core;

@SuppressWarnings("nls")
public enum LinkTypes {
    EN_CVPIPE(0, "Pipe with Check Valve"), //
    EN_PIPE(1, "Pipe"), //
    EN_PUMP(2, "Pump"), //
    EN_PRV(3, "Pressure Reducing Valve"), //
    EN_PSV(4, "Pressure Sustaining Valve"), //
    EN_PBV(5, "Pressure Breaker Valve"), //
    EN_FCV(6, "Flow Control Valve"), //
    EN_TCV(7, "Throttle Control Valve"), //
    EN_GPV(8, "General Purpose Valve");

    private int code;
    private String description;
    LinkTypes( int code, String description ) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static LinkTypes forCode( int i ) {
        LinkTypes[] values = values();
        for( LinkTypes type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }
}
