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
package org.jgrasstools.gears.io.vectorwriter;

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
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;

@Description("Vectors features writer to file module.")
@Documentation("VectorWriter.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("IO, Shapefile, Feature, Vector, Writing")
@Label(JGTConstants.FEATUREWRITER)
@Name("vectorwriter")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class VectorWriter extends JGTModel {
    @Description("The read feature collection.")
    @In
    public SimpleFeatureCollection geodata = null;

    @Description("The vector type to write (Supported is: shp).")
    @In
    // currently not used, for future compatibility
    public String pType = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The vector file to write.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @Out
    public String file = null;

    @Execute
    public void process() throws IOException {
        checkNull(file);

        File vectorFile = new File(file);
        String name = vectorFile.getName();
        if (name.toLowerCase().endsWith("shp") || pType.equals(JGTConstants.SHP)) {
            ShapefileFeatureWriter.writeShapefile(vectorFile.getAbsolutePath(), geodata);
        } else {
            throw new IOException("Format is currently not supported for file: " + name);
        }
    }

    /**
     * Fast write access mode. 
     * 
     * @param path the vector file path.
     * @param featureCollection the {@link FeatureCollection} to write.
     * @throws IOException
     */
    public static void writeVector( String path, SimpleFeatureCollection featureCollection ) throws IOException {
        VectorWriter writer = new VectorWriter();
        writer.file = path;
        writer.geodata = featureCollection;
        writer.process();
    }

}
