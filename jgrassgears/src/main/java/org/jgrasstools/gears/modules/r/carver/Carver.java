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
package org.jgrasstools.gears.modules.r.carver;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

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
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.modules.r.bobthebuilder.BobTheBuilder;
import org.jgrasstools.gears.modules.r.linesrasterizer.LinesRasterizer;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

@Description("Carves a raster using a vector map.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Carve, Raster")
@Name("carver")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class Carver extends JGTModel {

    @Description("The input raster.")
    @In
    public GridCoverage2D inRaster = null;

    @Description("The vector map used to carve the raster.")
    @In
    public SimpleFeatureCollection inCarver = null;

    @Description("The buffer.")
    @UI("combo:" + "simple" + "," + "interpolated")
    @In
    public String pMode = "simple";

    @Description("The buffer to use in the interpolation mode.")
    @In
    public double pBuffer = 30.0;

    @Description("The optional field containing the depth to be carved.")
    @In
    public String fDepth = null;

    @Description("The carve depth to use if no field is supplied.")
    @In
    public double pDepth = 6.0;

    @Description("The carved raster map.")
    @Out
    public GridCoverage2D outRaster = null;

    private RegionMap regionMap;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, inCarver);

        regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);

        if (pMode.equals("simple")) {
            simpleMode();
        } else {
            interpolatedMode();
        }

    }

    private void simpleMode() throws Exception {
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        LinesRasterizer lr = new LinesRasterizer();
        lr.fCat = fDepth;
        lr.pCat = pDepth;
        lr.inVector = inCarver;

        lr.pNorth = regionMap.getNorth();
        lr.pSouth = regionMap.getSouth();
        lr.pEast = regionMap.getEast();
        lr.pWest = regionMap.getWest();
        lr.pRows = rows;
        lr.pCols = cols;

        lr.pm = pm;
        lr.process();
        GridCoverage2D depthRaster = lr.outRaster;

        RenderedImage dtmRI = inRaster.getRenderedImage();
        RenderedImage depthRI = depthRaster.getRenderedImage();

        WritableRaster outWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, Double.NaN);

        RandomIter dtmIter = RandomIterFactory.create(dtmRI, null);
        RandomIter depthIter = RandomIterFactory.create(depthRI, null);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);
        for( int x = 0; x < dtmRI.getWidth(); x++ ) {
            for( int y = 0; y < dtmRI.getHeight(); y++ ) {
                double dtmValue = dtmIter.getSampleDouble(x, y, 0);
                double depthValue = depthIter.getSampleDouble(x, y, 0);
                double newValue;
                if (JGTConstants.isNovalue(depthValue)) {
                    newValue = dtmValue;
                } else {
                    newValue = dtmValue - depthValue;
                }

                double existing = outWR.getSampleDouble(x, y, 0);
                if (JGTConstants.isNovalue(existing)) {
                    outIter.setSample(x, y, 0, newValue);
                }
            }
        }

        outRaster = CoverageUtilities.buildCoverage("outraster", outWR, regionMap, inRaster.getCoordinateReferenceSystem());

    }

    private void interpolatedMode() throws Exception {
        
        pm.message("NOTE THAT AT THE MOMENT THIS METHOD ONLY USES pDepth AND NOT THE FIELD!!!");
        
        CoordinateReferenceSystem crs = inRaster.getCoordinateReferenceSystem();
        List<Geometry> riverGeoms = FeatureUtilities.featureCollectionToGeometriesList(inCarver, true, null);
        List<Geometry> bufferedRiverGeoms = new ArrayList<Geometry>();
        for( Geometry riverGeom : riverGeoms ) {
            Geometry bufferedGeom = riverGeom.buffer(pBuffer);
            bufferedRiverGeoms.add(bufferedGeom);
        }

        pm.beginTask("Union...", -1);
        GeometryCollection gc = new GeometryCollection(bufferedRiverGeoms.toArray(GeometryUtilities.TYPE_GEOMETRY),
                GeometryUtilities.gf());
        Geometry bufferUnion = gc.union();
        pm.done();

        SimpleFeatureCollection outPoints = FeatureCollections.newCollection();
        SimpleFeatureCollection outPolygons = FeatureCollections.newCollection();
        SimpleFeatureTypeBuilder polygonTypeBuilder = new SimpleFeatureTypeBuilder();
        polygonTypeBuilder.setName("cpoints");
        polygonTypeBuilder.setCRS(crs);
        polygonTypeBuilder.add("the_geom", Polygon.class);
        SimpleFeatureType polygonType = polygonTypeBuilder.buildFeatureType();
        SimpleFeatureBuilder polygonBuilder = new SimpleFeatureBuilder(polygonType);

        pm.beginTask("Bob it...", bufferUnion.getNumGeometries());
        for( int i = 0; i < bufferUnion.getNumGeometries(); i++ ) {
            Geometry bufferArea = bufferUnion.getGeometryN(i);
            PreparedGeometry preparedArea = PreparedGeometryFactory.prepare(bufferArea);
            List<Coordinate> involvedRiversCoordinate = new ArrayList<Coordinate>();
            // pm.beginTask("Find involved rivers...", riverGeoms.size());
            for( Geometry riverGeom : riverGeoms ) {
                if (preparedArea.intersects(riverGeom)) {
                    Coordinate[] coordinates = riverGeom.getCoordinates();
                    for( Coordinate coordinate : coordinates ) {
                        involvedRiversCoordinate.add(coordinate);
                    }
                }
                // pm.worked(1);
            }
            // pm.done();

            if (involvedRiversCoordinate.size() < 4) {
                continue;
            }

            // use buffered area and involved rivers to carve - one area at a time

            SimpleFeatureCollection controlPoints = createFC4ControlPoints(crs, involvedRiversCoordinate, inRaster);
            SimpleFeatureCollection areaFC = FeatureUtilities.featureCollectionFromGeometry(crs, bufferArea);
            outPoints.addAll(controlPoints);

            BobTheBuilder bob = new BobTheBuilder();
            bob.pm = new DummyProgressMonitor();// pm;
            bob.inRaster = inRaster;
            bob.inArea = areaFC;
            bob.inElevations = controlPoints;
            bob.pMaxbuffer = pBuffer + 10;
            bob.doErode = true;
            bob.doPolygonborder = true;
            bob.doUseOnlyInternal = true;
            bob.fElevation = "elev";
            bob.process();
            inRaster = bob.outRaster;

            polygonBuilder.addAll(new Object[]{bufferArea});
            SimpleFeature polygonFeature = polygonBuilder.buildFeature(null);
            outPolygons.add(polygonFeature);

            pm.worked(1);
        }
        pm.done();

        outRaster = inRaster;
    }

    private SimpleFeatureCollection createFC4ControlPoints( CoordinateReferenceSystem crs, List<Coordinate> riverCoords,
            GridCoverage2D dtm ) throws Exception {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("cpoints");
        b.setCRS(crs);
        b.add("the_geom", Point.class);
        b.add("elev", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        SimpleFeatureCollection fc = FeatureCollections.newCollection();

        for( Coordinate coordinate : riverCoords ) {
            double value = CoverageUtilities.getValue(dtm, coordinate);
            Point point = GeometryUtilities.gf().createPoint(coordinate);
            value = value - pDepth;
            Object[] values = new Object[]{point, value};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(null);
            fc.add(feature);
        }

        return fc;
    }

}
