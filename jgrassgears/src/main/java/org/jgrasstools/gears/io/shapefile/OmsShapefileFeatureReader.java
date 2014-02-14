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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSSHAPEFILEFEATUREREADER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSHAPEFILEFEATUREREADER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSHAPEFILEFEATUREREADER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSHAPEFILEFEATUREREADER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSHAPEFILEFEATUREREADER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSHAPEFILEFEATUREREADER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSHAPEFILEFEATUREREADER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSHAPEFILEFEATUREREADER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSHAPEFILEFEATUREREADER_UI;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSHAPEFILEFEATUREREADER_file_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSSHAPEFILEFEATUREREADER_geodata_DESCRIPTION;

import java.io.File;
import java.io.IOException;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description(OMSSHAPEFILEFEATUREREADER_DESCRIPTION)
@Author(name = OMSSHAPEFILEFEATUREREADER_AUTHORNAMES, contact = OMSSHAPEFILEFEATUREREADER_AUTHORCONTACTS)
@Keywords(OMSSHAPEFILEFEATUREREADER_KEYWORDS)
@Label(OMSSHAPEFILEFEATUREREADER_LABEL)
@Name(OMSSHAPEFILEFEATUREREADER_NAME)
@Status(OMSSHAPEFILEFEATUREREADER_STATUS)
@License(OMSSHAPEFILEFEATUREREADER_LICENSE)
@UI(OMSSHAPEFILEFEATUREREADER_UI)
public class OmsShapefileFeatureReader extends JGTModel {

    @Description(OMSSHAPEFILEFEATUREREADER_file_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String file = null;

    @Description(OMSSHAPEFILEFEATUREREADER_geodata_DESCRIPTION)
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
            FileDataStore store = FileDataStoreFinder.getDataStore(shapeFile);
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

        OmsShapefileFeatureReader reader = new OmsShapefileFeatureReader();
        reader.file = path;
        reader.readFeatureCollection();

        return reader.geodata;
    }

}
