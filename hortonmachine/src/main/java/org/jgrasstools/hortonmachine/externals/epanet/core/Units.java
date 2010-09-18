package org.jgrasstools.hortonmachine.externals.epanet.core;

@SuppressWarnings("nls")
public enum Units {
    CFS("CFS", "Cubic feet per second"), //
    GPM("GPM", "Gallons per minute"), //
    MGD("MGD", "Million gallons per day"), //
    IMGD("IMGD", "Imperial Million gallons per day"), //
    AFD("AFD", "Acre-feet per day"), //
    LPS("LPS", "Liters per second"), //
    LPM("LPM", "Liters per minute"), //
    MLD("MLD", "Million liters per day"), //
    CMH("CMH", "Cubic meters per hour"), //
    CMD("CMD", "Cubic meters per day");

    private String name;
    private String description;
    Units( String name, String description ) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
