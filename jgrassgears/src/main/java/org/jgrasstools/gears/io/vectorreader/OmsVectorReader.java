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
package org.jgrasstools.gears.io.vectorreader;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_file_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_outVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_pType_DESCRIPTION;

import java.io.File;
import java.io.IOException;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.jgrasstools.gears.io.properties.OmsPropertiesFeatureReader;
import org.jgrasstools.gears.io.shapefile.OmsShapefileFeatureReader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description(OMSVECTORREADER_DESCRIPTION)
@Documentation(OMSVECTORREADER_DOCUMENTATION)
@Author(name = OMSVECTORREADER_AUTHORNAMES, contact = OMSVECTORREADER_AUTHORCONTACTS)
@Keywords(OMSVECTORREADER_KEYWORDS)
@Label(OMSVECTORREADER_LABEL)
@Name(OMSVECTORREADER_NAME)
@Status(OMSVECTORREADER_STATUS)
@License(OMSVECTORREADER_LICENSE)
public class OmsVectorReader extends JGTModel {

    @Description(OMSVECTORREADER_pType_DESCRIPTION)
    @In
    // currently not used, for future compatibility
    public String pType = null;

    @Description(OMSVECTORREADER_file_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String file = null;

    @Description(OMSVECTORREADER_outVector_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector = null;

    @Execute
    public void process() throws IOException {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }

        checkNull(file);

        File vectorFile = new File(file);
        String name = vectorFile.getName();
        if (name.toLowerCase().endsWith("shp")) {
            OmsShapefileFeatureReader reader = new OmsShapefileFeatureReader();
            reader.file = vectorFile.getAbsolutePath();
            reader.pm = pm;
            reader.readFeatureCollection();
            outVector = reader.geodata;
        } else if (name.toLowerCase().endsWith("properties")) {
            outVector = OmsPropertiesFeatureReader.readPropertiesfile(vectorFile.getAbsolutePath());
        } else {
            throw new IOException("Format is currently not supported for file: " + name);
        }
    }

    /**
     * Fast read access mode. 
     * 
     * @param path the vector file path.
     * @return the read {@link FeatureCollection}.
     * @throws IOException
     */
    public static SimpleFeatureCollection readVector( String path ) throws IOException {
        SimpleFeatureCollection fc = getFC(path);
        return fc;
    }

    private static SimpleFeatureCollection getFC( String path ) throws IOException {
        OmsVectorReader reader = new OmsVectorReader();
        reader.file = path;
        reader.process();
        SimpleFeatureCollection fc = reader.outVector;
        return fc;
    }

    public static ReferencedEnvelope readEnvelope(String filePath) throws IOException {
        File shapeFile = new File(filePath);
        FileDataStore store = FileDataStoreFinder.getDataStore(shapeFile);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        return featureSource.getBounds();
    }
    

}
