package org.hortonmachine.nww.shapes;

import gov.nasa.worldwind.render.Polygon;

public class InfoPolygon extends Polygon implements IInfoShape{
    private String info;

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
