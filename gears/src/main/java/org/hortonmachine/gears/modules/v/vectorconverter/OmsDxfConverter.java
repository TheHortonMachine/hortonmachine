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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_LINE_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_POINTS_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_POLYGON_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_P_CODE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_STATUS;

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
import org.hortonmachine.gears.io.dxfdwg.libs.dxf.DxfFile;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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

    private CoordinateReferenceSystem crs;

    @Execute
    public void process() throws Exception {
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
                crs = CrsUtilities.getCrsFromEpsg(pCode, null);
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
