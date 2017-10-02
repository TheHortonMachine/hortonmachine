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
package org.hortonmachine.gears.modules.r.aoi;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.processing.Operations;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.JGTProcessingRegion;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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
import oms3.annotations.Range;
import oms3.annotations.Status;

@Description("Module that extracts a gridcoverage for a given area of interest, resampling it if necessary.")
@Label("Area of interest")
@Name("Area of interest")
@Documentation("")
@Author(name = "Andrea Antonello, Falko Braeutigam", contact = "www.hydrologis.com")
@Keywords("raster, crop")
@Status(Status.EXPERIMENTAL)
@License(OMSHYDRO_LICENSE)
public class OmsAreaOfInterestGridCoverage extends HMModel {

    @In
    @Description("The source raster data.")
    public GridCoverage2DReader inCoverageReader;

    @In
    @Description("The requested are aof interest bounds.")
    public ReferencedEnvelope aoi;

    @In
    @Description("The requested resampling resolution factor.")
    @Range(min = 1, max = 100)
    public int resolutionFactor = 1;

    @Description("The resampled and cropped gridcoverage.")
    @Out
    public GridCoverage2D outCoverage;

    @Execute
    public void process() throws Exception {

        CoordinateReferenceSystem crs = inCoverageReader.getCoordinateReferenceSystem();
        
        aoi = aoi.transform( crs, true );
        
        GeneralEnvelope originalEnvelope = inCoverageReader.getOriginalEnvelope();
        GridEnvelope2D overviewGridEnvelope = (GridEnvelope2D) inCoverageReader.getOriginalGridRange();

        double[] llCorner = originalEnvelope.getLowerCorner().getCoordinate();
        double[] urCorner = originalEnvelope.getUpperCorner().getCoordinate();

        JGTProcessingRegion originalRegion = new JGTProcessingRegion(llCorner[0], urCorner[0], llCorner[1], urCorner[1],
                overviewGridEnvelope.height, overviewGridEnvelope.width);

        RegionMap originalRegionMap = CoverageUtilities.makeRegionParamsMap(urCorner[1], llCorner[1], llCorner[0], urCorner[0],
                originalRegion.getWEResolution(), originalRegion.getNSResolution(), overviewGridEnvelope.width,
                overviewGridEnvelope.height);

        RegionMap subRegion = originalRegionMap.toSubRegion(aoi.getMaxY(), aoi.getMinY(), aoi.getMinX(), aoi.getMaxX());

        double xres = subRegion.getXres();
        double yres = subRegion.getYres();

        if (resolutionFactor > 1) {
            xres = xres * resolutionFactor;
            yres = yres * resolutionFactor;
        }

        outCoverage = CoverageUtilities.getGridCoverage((AbstractGridCoverage2DReader) inCoverageReader, subRegion.getNorth(),
                subRegion.getSouth(), subRegion.getEast(), subRegion.getWest(), xres, yres,
                crs);

        ReferencedEnvelope outenvelope = new ReferencedEnvelope(subRegion.getWest(), subRegion.getEast(), subRegion.getSouth(),
                subRegion.getNorth(), crs);
        outCoverage = (GridCoverage2D) Operations.DEFAULT.crop(outCoverage, outenvelope);

    }

}
