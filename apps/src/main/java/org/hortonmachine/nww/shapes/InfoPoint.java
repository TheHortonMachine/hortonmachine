package org.hortonmachine.nww.shapes;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.markers.BasicMarker;
import gov.nasa.worldwind.render.markers.MarkerAttributes;

public class InfoPoint extends BasicMarker implements IInfoShape {
    private String info;

    public InfoPoint( Position position, MarkerAttributes attrs ) {
        super(position, attrs);
    }

    public String getInfo() {
        return info;
    }

    public void setInfo( String info ) {
        this.info = info;
    }

}
