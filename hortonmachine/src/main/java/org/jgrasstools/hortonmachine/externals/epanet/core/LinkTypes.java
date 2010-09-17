package org.jgrasstools.hortonmachine.externals.epanet.core;

@SuppressWarnings("nls")
public enum LinkTypes {
    EN_CVPIPE(0, "Pipe with Check Valve", ""), //
    EN_PIPE(1, "Pipe", ""), //
    EN_PUMP(2, "Pump", ""), //
    EN_PRV(3, "Pressure Reducing Valve", "Pressure, psi (m)"), //
    EN_PSV(4, "Pressure Sustaining Valve", "Pressure, psi (m)"), //
    EN_PBV(5, "Pressure Breaker Valve", "Pressure, psi (m)"), //
    EN_FCV(6, "Flow Control Valve", "Flow (flow unit)"), //
    EN_TCV(7, "Throttle Control Valve", "Loss Coefficient"), //
    EN_GPV(8, "General Purpose Valve", "ID of head loss curve");

    private int code;
    private String description;
    private String setting;
    LinkTypes( int code, String description , String setting) {
        this.code = code;
        this.description = description;
        this.setting = setting;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
    
    public String getSetting() {
        return setting;
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
