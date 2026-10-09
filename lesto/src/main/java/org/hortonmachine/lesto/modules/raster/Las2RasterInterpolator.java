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
package org.hortonmachine.lesto.modules.raster;

import static java.lang.Math.round;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.io.File;
import java.util.List;

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
import oms3.annotations.Unit;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.io.las.ALasDataManager;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.modules.r.interpolation2d.OmsSurfaceInterpolator;
import org.hortonmachine.gears.modules.r.rastergenerator.OmsRasterGenerator;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

@Description("Interpolates the points of a lidar survey over the ground into a raster, with inverse distance weighting: the height of the canopy over the ground or the surface of the canopy.")
@Documentation(Las2RasterInterpolator.DOCUMENTATION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("raster, IDW, interpolation, lidar")
@Label(HMConstants.LESTO + "/raster")
@Name("las2rasterinterpolator")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class Las2RasterInterpolator extends HMModel {

    @Description("The lidar data: a las file, the index file of a folder of las files created by LasIndexer or a las database.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inIndexFile = null;

    @Description("The elevation model of the ground: it gives the area, the grid and the ground under the points.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDtm;

    @Description("If true, the height of the points over the ground is interpolated, if false their elevation.")
    @In
    public boolean doNormalize = true;

    @Description("The x resolution of the raster. If not supplied, the one of the elevation model is used.")
    @In
    public Double pXres;

    @Description("The y resolution of the raster. If not supplied, the one of the elevation model is used.")
    @In
    public Double pYres;

    @Description("The minimum height over the ground of the interpolated points.")
    @Unit("m")
    @In
    public double pThreshold = 0.0;

    @Description("The return to use, as 1 for the first returns. If not supplied, all the returns are used.")
    @In
    public Integer pImpulse = 1;

    @Description("The interpolated raster.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster = null;

    public static final String DOCUMENTATION = """
            Only the points higher than the threshold over the elevation model are interpolated: with the default threshold of 0, the points over the ground. Each cell gets the average of the points within 10 m of it, weighted by the inverse of their distance. With the first returns and the normalization, the raster is a model of the height of the canopy, with its elevation a model of its surface.

            The points on the ground are, with their errors, half over and half under the elevation model: in the open areas, the raster is interpolated from the few ground points over it and from the canopy around. Use a threshold of some decimeters to keep only the vegetation.
            """;

    @Execute
    public void process() throws Exception {
        checkNull(inIndexFile, inDtm, outRaster);

        GridCoverage2D inDtmGC = getRaster(inDtm);
        Polygon polygon = CoverageUtilities.getRegionPolygon(inDtmGC);
        CoordinateReferenceSystem crs = inDtmGC.getCoordinateReferenceSystem();

        try (ALasDataManager lasData = ALasDataManager.getDataManager(new File(inIndexFile), inDtmGC, pThreshold, crs)) {
            lasData.open();
            if (pImpulse != null) {
                lasData.setImpulsesConstraint(new double[]{pImpulse});
            }

            List<LasRecord> lasPoints = lasData.getPointsInGeometry(polygon, false);
            if (lasPoints.size() == 0) {
                pm.message("No points foudn in the given area. Check your input.");
                return;
            }

            RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDtmGC);
            double north = regionMap.getNorth();
            double south = regionMap.getSouth();
            double east = regionMap.getEast();
            double west = regionMap.getWest();
            if (pXres == null) {
                pXres = regionMap.getXres();
            }
            if (pYres == null) {
                pYres = regionMap.getYres();
            }

            int newRows = (int) round((north - south) / pYres);
            int newCols = (int) round((east - west) / pXres);

            DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
            final SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName("lasdata");
            b.setCRS(crs);
            b.add("the_geom", Point.class);
            b.add("elev", Double.class);
            final SimpleFeatureType featureType = b.buildFeatureType();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);

            pm.beginTask("Prepare points collection for interpolation...", lasPoints.size());
            for( LasRecord r : lasPoints ) {
                final Point point = gf.createPoint(new Coordinate(r.x, r.y));
                // the height over the dtm or the elevation
                double value = doNormalize ? r.groundElevation : r.z;
                final Object[] values = new Object[]{point, value};
                builder.addAll(values);
                final SimpleFeature feature = builder.buildFeature(null);
                newCollection.add(feature);
                pm.worked(1);
            }
            pm.done();

            OmsRasterGenerator omsRasterGenerator = new OmsRasterGenerator();
            omsRasterGenerator.pNorth = north;
            omsRasterGenerator.pSouth = south;
            omsRasterGenerator.pWest = west;
            omsRasterGenerator.pEast = east;
            omsRasterGenerator.pXres = (east - west) / newCols;
            omsRasterGenerator.pYres = (north - south) / newRows;
            omsRasterGenerator.inCrs = crs;
            omsRasterGenerator.doRandom = false;
            omsRasterGenerator.process();

            OmsSurfaceInterpolator idwInterpolator = new OmsSurfaceInterpolator();
            idwInterpolator.inVector = newCollection;
            idwInterpolator.inGrid = omsRasterGenerator.outRaster;
            idwInterpolator.inMask = null;
            idwInterpolator.fCat = "elev";
            idwInterpolator.pMode = Variables.IDW;
            idwInterpolator.pMaxThreads = getDefaultThreadsNum();
            idwInterpolator.pBuffer = 10.0;
            idwInterpolator.pm = pm;
            idwInterpolator.process();
            GridCoverage2D outRasterGC = idwInterpolator.outRaster;
            dumpRaster(outRasterGC, outRaster);

        }
    }

}
