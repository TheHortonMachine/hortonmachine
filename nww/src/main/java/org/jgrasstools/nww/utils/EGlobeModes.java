package org.jgrasstools.nww.utils;

public enum EGlobeModes {
    Earth("Earth"), FlatEarth("Flat Earth (lat/long)"), FlatEarthMercator("Flat Earth (Mercator)");

    private String description;

    private EGlobeModes(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static EGlobeModes getModeFromDescription(String description) {
        for (EGlobeModes mode : values()) {
            if (mode.getDescription().equals(description)) {
                return mode;
            }
        }
        return EGlobeModes.Earth;
    }

    public static String[] getModesDescriptions() {
        return new String[] { Earth.description, FlatEarthMercator.description, FlatEarth.description };
    }

}
