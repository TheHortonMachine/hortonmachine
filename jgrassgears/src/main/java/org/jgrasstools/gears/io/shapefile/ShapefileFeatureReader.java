/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description("Utility class for reading shapefiles to geotools featurecollections.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Shapefile, Feature, Vector, Reading")
@Label(JGTConstants.FEATUREREADER)
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class ShapefileFeatureReader extends JGTModel {
    @Description("The shapefile.")
    @UI(JGTConstants.FILE_UI_HINT)
    @In
    public String file = null;

    @Description("The read feature collection.")
    @Out
    public SimpleFeatureCollection geodata = null;

    @Execute
    public void readFeatureCollection() throws IOException {
        if (!concatOr(geodata == null, doReset)) {
            return;
        }
        File shapeFile = new File(file);
        ShapefileDataStore store = new ShapefileDataStore(shapeFile.toURI().toURL());
        SimpleFeatureSource featureSource = store.getFeatureSource();
        geodata = featureSource.getFeatures();
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
