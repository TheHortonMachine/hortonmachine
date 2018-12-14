/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.gears.ui;

import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.hortonmachine.gears.utils.style.StyleUtilities;

/**
 * A simple map frame where layers can be set or added with default styles.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class HMMapframe extends JMapFrame {

    private MapContent content;

    public HMMapframe( String title ) {
        super();
        content = new MapContent();
        content.setTitle(title);
        setMapContent(content);
    }

    public void addLayer( SimpleFeatureCollection featureCollection ) {
        FeatureLayer fl = makeFeatureLayer(featureCollection);
        addLayer(fl);
    }

    public void setLayer( SimpleFeatureCollection featureCollection ) {
        FeatureLayer fl = makeFeatureLayer(featureCollection);
        setLayer(fl);
    }

    public void addLayer( SimpleFeatureCollection featureCollection, Style style ) {
        FeatureLayer fl = new FeatureLayer(featureCollection, style);
        addLayer(fl);
    }
    
    public void setLayer( SimpleFeatureCollection featureCollection, Style style ) {
        FeatureLayer fl = new FeatureLayer(featureCollection, style);
        setLayer(fl);
    }

    public void addLayer( Layer layer ) {
        content.addLayer(layer);
    }

    public void setLayer( Layer layer ) {
        List<Layer> layers = content.layers();
        for( Layer l : layers ) {
            content.removeLayer(l);
        }
        content.addLayer(layer);
    }

    private FeatureLayer makeFeatureLayer( SimpleFeatureCollection featureCollection ) {
        Style style = StyleUtilities.createDefaultStyle(featureCollection);
        FeatureLayer fl = new FeatureLayer(featureCollection, style);
        return fl;
    }

}
