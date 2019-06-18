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
package org.hortonmachine.gears.modules.r.carver;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_F_DEPTH_LINES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_F_DEPTH_POLYGONS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_IN_CARVE_R_LINES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_IN_CARVE_R_POLYGONS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_P_DEPTH_LINES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_P_DEPTH_POLYGONS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCARVER_STATUS;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.linesrasterizer.OmsLinesRasterizer;
import org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer;
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

@Description(OMSCARVER_DESCRIPTION)
@Documentation(OMSCARVER_DOCUMENTATION)
@Author(name = OMSCARVER_AUTHORNAMES, contact = OMSCARVER_AUTHORCONTACTS)
@Keywords(OMSCARVER_KEYWORDS)
@Label(OMSCARVER_LABEL)
@Name(OMSCARVER_NAME)
@Status(OMSCARVER_STATUS)
@License(OMSCARVER_LICENSE)
public class OmsCarver extends HMModel {

    @Description(OMSCARVER_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster = null;

    @Description(OMSCARVER_IN_CARVE_R_POLYGONS_DESCRIPTION)
    @In
    public SimpleFeatureCollection inCarverPolygons = null;

    @Description(OMSCARVER_IN_CARVE_R_LINES_DESCRIPTION)
    @In
    public SimpleFeatureCollection inCarverLines = null;

    // @Description("The buffer.")
    // @UI("combo:" + "simple" + "," + "interpolated")
    // @In

    // for now only the simple mode is supported
    private String pMode = "simple";

    // @Description("The buffer to use in the interpolation mode.")
    // @In
    // public double pBuffer = 30.0;

    @Description(OMSCARVER_F_DEPTH_LINES_DESCRIPTION)
    @In
    public String fDepthLines = null;

    @Description(OMSCARVER_P_DEPTH_LINES_DESCRIPTION)
    @In
    public double pDepthLines = 6.0;

    @Description(OMSCARVER_F_DEPTH_POLYGONS_DESCRIPTION)
    @In
    public String fDepthPolygons = null;

    @Description(OMSCARVER_P_DEPTH_POLYGONS_DESCRIPTION)
    @In
    public double pDepthPolygons = 6.0;

    @Description(OMSCARVER_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster = null;

    private RegionMap regionMap;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster);

        regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);

