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
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.data.PrjFileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.io.dxfdwg.libs.DwgHandler;
import org.jgrasstools.gears.io.dxfdwg.libs.DwgReader;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("Utility class for reading dwg files to geotools featurecollections (based on jdwglib project).")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, DWG, Feature, Vector, Reading")
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class DwgFeatureReader extends JGTModel {
    @Description("The dwg file.")
    @Label("file")
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

    @Description("The read text feature collection.")
    @Out
    public SimpleFeatureCollection textFC;

    @Description("The read attributes feature collection.")
    @Out
    public SimpleFeatureCollection attributesFC;

    @Description("The read contour feature collection.")
    @Out
    public SimpleFeatureCollection contourFC;

    private CoordinateReferenceSystem crs;

    @Execute
    public void readFeatureCollection() throws Exception {
        if (!concatOr(pointsFC == null, lineFC == null, polygonFC == null, doReset)) {
            return;
        }

        File dwgFile = new File(file);
        File parentFolder = dwgFile.getParentFile();
        String nameWithoutExtention = FileUtilities.getNameWithoutExtention(dwgFile);
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

        DwgHandler dataHandler = new DwgHandler(dwgFile, crs);
        dataHandler.getLayerTypes();
        DwgReader dwgReader = dataHandler.getDwgReader();

        textFC = dwgReader.getTextFeatures();
        attributesFC = dwgReader.getAttributesFeatures();
        contourFC = dwgReader.getContourFeatures();
        pointsFC = dwgReader.getMultiPointFeatures();
        lineFC = dwgReader.getMultiLineFeatures();
        polygonFC = dwgReader.getMultiPolygonFeatures();

    }

}
