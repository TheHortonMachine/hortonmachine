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
package org.hortonmachine.gears.modules.v.vectorconverter;

import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.modules.v.vectorconverter.OmsDxfConverter.OMSDXFCONVERTER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.vectorconverter.OmsDxfConverter.OMSDXFCONVERTER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.vectorconverter.OmsDxfConverter.OMSDXFCONVERTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorconverter.OmsDxfConverter.OMSDXFCONVERTER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.vectorconverter.OmsDxfConverter.OMSDXFCONVERTER_KEYWORDS;
import static org.hortonmachine.gears.modules.v.vectorconverter.OmsDxfConverter.OMSDXFCONVERTER_LABEL;
import static org.hortonmachine.gears.modules.v.vectorconverter.OmsDxfConverter.OMSDXFCONVERTER_LICENSE;
import static org.hortonmachine.gears.modules.v.vectorconverter.OmsDxfConverter.OMSDXFCONVERTER_NAME;
import static org.hortonmachine.gears.modules.v.vectorconverter.OmsDxfConverter.OMSDXFCONVERTER_STATUS;

import java.io.File;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.PrjFileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.dxfdwg.libs.dxf.DxfFile;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.files.FileUtilities;

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

@Description(OMSDXFCONVERTER_DESCRIPTION)
@Documentation(OMSDXFCONVERTER_DOCUMENTATION)
@Author(name = OMSDXFCONVERTER_AUTHORNAMES, contact = OMSDXFCONVERTER_AUTHORCONTACTS)
@Keywords(OMSDXFCONVERTER_KEYWORDS)
@Label(OMSDXFCONVERTER_LABEL)
@Name(OMSDXFCONVERTER_NAME)
@Status(OMSDXFCONVERTER_STATUS)
@License(OMSDXFCONVERTER_LICENSE)
public class OmsDxfConverter extends HMModel {

    @Description(OMSDXFCONVERTER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String file = null;

    @Description(OMSDXFCONVERTER_P_CODE_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSDXFCONVERTER_POINTS_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection pointsVector = null;

    @Description(OMSDXFCONVERTER_LINE_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection lineVector = null;

    @Description(OMSDXFCONVERTER_POLYGON_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection polygonVector = null;

    public static final String OMSDXFCONVERTER_DESCRIPTION = "Reads the points, lines and polygons of an ASCII DXF file of a CAD into three vectors.";
    public static final String OMSDXFCONVERTER_DOCUMENTATION = """
            The entities read are POINT and TEXT, as points, LINE and the open POLYLINE and LWPOLYLINE, as \
            lines, and the closed POLYLINE and LWPOLYLINE, as polygons. The other entities, as CIRCLE, ARC, \
            SPLINE, HATCH, MTEXT and the INSERT of the blocks, are ignored. The entities inside the \
            definitions of the blocks are read where they are defined, in the coordinates of the block, not \
            where the blocks are inserted in the drawing.

            The features have the fields of the DXF entity: LAYER, LTYPE (the line type), ELEVATION, \
            THICKNESS, COLOR (the AutoCAD color index), and for the texts TEXT, TEXT_HEIGHT and TEXT_STYLE. \
            A filter on LAYER, with VectorFilter, separates the layers of the drawing.

            The DXF has no crs: it is taken from a prj file with the same name next to the DXF or, without \
            it, from the given code. Only the ASCII DXF is read, not the binary one.
            """;
    public static final String OMSDXFCONVERTER_KEYWORDS = "IO, DXF, CAD, Feature, Vector, Reading";
    public static final String OMSDXFCONVERTER_LABEL = VECTORPROCESSING;
    public static final String OMSDXFCONVERTER_NAME = "dxfimport";
    public static final int OMSDXFCONVERTER_STATUS = 40;
    public static final String OMSDXFCONVERTER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSDXFCONVERTER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSDXFCONVERTER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSDXFCONVERTER_FILE_DESCRIPTION = "The dxf file.";
    public static final String OMSDXFCONVERTER_P_CODE_DESCRIPTION = "The code defining the coordinate reference system, composed by authority and code number (ex. EPSG:3003). Applied if there is no prj file next to the dxf.";
    public static final String OMSDXFCONVERTER_POINTS_VECTOR_DESCRIPTION = "The output point vector.";
    public static final String OMSDXFCONVERTER_LINE_VECTOR_DESCRIPTION = "The output lines vector.";
    public static final String OMSDXFCONVERTER_POLYGON_VECTOR_DESCRIPTION = "The output polygons vector.";

    private CoordinateReferenceSystem crs;

    @Execute
    public void process() throws Exception {
        if (!concatOr(pointsVector == null, lineVector == null, polygonVector == null, doReset)) {
            return;
        }
        checkNull(file);

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
                crs = HMCrsRegistry.INSTANCE.getCrs(pCode);
            } else {
                throw new ModelsIllegalargumentException("Please specify the CRS for the imported DXF file.", this, pm);
            }
        }

        DxfFile dxf = DxfFile.createFromFile(dxfFile, crs);

        pointsVector = dxf.getPoints();
        lineVector = dxf.getLines();
        polygonVector = dxf.getPolygons();

    }

}