        if (pMode.equals("simple")) {
            simpleMode();
        } else {
            // interpolatedMode();
        }

    }

    private void simpleMode() throws Exception {
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        GridCoverage2D depthLinesRaster = null;
        GridCoverage2D depthPolygonsRaster = null;

        if (inCarverLines != null) {
            OmsLinesRasterizer lr = new OmsLinesRasterizer();
            lr.fCat = fDepthLines;
            lr.pCat = pDepthLines;
            lr.inVector = inCarverLines;
            lr.pNorth = regionMap.getNorth();
            lr.pSouth = regionMap.getSouth();
            lr.pEast = regionMap.getEast();
            lr.pWest = regionMap.getWest();
            lr.pRows = rows;
            lr.pCols = cols;
            lr.pm = pm;
            lr.process();
            depthLinesRaster = lr.outRaster;
        }
        if (inCarverPolygons != null) {
            OmsScanLineRasterizer raster = new OmsScanLineRasterizer();
            raster.inVector = inCarverPolygons;
            raster.pNorth = regionMap.getNorth();
            raster.pSouth = regionMap.getSouth();
            raster.pEast = regionMap.getEast();
            raster.pWest = regionMap.getWest();
            raster.pRows = rows;
            raster.pCols = cols;
            raster.fCat = fDepthLines;
            raster.pValue = pDepthLines;
            raster.pm = pm;
            raster.process();

            depthPolygonsRaster = raster.outRaster;
        }

        GridCoverage2D finalCarveGC = null;
        if (depthLinesRaster != null && depthPolygonsRaster != null) {
            finalCarveGC = CoverageUtilities.mergeCoverages(depthPolygonsRaster, depthLinesRaster);
        } else if (depthLinesRaster == null && depthPolygonsRaster != null) {
            finalCarveGC = depthPolygonsRaster;
        } else if (depthLinesRaster != null && depthPolygonsRaster == null) {
            finalCarveGC = depthLinesRaster;
        } else {
            throw new ModelsIllegalargumentException("At least one lines or one polygon carving layer needs to be supplied.",
                    this, pm);
        }

        RandomIter dtmIter = CoverageUtilities.getRandomIterator(inRaster);
        RandomIter depthIter = CoverageUtilities.getRandomIterator(finalCarveGC);

        WritableRaster outWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, HMConstants.doubleNovalue);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        try {
            for( int y = 0; y < rows; y++ ) {
                for( int x = 0; x < cols; x++ ) {
                    double dtmValue = dtmIter.getSampleDouble(x, y, 0);
                    if (HMConstants.isNovalue(dtmValue)) {
                        continue;
                    }
                    double depthValue = depthIter.getSampleDouble(x, y, 0);
                    double newValue;
                    if (HMConstants.isNovalue(depthValue)) {
                        newValue = dtmValue;
                    } else {
                        newValue = dtmValue - depthValue;
                    }
                    outIter.setSample(x, y, 0, newValue);
                }
            }
        } finally {
            dtmIter.done();
            depthIter.done();
            outIter.done();
        }

        outRaster = CoverageUtilities.buildCoverage("outraster", outWR, regionMap, inRaster.getCoordinateReferenceSystem());

    }

    // private void interpolatedMode() throws Exception {
    //
    // pm.message("NOTE THAT AT THE MOMENT THIS METHOD ONLY USES pDepth AND NOT THE FIELD!!!");
    //
    // CoordinateReferenceSystem crs = inRaster.getCoordinateReferenceSystem();
    // List<Geometry> riverGeoms = FeatureUtilities.featureCollectionToGeometriesList(inCarverLines,
    // true, null);
    // List<Geometry> bufferedRiverGeoms = new ArrayList<Geometry>();
    // for( Geometry riverGeom : riverGeoms ) {
    // Geometry bufferedGeom = riverGeom.buffer(pBuffer);
    // bufferedRiverGeoms.add(bufferedGeom);
    // }
    //
    // pm.beginTask("Union...", -1);
    // GeometryCollection gc = new
    // GeometryCollection(bufferedRiverGeoms.toArray(GeometryUtilities.TYPE_GEOMETRY),
    // GeometryUtilities.gf());
    // Geometry bufferUnion = gc.union();
    // pm.done();
    //
    // SimpleFeatureCollection outPoints = new DefaultFeatureCollection();
    // SimpleFeatureCollection outPolygons = new DefaultFeatureCollection();
    // SimpleFeatureTypeBuilder polygonTypeBuilder = new SimpleFeatureTypeBuilder();
    // polygonTypeBuilder.setName("cpoints");
    // polygonTypeBuilder.setCRS(crs);
    // polygonTypeBuilder.add("the_geom", Polygon.class);
    // SimpleFeatureType polygonType = polygonTypeBuilder.buildFeatureType();
    // SimpleFeatureBuilder polygonBuilder = new SimpleFeatureBuilder(polygonType);
    //
    // pm.beginTask("Bob it...", bufferUnion.getNumGeometries());
    // for( int i = 0; i < bufferUnion.getNumGeometries(); i++ ) {
    // Geometry bufferArea = bufferUnion.getGeometryN(i);
    // PreparedGeometry preparedArea = PreparedGeometryFactory.prepare(bufferArea);
    // List<Coordinate> involvedRiversCoordinate = new ArrayList<Coordinate>();
    // // pm.beginTask("Find involved rivers...", riverGeoms.size());
    // for( Geometry riverGeom : riverGeoms ) {
    // if (preparedArea.intersects(riverGeom)) {
    // Coordinate[] coordinates = riverGeom.getCoordinates();
    // for( Coordinate coordinate : coordinates ) {
    // involvedRiversCoordinate.add(coordinate);
    // }
    // }
    // // pm.worked(1);
    // }
    // // pm.done();
    //
    // if (involvedRiversCoordinate.size() < 4) {
    // continue;
    // }
    //
    // // use buffered area and involved rivers to carve - one area at a time
    //
    // SimpleFeatureCollection controlPoints = createFC4ControlPoints(crs, involvedRiversCoordinate,
    // inRaster);
    // SimpleFeatureCollection areaFC = FeatureUtilities.featureCollectionFromGeometry(crs,
    // bufferArea);
    // outPoints.addAll(controlPoints);
    //
    // OmsBobTheBuilder bob = new OmsBobTheBuilder();
    // bob.pm = new DummyProgressMonitor();// pm;
    // bob.inRaster = inRaster;
    // bob.inArea = areaFC;
    // bob.inElevations = controlPoints;
    // bob.pMaxbuffer = pBuffer + 10;
    // bob.doErode = true;
    // bob.doPolygonborder = true;
    // bob.doUseOnlyInternal = true;
    // bob.fElevation = "elev";
    // bob.process();
    // inRaster = bob.outRaster;
    //
    // polygonBuilder.addAll(new Object[]{bufferArea});
    // SimpleFeature polygonFeature = polygonBuilder.buildFeature(null);
    // outPolygons.add(polygonFeature);
    //
    // pm.worked(1);
    // }
    // pm.done();
    //
    // outRaster = inRaster;
    // }

    // private SimpleFeatureCollection createFC4ControlPoints( CoordinateReferenceSystem crs,
    // List<Coordinate> riverCoords,
    // GridCoverage2D dtm ) throws Exception {
    // SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
    // b.setName("cpoints");
    // b.setCRS(crs);
    // b.add("the_geom", Point.class);
    // b.add("elev", Double.class);
    // SimpleFeatureType type = b.buildFeatureType();
    // SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
    //
    // SimpleFeatureCollection fc = new DefaultFeatureCollection();
    //
    // for( Coordinate coordinate : riverCoords ) {
    // double value = CoverageUtilities.getValue(dtm, coordinate);
    // Point point = GeometryUtilities.gf().createPoint(coordinate);
    // value = value - pDepth;
    // Object[] values = new Object[]{point, value};
    // builder.addAll(values);
    // SimpleFeature feature = builder.buildFeature(null);
    // fc.add(feature);
    // }
    //
    // return fc;
    // }

}
