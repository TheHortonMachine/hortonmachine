package org.jgrasstools.server.jetty.map;

import org.jgrasstools.gears.utils.style.SimpleStyle;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public interface NwwDataProvider {
    
    public static final String PROVIDERS = "NWWPROVIDERS";

    public boolean isPoints();
    
    public boolean isLines();
    
    public boolean isPolygon();
    
    public String getName();
    
    public SimpleStyle getStyle() throws Exception;
    
    public int size();
    
    public Geometry getGeometryAt(int index);

    public String getLabelAt(int index);
    
    public Envelope getBounds();
}
