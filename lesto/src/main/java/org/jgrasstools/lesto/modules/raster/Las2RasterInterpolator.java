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
package org.jgrasstools.lesto.modules.raster;

import static java.lang.Math.round;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.io.File;
import java.util.List;

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
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.index.LasDataManager;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.Variables;
import org.jgrasstools.gears.modules.r.interpolation2d.OmsSurfaceInterpolator;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

@Description("Convert a las to a raster through IDW interpolation.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("raster, IDW, interpolation, lidar")
@Label(JGTConstants.LAS + "/raster")
@Name("las2rasterinterpolator")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class Las2RasterInterpolator extends JGTModel {

    @Description("Las files folder main index file path.")
    @In
    public String inIndexFile = null;

    @Description("A dtm raster to use for the area of interest and to calculate the elevation threshold.")
    @In
    public GridCoverage2D inDtm;

    @Description("Flag to normalize with the dtm.")
    @In
    public boolean doNormalize = true;

    @Description("New x resolution (if null, the dtm is used).")
    @In
    public Double pXres;

    @Description("New y resolution (if null, the dtm is used).")
    @In
    public Double pYres;

    @Description("The elevation threshold to apply to the chm.")
    @In
    public double pThreshold = 0.0;

    @Description("The output raster.")
    @Out
    public GridCoverage2D outRaster = null;

    @Execute
    public void process() throws Exception {
        checkNull(inIndexFile, inDtm);

        CoordinateReferenceSystem crs = null;
        Polygon polygon = CoverageUtilities.getRegionPolygon(inDtm);
        crs = inDtm.getCoordinateReferenceSystem();

        try (LasDataManager lasData = new LasDataManager(new File(inIndexFile), inDtm, pThreshold, crs)) {
            lasData.open();
            lasData.setClassesConstraint(new double[]{3.0});
            lasData.setImpulsesConstraint(new double[]{1.0});

            List<LasRecord> lasPoints = lasData.getPointsInGeometry(polygon, false);

            RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDtm);
            double north = regionMap.getNorth();
            double south = regionMap.getSouth();
            double east = regionMap.getEast();
            double west = regionMap.getWest();

            int newRows = (int) round((north - south) / pYres);
            int newCols = (int) round((east - west) / pXres);

            GridGeometry2D newGridGeometry2D = CoverageUtilities.gridGeometryFromRegionValues(north, south, east, west, newCols,
                    newRows, crs);

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
                final Object[] values = new Object[]{point, r.z,};
                builder.addAll(values);
                final SimpleFeature feature = builder.buildFeature(null);
                newCollection.add(feature);
                pm.worked(1);
            }
            pm.done();

            OmsSurfaceInterpolator idwInterpolator = new OmsSurfaceInterpolator();
            idwInterpolator.inVector = newCollection;
            idwInterpolator.inGrid = newGridGeometry2D;
            idwInterpolator.inMask = null;
            idwInterpolator.fCat = "elev";
            idwInterpolator.pMode = Variables.IDW;
            idwInterpolator.pMaxThreads = getDefaultThreadsNum();
            idwInterpolator.pBuffer = 10.0;
            idwInterpolator.pm = pm;
            idwInterpolator.process();
            outRaster = idwInterpolator.outRaster;

        }
    }

}
