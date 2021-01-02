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
package org.hortonmachine.modules;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.media.jai.iterator.RandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.libs.exceptions.ModelsIOException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description("Module to prepare input data for the Geoframe modelling environment.")
@Author(name = "Antonello Andrea, Silvia Franceschi", contact = "http://www.hydrologis.com")
@Keywords("geoframe")
@Label(HMConstants.HYDROGEOMORPHOLOGY)
@Name("_GeoframeInputsBuilder")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class GeoframeInputsBuilder extends HMModel {

    @Description("Input pitfiller raster map.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inPitfiller = null;

    @Description("Input flowdirections raster map.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDrain = null;

    @Description("Input network raster map.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNet = null;

    @Description("Input skyview factor raster map.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inSkyview = null;

    @Description("Input numbered basins raster map.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inBasins = null;

    @Description("Output folder for the geoframe data preparation")
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String outFolder = null;

    private boolean doOverWrite = false;

    @Execute
    public void process() throws Exception {
        checkNull(inPitfiller, inDrain, inNet, inSkyview, inBasins, outFolder);

        GridCoverage2D subBasins = getRaster(inBasins);
        pm.beginTask("Vectorize raster map...", IHMProgressMonitor.UNKNOWN);
        List<Polygon> cells = CoverageUtilities.gridcoverageToCellPolygons(subBasins, null);
        pm.done();

        GridCoverage2D pit = getRaster(inPitfiller);
        GridCoverage2D sky = getRaster(inSkyview);
        GridCoverage2D drain = getRaster(inDrain);
        GridCoverage2D net = getRaster(inNet);

        Map<Integer, List<Polygon>> collected = cells.parallelStream()
                .filter(poly -> ((Number) poly.getUserData()).doubleValue() != HMConstants.doubleNovalue)
                .collect(Collectors.groupingBy(poly -> ((Number) poly.getUserData()).intValue()));

        SimpleFeatureBuilder builder = getBuilder(pit.getCoordinateReferenceSystem());

        DefaultFeatureCollection allBasins = new DefaultFeatureCollection();

        HashMap<Integer, Envelope> id2BoundsMap = new HashMap<>();

        StringBuilder csvText = new StringBuilder();
        csvText.append("#basinid;x;y;centroid_elev;centroid_area;netlength;centroid_skyview\n");

        pm.beginTask("Extract vector basins...", collected.size());
        for( Entry<Integer, List<Polygon>> entry : collected.entrySet() ) {
            int basinNum = entry.getKey();

            List<Polygon> polygons = entry.getValue();
            Geometry basin = CascadedPolygonUnion.union(polygons);

            // extract largest basin
            double maxArea = Double.NEGATIVE_INFINITY;
            Geometry maxPolygon = basin;
            int numGeometries = basin.getNumGeometries();
            if (numGeometries > 1) {
                for( int i = 0; i < numGeometries; i++ ) {
                    Geometry geometryN = basin.getGeometryN(i);
                    double area = geometryN.getArea();
                    if (area > maxArea) {
                        maxArea = area;
                        maxPolygon = geometryN;
                    }
                }
            }

            id2BoundsMap.put(basinNum, maxPolygon.getEnvelopeInternal());

            Point centroid = maxPolygon.getCentroid();
            double area = maxPolygon.getArea();

            Coordinate point = centroid.getCoordinate();
            double elev = CoverageUtilities.getValue(pit, point);
            double skyview = CoverageUtilities.getValue(sky, point);

            csvText.append(basinNum).append(";");
            csvText.append(point.x).append(";");
            csvText.append(point.y).append(";");
            csvText.append(elev).append(";");
            csvText.append(area).append(";");
            csvText.append(-1).append(";");
            csvText.append(skyview).append("\n");

            Object[] values = new Object[]{maxPolygon, basinNum, point.x, point.y, elev, area, -1, skyview}; // TODO add lunghezza
                                                                                                             // asta
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(null);

            DefaultFeatureCollection singleBasin = new DefaultFeatureCollection();
            singleBasin.add(feature);
            File basinFolder = makeBasinFolder(basinNum);
            File basinShpFile = new File(basinFolder, "subbasins_complete_ID_" + basinNum + ".shp");
            if (!basinShpFile.exists() || doOverWrite) {
                dumpVector(singleBasin, basinShpFile.getAbsolutePath());
            }

            allBasins.add(feature);
            pm.worked(1);
        }
        pm.done();

        File folder = new File(outFolder);
        File basinShpFile = new File(folder, "subbasins_complete.shp");
        if (!basinShpFile.exists() || doOverWrite) {
            dumpVector(allBasins, basinShpFile.getAbsolutePath());
        }
        File csvFile = new File(folder, "subbasins.csv");
        if (!csvFile.exists() || doOverWrite) {
            FileUtilities.writeFile(csvText.toString(), csvFile);
        }

        pm.beginTask("Extracting raster data for each basin...", id2BoundsMap.size());
        CoordinateReferenceSystem crs = subBasins.getCoordinateReferenceSystem();
        for( Entry<Integer, Envelope> entry : id2BoundsMap.entrySet() ) {
            int basinNum = entry.getKey();
            Envelope env = entry.getValue();
            // create basins masking

            GridCoverage2D clipped = CoverageUtilities.clipCoverage(subBasins, new ReferencedEnvelope(env, crs));
            WritableRaster clippedWR = CoverageUtilities.renderedImage2IntWritableRaster(clipped.getRenderedImage(), false);

            RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(clipped);
            int cols = regionMap.getCols();
            int rows = regionMap.getRows();
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    int value = clippedWR.getSample(c, r, 0);
                    if (value != basinNum) {
                        clippedWR.setSample(c, r, 0, HMConstants.intNovalue);
                    }
                }
            }
            GridCoverage2D maskCoverage = CoverageUtilities.buildCoverage("basin" + basinNum, clippedWR, regionMap, crs);

            File basinFolder = makeBasinFolder(basinNum);

            GridCoverage2D clippedPit = CoverageUtilities.clipCoverage(pit, new ReferencedEnvelope(env, crs));
            GridCoverage2D cutPit = CoverageUtilities.coverageValuesMapper(clippedPit, maskCoverage);
            File pitFile = new File(basinFolder, "dtm_" + basinNum + ".asc");
            if (!pitFile.exists() || doOverWrite) {
                dumpRaster(cutPit, pitFile.getAbsolutePath());
            }

            GridCoverage2D clippedSky = CoverageUtilities.clipCoverage(sky, new ReferencedEnvelope(env, crs));
            GridCoverage2D cutSky = CoverageUtilities.coverageValuesMapper(clippedSky, maskCoverage);
            File skyFile = new File(basinFolder, "sky_" + basinNum + ".asc");
            if (!skyFile.exists() || doOverWrite) {
                dumpRaster(cutSky, skyFile.getAbsolutePath());
            }

            GridCoverage2D clippedDrain = CoverageUtilities.clipCoverage(drain, new ReferencedEnvelope(env, crs));
            GridCoverage2D cutDrain = CoverageUtilities.coverageValuesMapper(clippedDrain, maskCoverage);
            File drainFile = new File(basinFolder, "drain_" + basinNum + ".asc");
            if (!drainFile.exists() || doOverWrite) {
                dumpRaster(cutDrain, drainFile.getAbsolutePath());
            }

            GridCoverage2D clippedNet = CoverageUtilities.clipCoverage(net, new ReferencedEnvelope(env, crs));
            GridCoverage2D cutNet = CoverageUtilities.coverageValuesMapper(clippedNet, maskCoverage);
            File netFile = new File(basinFolder, "net_" + basinNum + ".asc");
            if (!netFile.exists() || doOverWrite) {
                dumpRaster(cutNet, netFile.getAbsolutePath());
            }

            pm.worked(1);
        }
        pm.done();

    }

    private File makeBasinFolder( int basinNum ) throws ModelsIOException {
        File folder = new File(outFolder);
        File basinFolder = new File(folder, String.valueOf(basinNum));
        FileUtilities.folderCheckMakeOrDie(basinFolder.getAbsolutePath());
        return basinFolder;
    }

    private SimpleFeatureBuilder getBuilder( CoordinateReferenceSystem crs ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("basin");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("basinid", Integer.class);
        b.add("centrx", Double.class);
        b.add("centry", Double.class);
        b.add("centrz", Double.class);
        b.add("area", Double.class);
        b.add("length", Double.class);
        b.add("skyview", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        return builder;
    }

    public static void main( String[] args ) throws Exception {
        String path = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_projects/15_uniTN_basins/brenta/brenta_all/";

        String pit = path + "pitfiller.asc";
        String drain = path + "brenta_drain.asc";
        String net = path + "brenta_net_10000.asc";
        String sky = path + "brenta_skyview.asc";
        String basins = path + "mytest_pts_desiredbasins_10000000_20.asc";
        String outfolder = path + "geoframe";

        GeoframeInputsBuilder g = new GeoframeInputsBuilder();
        g.inPitfiller = pit;
        g.inDrain = drain;
        g.inNet = net;
        g.inSkyview = sky;
        g.inBasins = basins;
        g.outFolder = outfolder;
        g.process();
    }

}
