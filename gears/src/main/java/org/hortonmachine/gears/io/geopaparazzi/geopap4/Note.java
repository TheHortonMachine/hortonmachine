package org.hortonmachine.gears.io.geopaparazzi.geopap4;

/**
 * Represents a simple geopap note.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class Note {
    public String simpleText;
    public String description;
    public long timeStamp;
    public long id;
    public double lon;
    public double lat;
    public double altim;
    
    
    @Override
    public String toString() {
        return simpleText;
    }
}