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
package org.jgrasstools.gears.modules.v.vectorconverter;

import java.io.File;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;

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

import org.geotools.data.PrjFileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.io.dxfdwg.libs.dxf.DxfFile;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("Module to convert dxf files to geotools vecotors.")
@Documentation("OmsDxfConverter.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("IO, OmsDwgConverter, Feature, Vector, Reading")
@Label(JGTConstants.VECTORPROCESSING)
@Status(Status.CERTIFIED)
@Name("dxfimport")
@License("General Public License Version 3 (GPLv3)")
public class OmsDxfConverter extends JGTModel {
    @Description("The dxf file.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String file = null;

    @Description("The code defining the coordinate reference system, composed by authority and code number (ex. EPSG:4328). Applied in the case the file is missing.")
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description("The output point vector.")
    @Out
    public SimpleFeatureCollection pointsVector = null;

    @Description("The output lines vector.")
    @Out
    public SimpleFeatureCollection lineVector = null;

    @Description("The output polygons vector.")
    @Out
    public SimpleFeatureCollection polygonVector = null;

    private CoordinateReferenceSystem crs;

    @Execute
    public void readFeatureCollection() throws Exception {
        if (!concatOr(pointsVector == null, lineVector == null, polygonVector == null, doReset)) {
            return;
        }

        File dxfFile = new File(file);
        File parentFolder = dxfFile.getParentFile();
        String nameWithoutExtention = FileUtilities.getNameWithoutExtention(dxfFile);
        File prjFile = new File(parentFolder, nameWithoutExtention + ".prj");
        if (prjFile.exists()) {
            FileInputStream instream = null;
            try {
                instream = new FileInputStream(prjFile);
                final FileChannel channel = instream.getChannel();
                PrjFileReader reader = new PrjFileReader(channel);
                crs = reader.getCoordinateReferenceSystem();
            } finally {
                if (instream != null)
                    instream.close();
            }
        }
        if (crs == null) {
            if (pCode != null) {
                crs = CRS.decode(pCode);
            } else {
                throw new ModelsIllegalargumentException("Please specify the CRS for the imported DXF file.", this);
            }
        }

        DxfFile dxf = DxfFile.createFromFile(dxfFile, crs);

        pointsVector = dxf.getPoints();
        lineVector = dxf.getLines();
        polygonVector = dxf.getPolygons();

    }

}
