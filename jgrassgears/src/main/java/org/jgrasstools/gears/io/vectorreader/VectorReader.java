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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.jgrasstools.gears.io.properties.PropertiesFeatureReader;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;

@Description("Vectors features reader module.")
@Documentation("VectorReader.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("IO, Shapefile, Feature, Vector, Reading")
@Label(JGTConstants.FEATUREREADER)
@Name("vectorreader")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class VectorReader extends JGTModel {
    @Description("The vector type to read (Supported is: shp, properties).")
    @In
    // currently not used, for future compatibility
    public String pType = null;

    @Description("The vector file to read.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String file = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The read feature collection.")
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
            outVector = ShapefileFeatureReader.readShapefile(vectorFile.getAbsolutePath());
        } else if (name.toLowerCase().endsWith("properties")) {
            outVector = PropertiesFeatureReader.readPropertiesfile(vectorFile.getAbsolutePath());
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
        VectorReader reader = new VectorReader();
        reader.file = path;
        reader.process();
        SimpleFeatureCollection fc = reader.outVector;
        return fc;
    }

    /**
     * Fast reading of the bounds of a vector dataset.
     * 
     * @param path the vector file path.
     * @return the array containing the bounds as [n, s, e, w].
     * @throws IOException
     */
    public static double[] readBounds( String path ) throws IOException {
        SimpleFeatureCollection fc = getFC(path);
        ReferencedEnvelope bounds = fc.getBounds();

        double[] nsew = {//
        bounds.getMaxY(), //
                bounds.getMinY(), //
                bounds.getMaxX(), //
                bounds.getMinX() //
        };
        return nsew;
    }

}
