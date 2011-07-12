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
package org.jgrasstools.gears.io.shapefile;

import java.io.File;
import java.io.IOException;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.indexed.IndexedShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;

@Description("Utility class for reading shapefiles to geotools featurecollections.")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("IO, Shapefile, Feature, Vector, Reading")
@Label(JGTConstants.FEATUREREADER)
@Status(Status.CERTIFIED)
@UI(JGTConstants.HIDE_UI_HINT)
@License("General Public License Version 3 (GPLv3)")
public class ShapefileFeatureReader extends JGTModel {
    @Description("The shapefile.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String file = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The read feature collection.")
    @Out
    public SimpleFeatureCollection geodata = null;

    @Execute
    public void readFeatureCollection() throws IOException {
        if (!concatOr(geodata == null, doReset)) {
            return;
        }

        try {
            File shapeFile = new File(file);
            pm.beginTask("Reading features from shapefile: " + shapeFile.getName(), -1);
            IndexedShapefileDataStore   store = new IndexedShapefileDataStore(shapeFile.toURI().toURL());
            SimpleFeatureSource featureSource = store.getFeatureSource();
            geodata = featureSource.getFeatures();
        } finally {
            pm.done();
        }
    }

    /**
     * Fast read access mode. 
     * 
     * @param path the shapefile path.
     * @return the read {@link FeatureCollection}.
     * @throws IOException
     */
    public static SimpleFeatureCollection readShapefile( String path ) throws IOException {

        ShapefileFeatureReader reader = new ShapefileFeatureReader();
        reader.file = path;
        reader.readFeatureCollection();

        return reader.geodata;
    }

    
  
    
    
}
