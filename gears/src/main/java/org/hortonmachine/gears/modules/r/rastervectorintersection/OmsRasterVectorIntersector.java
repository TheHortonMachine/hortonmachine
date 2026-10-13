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
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.geotools.api.feature.simple.SimpleFeatureType;

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
    
    public static final String OMSRASTERVECTORINTERSECTOR_DESCRIPTION = "Cuts a raster with polygons: keeps the cells inside them, or with the inverse switch the cells outside.";
    public static final String OMSRASTERVECTORINTERSECTOR_DOCUMENTATION = """
            A cell is inside when its center is inside a polygon, as in ScanLineRasterizer, that \
            makes the mask of the polygons on the grid of the raster; CutOut then keeps the \
            values under the mask, or the ones outside of it with the inverse switch. The other \
            cells become novalue, the grid doesn't change: to also shrink the grid to the \
            polygons, RasterResizer is the tool.

            It is the one step version of ScanLineRasterizer with the raster as grid, followed by \
            CutOut, for example to keep the elevation of a basin from its polygon, or to blank \
            the lakes of an elevation model.
            """;
    public static final String OMSRASTERVECTORINTERSECTOR_KEYWORDS = "Raster, Vector, Intersect, Clip, Mask";
    public static final String OMSRASTERVECTORINTERSECTOR_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERVECTORINTERSECTOR_NAME = "rvintersector";
    public static final int OMSRASTERVECTORINTERSECTOR_STATUS = 40;
    public static final String OMSRASTERVECTORINTERSECTOR_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERVECTORINTERSECTOR_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERVECTORINTERSECTOR_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERVECTORINTERSECTOR_IN_VECTOR_DESCRIPTION = "The polygons to cut the raster with.";
    public static final String OMSRASTERVECTORINTERSECTOR_IN_RASTER_DESCRIPTION = "The raster to cut.";
    public static final String OMSRASTERVECTORINTERSECTOR_DO_INVERSE_DESCRIPTION = "If true, keeps the cells outside of the polygons instead of the ones inside.";
    public static final String OMSRASTERVECTORINTERSECTOR_OUT_RASTER_DESCRIPTION = "The cut raster.";


    @Execute
    public void process() throws Exception {
        checkNull(inRaster, inVector);

        SimpleFeatureType schema = inVector.getSchema();
        if (!EGeometryType.isPolygon(schema.getGeometryDescriptor())) {
            throw new ModelsRuntimeException("The module works only with polygon vectors.", this);
        }

        // the mask of the polygons, on the grid of the raster
        OmsScanLineRasterizer raster = new OmsScanLineRasterizer();
        raster.pm = pm;
        raster.inVector = inVector;
        raster.inRaster = inRaster;
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
