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

package org.jgrasstools.modules;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.r.tmsgenerator.OmsTmsGenerator;
import org.jgrasstools.gears.utils.files.FileUtilities;

@Description("A map creator for geopaparazzi.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("geopaparazzi, maps")
@Label(JGTConstants.MOBILE)
@Name("geopaparazzimapscreator")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class GeopaparazziMapsCreator extends JGTModel {
    @Description("Area of interest shapefile.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inROI = null;

    @Description("Zoom limit area shapefile.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inZoomLimitROI = null;

    @Description("Optional input raster map or image mosaic.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster = null;

    @Description("Optional input vector map 1.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVector1 = null;

    @Description("Optional input vector map 2.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVector2 = null;

    @Description("Optional input vector map 3.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVector3 = null;

    @Description("Optional input vector map 4.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVector4 = null;

    @Description("Optional input vector map 5.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVector5 = null;

    @Description("Dataset name")
    @In
    public String pName = "newdataset";

    @Description("Min zoom level.")
    @In
    public int pMinZoom = 13;

    @Description("Max zoom level.")
    @In
    public int pMaxZoom = 19;

    @Description("Zoom limit.")
    @In
    public int pZoomLimit = 19;

    @Description("Zoom limit.")
    @In
    @UI("combo: png,jpg")
    public String pImageType = "png";

    @Description("The output folder.")
    @UI(JGTConstants.FOLDEROUT_UI_HINT)
    @In
    public String outFolder = null;

    @Execute
    public void process() throws Exception {
        checkNull(inROI, outFolder);

        SimpleFeatureCollection boundsVector = OmsVectorReader.readVector(inROI);
        ReferencedEnvelope bounds = boundsVector.getBounds();
        // bounds.expandBy(50.0);

        OmsTmsGenerator gen = new OmsTmsGenerator();
        if (inRaster != null) {
            List<String> inRasters = new ArrayList<String>();
            inRasters.add(inRaster);
            gen.inRasterFile = FileUtilities.stringListAsTmpFile(inRasters).getAbsolutePath();
        }
        if (inVector1 != null || inVector2 != null || inVector3 != null || inVector4 != null || inVector5 != null) {
            List<String> inVectors = new ArrayList<String>();
            if (inVector1 != null)
                inVectors.add(inVector1);
            if (inVector2 != null)
                inVectors.add(inVector2);
            if (inVector3 != null)
                inVectors.add(inVector3);
            if (inVector4 != null)
                inVectors.add(inVector4);
            if (inVector5 != null)
                inVectors.add(inVector5);
            gen.inVectorFile = FileUtilities.stringListAsTmpFile(inVectors).getAbsolutePath();
        }
        gen.pMinzoom = pMinZoom;
        gen.pMaxzoom = pMaxZoom;
        gen.pName = pName;
        gen.inPath = outFolder;
        gen.pWest = bounds.getMinX();
        gen.pEast = bounds.getMaxX();
        gen.pNorth = bounds.getMaxY();
        gen.pSouth = bounds.getMinY();
        // gen.pEpsg = "EPSG:32632";
        gen.dataCrs = bounds.getCoordinateReferenceSystem();
        gen.doMbtiles = true;

        gen.inZoomLimitVector = inZoomLimitROI;
        gen.pZoomLimit = pZoomLimit;

        switch( pImageType ) {
        case "jpg":
            gen.pImagetype = 1;
            break;
        case "png":
        default:
            gen.pImagetype = 0;
            break;
        }
        gen.pm = pm;
        gen.process();

    }
}