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
package org.hortonmachine.modules;

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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

@Description("Adds a value from a vector layer over a raster layer.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("vector, raster, add")
@Label(HMConstants.RASTERPROCESSING)
@Name("rasterextruder")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class RasterExtruder extends HMModel {

    @Description("The input raster to modify.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description("The input vector with the values to add.")
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector;

    @Description("The field containing the value to add from the vector map.")
    @In
    public String pAddingField;

    @Description("The output raster.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, inVector, pAddingField);

        GridCoverage2D dtm = getRaster(inRaster);
        SimpleFeatureCollection vector = getVector(inVector);

        OmsScanLineRasterizer r = new OmsScanLineRasterizer();
        r.inRaster = dtm;
        r.inVector = vector;
        r.fCat = pAddingField;
        r.process();
        GridCoverage2D outGC = r.outRaster;

        GridCoverage2D mergedGC = CoverageUtilities.mergeCoverages(outGC, dtm);
        dumpRaster(mergedGC, outRaster);
    }

}
