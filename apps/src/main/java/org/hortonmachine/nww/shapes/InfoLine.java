package org.hortonmachine.nww.shapes;

import java.util.List;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Path;

public class InfoLine extends Path implements IInfoShape {
    private String info;
    
    public InfoLine( List<Position> verticesList ) {
        super(verticesList);
    }

    public String getInfo() {
        return info;
    }

    public void setInfo( String info ) {
        this.info = info;
    }
}