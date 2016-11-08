/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrasstools.server.jetty.providers.data;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.utils.style.SimpleStyle;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Interface for server data providers.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface NwwDataProvider {

    public boolean isPoints();
    
    public boolean isLines();
    
    public boolean isPolygon();

    public String getName();
    
    public SimpleStyle getStyle() throws Exception;
    
    public int size();
    
    public Geometry getGeometryAt(int index);

    public String getLabelAt(int index);
    
    public Envelope getBounds();
    
    public String asGeoJson() throws Exception;
    
    public SimpleFeatureCollection subCollection(String cqlFilter) throws Exception;
}
