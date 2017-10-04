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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_ATTRIBUTES_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_CONTOUR_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_LINE_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_POINTS_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_POLYGON_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_P_CODE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_STATUS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDWGCONVERTER_TEXT_VECTOR_DESCRIPTION;

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
import org.hortonmachine.gears.io.dxfdwg.libs.DwgHandler;
import org.hortonmachine.gears.io.dxfdwg.libs.DwgReader;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description(OMSDWGCONVERTER_DESCRIPTION)
@Documentation(OMSDWGCONVERTER_DOCUMENTATION)
@Author(name = OMSDWGCONVERTER_AUTHORNAMES, contact = OMSDWGCONVERTER_AUTHORCONTACTS)
@Keywords(OMSDWGCONVERTER_KEYWORDS)
@Label(OMSDWGCONVERTER_LABEL)
@Name(OMSDWGCONVERTER_NAME)
@Status(OMSDWGCONVERTER_STATUS)
@License(OMSDWGCONVERTER_LICENSE)
public class OmsDwgConverter extends HMModel {

    @Description(OMSDWGCONVERTER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String file = null;

    @Description(OMSDWGCONVERTER_P_CODE_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSDWGCONVERTER_POINTS_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection pointsVector = null;

    @Description(OMSDWGCONVERTER_LINE_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection lineVector = null;

    @Description(OMSDWGCONVERTER_POLYGON_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection polygonVector = null;

    @Description(OMSDWGCONVERTER_TEXT_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection textVector;

    @Description(OMSDWGCONVERTER_ATTRIBUTES_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection attributesVector;

    @Description(OMSDWGCONVERTER_CONTOUR_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection contourVector;

    private CoordinateReferenceSystem crs;

    @Execute
    public void readFeatureCollection() throws Exception {
        if (!concatOr(pointsVector == null, lineVector == null, polygonVector == null, doReset)) {
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
                crs = CrsUtilities.getCrsFromEpsg(pCode, null);
            } else {
                throw new ModelsIllegalargumentException("Please specify the CRS for the imported DWG file.", this, pm);
            }
        }

        DwgHandler dataHandler = new DwgHandler(dwgFile, crs);
        dataHandler.getLayerTypes();
        DwgReader dwgReader = dataHandler.getDwgReader();

        textVector = dwgReader.getTextFeatures();
        attributesVector = dwgReader.getAttributesFeatures();
        contourVector = dwgReader.getContourFeatures();
        pointsVector = dwgReader.getMultiPointFeatures();
        lineVector = dwgReader.getMultiLineFeatures();
        polygonVector = dwgReader.getMultiPolygonFeatures();

    }

}
