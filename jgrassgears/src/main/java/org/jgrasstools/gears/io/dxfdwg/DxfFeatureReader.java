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
package org.jgrasstools.gears.io.dxfdwg;

import java.io.File;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.data.PrjFileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.io.dxfdwg.libs.dxf.DxfFile;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("Utility class for reading dxf files to geotools featurecollections (based on Michael Michauds work).")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, DXF, Feature, Vector, Reading")
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class DxfFeatureReader extends JGTModel {
    @Description("The dxf file.")
    @In
    public String file = null;

    @Description("The code defining the coordinate reference system, composed by authority and code number (ex. EPSG:4328). Applied in the case the file is missing.")
    @In
    public String pCode;

    @Description("The read point feature collection.")
    @Out
    public SimpleFeatureCollection pointsFC = null;

    @Description("The read lines feature collection.")
    @Out
    public SimpleFeatureCollection lineFC = null;

    @Description("The read polygons feature collection.")
    @Out
    public SimpleFeatureCollection polygonFC = null;

    private CoordinateReferenceSystem crs;

    @Execute
    public void readFeatureCollection() throws Exception {
        if (!concatOr(pointsFC == null, lineFC == null, polygonFC == null, doReset)) {
            return;
        }

        File dxfFile = new File(file);
        File parentFolder = dxfFile.getParentFile();
        String nameWithoutExtention = FileUtilities.getNameWithoutExtention(dxfFile);
        File prjFile = new File(parentFolder, nameWithoutExtention + ".prj");
        if (prjFile.exists()) {
            FileInputStream instream = new FileInputStream(prjFile);
            final FileChannel channel = instream.getChannel();
            PrjFileReader reader = new PrjFileReader(channel);
            crs = reader.getCoordinateReferenceSystem();
        }
        if (crs == null) {
            if (pCode != null) {
                crs = CRS.decode(pCode);
            } else {
                throw new ModelsIllegalargumentException("Please specify the CRS for the imported DXF file.", this);
            }
        }

        DxfFile dxf = DxfFile.createFromFile(dxfFile, crs);

        pointsFC = dxf.getPoints();
        lineFC = dxf.getLines();
        polygonFC = dxf.getPolygons();

    }

}
