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
package org.hortonmachine.gears.io.subsampling;

import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_AUTHORCONTACTS;
import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_AUTHORNAMES;
import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_DESCRIPTION;
import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_KEYWORDS;
import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_LABEL;
import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_LICENSE;
import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_NAME;
import static org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler.OMSRASTERSUBSAMPLER_STATUS;
import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

@Description(OMSRASTERSUBSAMPLER_DESCRIPTION)
@Author(name = OMSRASTERSUBSAMPLER_AUTHORNAMES, contact = OMSRASTERSUBSAMPLER_AUTHORCONTACTS)
@Keywords(OMSRASTERSUBSAMPLER_KEYWORDS)
@Label(OMSRASTERSUBSAMPLER_LABEL)
@Name(OMSRASTERSUBSAMPLER_NAME)
@Status(OMSRASTERSUBSAMPLER_STATUS)
@License(OMSRASTERSUBSAMPLER_LICENSE)
public class OmsRasterSubsampler extends HMModel {

    @Description(OMSRASTERSUBSAMPLER_FILE_DESCRIPTION)
    @In
    public GridCoverage2D inRaster = null;

    @Description(OMSRASTERSUBSAMPLER_P_FACTOR_DESCRIPTION)
    @In
    public int pFactor = 2;

    @Description(OMSRASTERSUBSAMPLER_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster = null;

    public static final String OMSRASTERSUBSAMPLER_DESCRIPTION = "Raster subsampler module.";
    public static final String OMSRASTERSUBSAMPLER_DOCUMENTATION = "";
    public static final String OMSRASTERSUBSAMPLER_KEYWORDS = "IO, Coverage, Raster, Subsampling";
    public static final String OMSRASTERSUBSAMPLER_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERSUBSAMPLER_NAME = "rastersubsampler";
    public static final int OMSRASTERSUBSAMPLER_STATUS = 40;
    public static final String OMSRASTERSUBSAMPLER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERSUBSAMPLER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERSUBSAMPLER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERSUBSAMPLER_FILE_DESCRIPTION = "The raster to process.";
    public static final String OMSRASTERSUBSAMPLER_P_FACTOR_DESCRIPTION = "The subsampling factor (default is 2).";
    public static final String OMSRASTERSUBSAMPLER_OUT_RASTER_DESCRIPTION = "The output raster map.";

    @Execute
    public void process() throws Exception {
        checkNull(inRaster);

        HMRaster raster = HMRaster.fromGridCoverage(inRaster);

        RegionMap regionMap = raster.getRegionMap();

        double newXRes = regionMap.getXres() * pFactor;
        double newYRes = regionMap.getYres() * pFactor;

        double xRest = regionMap.getWidth() % newXRes;
        double yRest = regionMap.getHeight() % newYRes;

        double newEast = regionMap.getEast() - xRest;
        double newSouth = regionMap.getSouth() + yRest;
        
        RegionMap newRegionMap = RegionMap.fromBoundsAndResolution(regionMap.getWest(), newEast, newSouth, regionMap.getNorth(), newXRes, newYRes);
        
        HMRaster outHMRaster = new HMRaster.HMRasterWritableBuilder().setCrs(raster.getCrs()).setName("subsampled").setRegion(newRegionMap).build();
        outHMRaster.mapRaster(pm, raster);
        
        outRaster = outHMRaster.buildCoverage();
        raster.close();
        outHMRaster.close();
    }

}
