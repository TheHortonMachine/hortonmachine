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
import oms3.annotations.Documentation;
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

@Description("Puts the values of a field of polygons into a raster: the cells inside the polygons get the value of their polygon, or, with the add option, the value is added to the one of the cell, as the heights of the buildings onto an elevation model.")
@Documentation("""
        The polygons are rasterized on the grid of the raster with ScanLineRasterizer: the cells whose center is inside a polygon are changed, the others are kept. The field must be numeric, and the polygons in the CRS of the raster.

        By default the value of the field replaces the one of the cell, as to set the elevation of a lake or of a flat area. With the add option, it is added to it: with the heights of the buildings, an elevation model of the terrain becomes one of the surface with the buildings extruded, for the analyses of visibility, shadows or flooding in urban areas. The novalues of the raster stay novalue.
        """)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("vector, raster, add, extrude, buildings")
@Label(HMConstants.RASTERPROCESSING)
@Name("rasterextruder")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class RasterExtruder extends HMModel {

    @Description("The raster to modify, as an elevation model.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description("The polygons with the values.")
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector;

    @Description("The numeric field of the polygons with their value.")
    @In
    public String pAddingField;

    @Description("If true, the value of the polygon is added to the one of the cell, else it replaces it.")
    @In
    public boolean doAdd = false;

    @Description("The modified raster.")
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
        r.pm = pm;
        r.process();
        GridCoverage2D outGC = r.outRaster;

        GridCoverage2D resultGC;
        if (doAdd) {
            resultGC = CoverageUtilities.sumCoverages(outGC, dtm);
        } else {
            resultGC = CoverageUtilities.mergeCoverages(outGC, dtm);
        }
        dumpRaster(resultGC, outRaster);
    }

}
