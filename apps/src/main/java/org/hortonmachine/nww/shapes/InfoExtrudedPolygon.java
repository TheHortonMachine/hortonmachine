package org.hortonmachine.nww.shapes;

import gov.nasa.worldwind.render.ExtrudedPolygon;

public class InfoExtrudedPolygon extends ExtrudedPolygon implements IInfoShape{
    private String info;

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
