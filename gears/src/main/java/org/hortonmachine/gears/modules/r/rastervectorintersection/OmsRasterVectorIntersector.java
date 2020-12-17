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
package org.hortonmachine.gears.modules.r.rastervectorintersection;

import static org.hortonmachine.gears.modules.r.rastervectorintersection.OmsRasterVectorIntersector.*;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.cutout.OmsCutOut;
import org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.opengis.feature.simple.SimpleFeatureType;

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

@Description(OMSRASTERVECTORINTERSECTOR_DESCRIPTION)
@Documentation(OMSRASTERVECTORINTERSECTOR_DOCUMENTATION)
@Author(name = OMSRASTERVECTORINTERSECTOR_AUTHORNAMES, contact = OMSRASTERVECTORINTERSECTOR_AUTHORCONTACTS)
@Keywords(OMSRASTERVECTORINTERSECTOR_KEYWORDS)
@Label(OMSRASTERVECTORINTERSECTOR_LABEL)
@Name(OMSRASTERVECTORINTERSECTOR_NAME)
@Status(OMSRASTERVECTORINTERSECTOR_STATUS)
@License(OMSRASTERVECTORINTERSECTOR_LICENSE)
public class OmsRasterVectorIntersector extends HMModel {

    @Description(OMSRASTERVECTORINTERSECTOR_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector = null;

    @Description(OMSRASTERVECTORINTERSECTOR_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERVECTORINTERSECTOR_DO_INVERSE_DESCRIPTION)
    @In
    public boolean doInverse = false;

    @Description(OMSRASTERVECTORINTERSECTOR_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;
    
    public static final String OMSRASTERVECTORINTERSECTOR_DESCRIPTION = "Module for raster with polygon vector intersection.";
    public static final String OMSRASTERVECTORINTERSECTOR_DOCUMENTATION = "";
    public static final String OMSRASTERVECTORINTERSECTOR_KEYWORDS = "Raster, Vector, Intersect";
    public static final String OMSRASTERVECTORINTERSECTOR_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERVECTORINTERSECTOR_NAME = "rvintersector";
    public static final int OMSRASTERVECTORINTERSECTOR_STATUS = 5;
    public static final String OMSRASTERVECTORINTERSECTOR_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERVECTORINTERSECTOR_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERVECTORINTERSECTOR_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERVECTORINTERSECTOR_IN_VECTOR_DESCRIPTION = "The polygon vector to use for the intersection.";
    public static final String OMSRASTERVECTORINTERSECTOR_IN_RASTER_DESCRIPTION = "The raster to use for the intersection.";
    public static final String OMSRASTERVECTORINTERSECTOR_DO_INVERSE_DESCRIPTION = "Flag to use to invert the result (default is false = keep data inside vector)";
    public static final String OMSRASTERVECTORINTERSECTOR_OUT_RASTER_DESCRIPTION = "The output raster.";


    @Execute
    public void process() throws Exception {
        checkNull(inRaster, inVector);

        SimpleFeatureType schema = inVector.getSchema();
        if (!EGeometryType.isPolygon(schema.getGeometryDescriptor())) {
            throw new ModelsRuntimeException("The module works only with polygon vectors.", this);
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        OmsScanLineRasterizer raster = new OmsScanLineRasterizer();
        raster.inVector = inVector;
        raster.pCols = regionMap.getCols();
        raster.pRows = regionMap.getRows();
        raster.pNorth = regionMap.getNorth();
        raster.pSouth = regionMap.getSouth();
        raster.pEast = regionMap.getEast();
        raster.pWest = regionMap.getWest();
        raster.pValue = 1.0;
        raster.process();
        GridCoverage2D rasterizedVector = raster.outRaster;

        OmsCutOut cutout = new OmsCutOut();
        cutout.pm = pm;
        cutout.inRaster = inRaster;
        cutout.inMask = rasterizedVector;
        cutout.doInverse = doInverse;
        cutout.process();
        outRaster = cutout.outRaster;
    }
}
