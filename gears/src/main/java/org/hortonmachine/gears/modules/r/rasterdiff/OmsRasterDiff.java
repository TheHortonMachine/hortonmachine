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
package org.hortonmachine.gears.modules.r.rasterdiff;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_KEYWORDS;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_LABEL;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_LICENSE;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_NAME;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_STATUS;

import java.util.Arrays;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.mapcalc.OmsMapcalc;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

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

@Description(OMSRASTERDIFF_DESCRIPTION)
@Documentation(OMSRASTERDIFF_DOCUMENTATION)
@Author(name = OMSRASTERDIFF_AUTHORNAMES, contact = OMSRASTERDIFF_AUTHORCONTACTS)
@Keywords(OMSRASTERDIFF_KEYWORDS)
@Label(OMSRASTERDIFF_LABEL)
@Name(OMSRASTERDIFF_NAME)
@Status(OMSRASTERDIFF_STATUS)
@License(OMSRASTERDIFF_LICENSE)
public class OmsRasterDiff extends HMModel {

    @Description(OMSRASTERDIFF_IN_RASTER1_DESCRIPTION)
    @In
    public GridCoverage2D inRaster1;

    @Description(OMSRASTERDIFF_IN_RASTER2_DESCRIPTION)
    @In
    public GridCoverage2D inRaster2;

    @Description(OMSRASTERDIFF_P_THRESHOLD_DESCRIPTION)
    @In
    public Double pThreshold;

    @Description(OMSRASTERDIFF_DO_NEGATIVES_DESCRIPTION)
    @In
    public boolean doNegatives = true;

    @Description(OMSRASTERDIFF_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OMSRASTERDIFF_DESCRIPTION = "Subtracts a raster from another, cell by cell, as the canopy height model from the surface and the terrain models.";
    public static final String OMSRASTERDIFF_DOCUMENTATION = """
            The result is the first raster minus the second one, on the grid of the first: the two \
            rasters need the same grid. A cell that is novalue in one of them is novalue in the \
            result. Without the negatives the differences below 0 become 0, as the canopy that \
            can't be below the ground in a surface model minus a terrain model with some noise. \
            The threshold then sets to novalue the differences below it, as the low vegetation \
            and the ground under a canopy height model of 2 m.

            It is a shortcut for the most common Mapcalc expression: Mapcalc is the tool for any \
            other combination of rasters.
            """;
    public static final String OMSRASTERDIFF_KEYWORDS = "Raster, Difference, Subtract, Canopy height model";
    public static final String OMSRASTERDIFF_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERDIFF_NAME = "rdiff";
    public static final int OMSRASTERDIFF_STATUS = 40;
    public static final String OMSRASTERDIFF_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERDIFF_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERDIFF_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERDIFF_IN_RASTER1_DESCRIPTION = "The raster to subtract from.";
    public static final String OMSRASTERDIFF_IN_RASTER2_DESCRIPTION = "The raster to subtract, on the grid of the first one.";
    public static final String OMSRASTERDIFF_P_THRESHOLD_DESCRIPTION = "An optional threshold: the differences below it become novalue.";
    public static final String OMSRASTERDIFF_DO_NEGATIVES_DESCRIPTION = "If false, the negative differences become 0.";
    public static final String OMSRASTERDIFF_OUT_RASTER_DESCRIPTION = "The difference of the rasters.";

    @Execute
    public void process() throws Exception {
        checkNull(inRaster1, inRaster2);

        RegionMap regionMap1 = RegionMap.fromGridGeometry(inRaster1.getGridGeometry());
        RegionMap regionMap2 = RegionMap.fromGridGeometry(inRaster2.getGridGeometry());
        if (!regionMap1.equalsColsRows(regionMap2)) {
            throw new ModelsIllegalargumentException("The two rasters need to have the same grid.", this, pm);
        }

        // the novalues are null for mapcalc, and a null in the expression makes the result null
        StringBuilder script = new StringBuilder("images{r1=read; r2=read; diff=write;} d = r1 - r2;");
        if (!doNegatives) {
            script.append(" d = d < 0 ? 0 : d;");
        }
        if (pThreshold != null) {
            script.append(" d = d < ").append(pThreshold.toString()).append(" ? null : d;");
        }
        script.append(" diff = d;");

        OmsMapcalc mapcalc = new OmsMapcalc();
        mapcalc.pm = pm;
        mapcalc.inRasters = Arrays.asList(mapcalcRaster("r1", inRaster1, regionMap1),
                mapcalcRaster("r2", inRaster2, regionMap1));
        mapcalc.pFunction = script.toString();
        mapcalc.process();
        outRaster = mapcalc.outRaster;
    }

    /**
     * The same raster, with the name by which mapcalc knows it.
     */
    private GridCoverage2D mapcalcRaster( String name, GridCoverage2D raster, RegionMap regionMap ) {
        return CoverageUtilities.buildCoverageWithNovalue(name, raster.getRenderedImage(), regionMap,
                raster.getCoordinateReferenceSystem(), HMConstants.getNovalue(raster));
    }

}
