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
package org.jgrasstools.gears.modules.r.rastervectorintersection;

import static org.jgrasstools.gears.utils.geometry.GeometryUtilities.getGeometryType;
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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.r.cutout.OmsCutOut;
import org.jgrasstools.gears.modules.r.scanline.OmsScanLineRasterizer;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities.GEOMETRYTYPE;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryType;

@Description("Module for raster with polygon vector intersection.")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Raster, Vector, Intersect")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.EXPERIMENTAL)
@Name("rvintersector")
@License("General Public License Version 3 (GPLv3)")
public class OmsRasterVectorIntersector extends JGTModel {

    @Description("The polygon vector to use for the intersection.")
    @In
    public SimpleFeatureCollection inVector = null;

    @Description("The raster to use for the intersection.")
    @In
    public GridCoverage2D inRaster;

    @Description("Flag to use to invert the result (default is false = keep data inside vector)")
    @In
    public boolean doInverse = false;

    @Description("The output raster.")
    @Out
    public GridCoverage2D outRaster;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, inVector);

        SimpleFeatureType schema = inVector.getSchema();
        GeometryType type = schema.getGeometryDescriptor().getType();
        if (getGeometryType(type) != GEOMETRYTYPE.POLYGON && getGeometryType(type) != GEOMETRYTYPE.MULTIPOLYGON) {
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
