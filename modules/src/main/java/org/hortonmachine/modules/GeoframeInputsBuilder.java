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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.exceptions.ModelsIOException;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.modules.r.summary.OmsRasterSummary;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.hmachine.modules.basin.rescaleddistance.OmsRescaledDistance;
import org.hortonmachine.hmachine.modules.network.PfafstetterNumber;
import org.hortonmachine.hmachine.modules.network.networkattributes.NetworkChannel;
import org.hortonmachine.hmachine.modules.network.networkattributes.OmsNetworkAttributesBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.linearref.LengthIndexedLine;
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

    @Description("Input tca raster map.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inTca = null;

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

    @Description("Optional input lakes vector map.")
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inLakes = null;

    @Description("The geoframe topology file, mandatory in case of lakes.")
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inGeoframeTopology = null;

    @Description(OmsRescaledDistance.OMSRESCALEDDISTANCE_pRatio_DESCRIPTION)
    @In
    public double pRatio = 50;

    @Description("Output folder for the geoframe data preparation")
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String outFolder = null;

    private boolean doOverWrite = true;

    /**
     * If <code>true</code>, hack is used to find main network in basin, else pfafstetter.
     */
    private boolean useHack = true;

    @Execute
    public void process() throws Exception {
        checkNull(inPitfiller, inDrain, inTca, inNet, inSkyview, inBasins, outFolder);

        GridCoverage2D subBasins = getRaster(inBasins);
        CoordinateReferenceSystem crs = subBasins.getCoordinateReferenceSystem();
        List<Polygon> cells = CoverageUtilities.gridcoverageToCellPolygons(subBasins, null, true, pm);

        GridCoverage2D pit = getRaster(inPitfiller);
        GridCoverage2D sky = getRaster(inSkyview);
        GridCoverage2D drain = getRaster(inDrain);
        GridCoverage2D net = getRaster(inNet);
        GridCoverage2D tca = getRaster(inTca);

        List<Geometry> lakesList = new ArrayList<>();
        if (inLakes != null) {
            SimpleFeatureCollection lakesFC = getVector(inLakes);
            List<SimpleFeature> lakesFList = FeatureUtilities.featureCollectionToList(lakesFC);
            // remove those that don't even cover the raster envelope
            Polygon rasterBounds = FeatureUtilities.envelopeToPolygon(pit.getEnvelope2D());
            for( SimpleFeature lakeF : lakesFList ) {
                Geometry lakeGeom = (Geometry) lakeF.getDefaultGeometry();
                Envelope env = lakeGeom.getEnvelopeInternal();
                if (rasterBounds.getEnvelopeInternal().intersects(env)) {
                    // TODO would be good to check with basin geom
                    lakesList.add(lakeGeom);
                }
            }
        }

        OmsNetworkAttributesBuilder netAttributesBuilder = new OmsNetworkAttributesBuilder();
        netAttributesBuilder.pm = pm;
        netAttributesBuilder.inDem = pit;
        netAttributesBuilder.inFlow = drain;
        netAttributesBuilder.inTca = tca;
        netAttributesBuilder.inNet = net;
        netAttributesBuilder.doHack = true;
        netAttributesBuilder.onlyDoSimpleGeoms = false;
        netAttributesBuilder.process();
        SimpleFeatureCollection outNet = netAttributesBuilder.outNet;
//        dumpVector(outNet, "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_projects/15_uniTN_basins/brenta/brenta_063basins/network_attributes_full.shp");

        String userDataField = useHack ? NetworkChannel.HACKNAME : NetworkChannel.PFAFNAME;
        List<Geometry> netGeometries = FeatureUtilities.featureCollectionToGeometriesList(outNet, true, userDataField);

        Map<Integer, List<Geometry>> collected = cells.parallelStream()
                .filter(poly -> ((Number) poly.getUserData()).doubleValue() != HMConstants.doubleNovalue)
                .collect(Collectors.groupingBy(poly -> ((Number) poly.getUserData()).intValue()));

        SimpleFeatureBuilder basinsBuilder = getBasinsBuilder(pit.getCoordinateReferenceSystem());
        SimpleFeatureBuilder basinCentroidsBuilder = getBasinCentroidsBuilder(pit.getCoordinateReferenceSystem());
        SimpleFeatureBuilder singleNetBuilder = getSingleNetBuilder(pit.getCoordinateReferenceSystem());

        DefaultFeatureCollection allBasinsFC = new DefaultFeatureCollection();
        DefaultFeatureCollection allNetworksFC = new DefaultFeatureCollection();

        StringBuilder csvText = new StringBuilder();
        csvText.append("#id;x;y;elev_m;avgelev_m;area_km2;netlength;centroid_skyview\n");

        // aggregate basins and check for lakes
        Map<Integer, Geometry> basinId2geomMap = new HashMap<>();
        pm.beginTask("Join basin cells...", collected.size());
        int maxBasinNum = 0;
        for( Entry<Integer, List<Geometry>> entry : collected.entrySet() ) {
            int basinNum = entry.getKey();
            maxBasinNum = Math.max(maxBasinNum, basinNum);

            List<Geometry> polygons = entry.getValue();
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

            // if lakes are available, they can't completely contain a basin
            // and can't be completely contained in a basin
            for( Geometry lakeGeom : lakesList ) {
                if (maxPolygon.contains(lakeGeom)) {
                    throw new ModelsIllegalargumentException("A basin can't completely contain a lake. Check your data.", this);
                }
            }

            basinId2geomMap.put(basinNum, maxPolygon);
            pm.worked(1);
        }
        pm.done();

        // if lakes are available, they have to contain at least one confluence
        for( Geometry lakeGeom : lakesList ) {
            PreparedGeometry preparedLake = PreparedGeometryFactory.prepare(lakeGeom);
            boolean hasPoint = false;
            for( Geometry netGeom : netGeometries ) {
                LineString line = (LineString) netGeom;
                Point startPoint = line.getStartPoint();
                Point endPoint = line.getEndPoint();
                if (preparedLake.contains(startPoint) || preparedLake.contains(endPoint)) {
                    hasPoint = true;
                    break;
                }
            }

            if (!hasPoint) {
                throw new ModelsIllegalargumentException("A lake has to contain at least one confluence. Check your data.", this);
            }
        }

        // if lakes are available, we need to find the basins that intersect and cut the basins on
        // them
        List<Integer> lakesIdList = new ArrayList<>();
        if (!lakesList.isEmpty()) {
            List<Basin> allBasins = new ArrayList<>();
            int nextBasinNum = maxBasinNum + 1;
            Basin rootBasin = getRootBasin(basinId2geomMap, allBasins);
            if (rootBasin != null) {
                pm.beginTask("Handle lake-basin intersections...", lakesList.size());
                for( Geometry lakeGeom : lakesList ) {
                    Basin outBasin = findFirstIntersecting(rootBasin, lakeGeom);
                    if (outBasin != null) {
                        // found the out basin. All others intersecting will drain into this
                        List<Basin> intersectingBasins = allBasins.parallelStream().filter(tmpBasin -> {
                            boolean isNotOutBasin = tmpBasin.id != outBasin.id;
                            boolean intersectsLake = tmpBasin.basinGeometry.intersects(lakeGeom);
                            return isNotOutBasin && intersectsLake;
                        }).collect(Collectors.toList());

                        // of the intersecting find those that are completely contained
                        List<Basin> completelyContainedBasins = new ArrayList<>();
                        List<Basin> justIntersectingBasins = new ArrayList<>();
                        intersectingBasins.forEach(interBasin -> {
                            if (lakeGeom.contains(interBasin.basinGeometry)) {
                                completelyContainedBasins.add(interBasin);
                            } else {
                                justIntersectingBasins.add(interBasin);
                            }
                        });
                        intersectingBasins = justIntersectingBasins;

                        // create a new basin based on the lake
                        Basin lakeBasin = new Basin();
                        lakeBasin.basinGeometry = lakeGeom;
                        lakeBasin.id = nextBasinNum++;
                        lakesIdList.add(lakeBasin.id);
                        lakeBasin.downStreamBasin = outBasin;
                        lakeBasin.downStreamBasinId = outBasin.id;
                        lakeBasin.upStreamBasins.addAll(intersectingBasins);
                        basinId2geomMap.put(lakeBasin.id, lakeBasin.basinGeometry);

                        // then disconnect completely contained basins up and down linking them to
                        // the lake
                        for( Basin containedBasin : completelyContainedBasins ) {
                            for( Basin tmpBasin : allBasins ) {
                                if (tmpBasin.id != containedBasin.id) {
                                    // find basins that drain into the fully contained and relink
                                    // them to the lake
                                    if (tmpBasin.downStreamBasinId == containedBasin.id) {
                                        tmpBasin.downStreamBasinId = lakeBasin.id;
                                        tmpBasin.downStreamBasin = lakeBasin;
                                    }
                                    // find basins in which the completely contained drains into and
                                    // relink with lake
                                    Basin removeBasin = null;
                                    for( Basin tmpUpBasin : tmpBasin.upStreamBasins ) {
                                        if (tmpUpBasin.id == containedBasin.id) {
                                            removeBasin = tmpUpBasin;
                                        }
                                    }
                                    if (removeBasin != null) {
                                        tmpBasin.upStreamBasins.remove(removeBasin);
                                        tmpBasin.upStreamBasins.add(lakeBasin);
                                    }
                                }
                            }
                            // at this point the containedBasin should be orphan
                            containedBasin.downStreamBasin = null;
                            containedBasin.upStreamBasins = null;
                            basinId2geomMap.remove(containedBasin.id);
                        }

                        // handle interaction with downstream basin
                        outBasin.upStreamBasins.removeAll(lakeBasin.upStreamBasins);
                        outBasin.upStreamBasins.add(lakeBasin);
                        Geometry geomToCut = basinId2geomMap.get(outBasin.id);
                        Geometry newGeom;
                        try {
                            newGeom = geomToCut.difference(lakeGeom);
                        } catch (Exception e) {
                            File folder = new File(outFolder);
                            File errorFile = new File(folder, "errors." + HMConstants.GPKG + "#error_basin_" + outBasin.id);
                            
                            String message = "An error occurred during intersection between basin and lake geometries. IGNORING LAKE.\nGeometries written to: " + errorFile;
                            
                            pm.errorMessage(message);
                            
                            geomToCut.setUserData("basin_" + outBasin.id);
                            lakeGeom.setUserData("lake");
                            SimpleFeatureCollection fc = FeatureUtilities.featureCollectionFromGeometry(crs, geomToCut, lakeGeom);
                            OmsVectorWriter.writeVector(errorFile.getAbsolutePath(), fc);
                            
                            continue;
                        }
                        outBasin.basinGeometry = newGeom;
                        basinId2geomMap.put(outBasin.id, newGeom);

                        // handle interaction with upstream basins
                        intersectingBasins.forEach(iBasin -> {
                            iBasin.downStreamBasin = lakeBasin;
                            iBasin.downStreamBasinId = lakeBasin.id;

                            Geometry basinGeomToCut = basinId2geomMap.get(iBasin.id);
                            Geometry newBasinGeom = basinGeomToCut.difference(lakeGeom);
                            basinId2geomMap.put(iBasin.id, newBasinGeom);
                        });

                        // we also need to cut the streams on the lakes
                        List<Geometry> cutNetGeometries = new ArrayList<>();
                        for( Geometry netGeom : netGeometries ) {
                            if (netGeom.intersects(lakeGeom)) {
                                Geometry difference = netGeom.difference(lakeGeom);
                                if (!difference.isEmpty()) {
                                    if (difference.getNumGeometries() > 1) {
                                        // choose longest
                                        Geometry longest = null;
                                        double maxLength = -1;
                                        for( int i = 0; i < difference.getNumGeometries(); i++ ) {
                                            Geometry geometryN = difference.getGeometryN(i);
                                            double l = geometryN.getLength();
                                            if (l > maxLength) {
                                                maxLength = l;
                                                longest = geometryN;
                                            }
                                        }
                                        difference = longest;
                                    }
                                    cutNetGeometries.add(difference);
                                    difference.setUserData(netGeom.getUserData());
                                }
                            } else {
                                cutNetGeometries.add(netGeom);
                            }
                        }
                        netGeometries = cutNetGeometries;
                    }
                }

                // finally rewrite the topology file.

                File topoFile = new File(inGeoframeTopology);
                String topoFilename = FileUtilities.getNameWithoutExtention(topoFile);
                File newTopoFile = new File(topoFile.getParentFile(), topoFilename + "_lakes.txt");
                List<String> topology = new ArrayList<>();
                writeTopology(topology, rootBasin);
                StringBuilder sb = new StringBuilder();
                topology.forEach(record -> sb.append(record).append("\n"));
                FileUtilities.writeFile(sb.toString(), newTopoFile);

            } else {
                pm.errorMessage("Unable to find the basin topology.");
            }
        }

        pm.beginTask("Extract vector basins...", basinId2geomMap.size());
        for( Entry<Integer, Geometry> entry : basinId2geomMap.entrySet() ) {
            int basinNum = entry.getKey();
            pm.message("Processing basin " + basinNum + "...");
            Geometry basinPolygon = entry.getValue();

            double mainNetLength = 0; // for lakes ==0
            List<LineString> netPieces = new ArrayList<>();
            List<Integer> checkValueList = new ArrayList<>();

            boolean isLake = lakesIdList.contains(basinNum);
            if (!isLake) {
                // get network pieces inside basin
                int minCheckValue = Integer.MAX_VALUE;
                HashMap<Integer, List<LineString>> checkValueList4Lines = new HashMap<>();
                for( Geometry netGeom : netGeometries ) {
                    if (netGeom.intersects(basinPolygon)) {
                        LengthIndexedLine lil = new LengthIndexedLine(netGeom);
                        Coordinate centerCoord = lil.extractPoint(0.5);
                        double value = CoverageUtilities.getValue(subBasins, centerCoord);
                        if ((int) value == basinNum) {
                            Object userData = netGeom.getUserData();

                            int checkValue;
                            if (useHack) {
                                checkValue = Integer.parseInt(userData.toString());
                            } else {
                                String pfaf = userData.toString();
                                PfafstetterNumber p = new PfafstetterNumber(pfaf);
                                checkValue = p.getOrder();
                            }
                            minCheckValue = Math.min(minCheckValue, checkValue);
                            checkValueList.add(checkValue);
                            List<LineString> list = checkValueList4Lines.get(checkValue);
                            if (list == null) {
                                list = new ArrayList<>();
                            }
                            list.add((LineString) netGeom);
                            checkValueList4Lines.put(checkValue, list);

                            netPieces.add((LineString) netGeom);

                        }
                    }
                }
                if (minCheckValue != Integer.MAX_VALUE) {
                    List<LineString> minCheckValueLines = checkValueList4Lines.get(minCheckValue);
                    for( LineString minCheckValueLine : minCheckValueLines ) {
                        mainNetLength += minCheckValueLine.getLength();
                    }
                }
            }

            Envelope basinEnvelope = basinPolygon.getEnvelopeInternal();

            Point basinCentroid = basinPolygon.getCentroid();
            double areaM2 = basinPolygon.getArea();
            double areaKm2 = areaM2 / 1000000.0;

            Coordinate point = basinCentroid.getCoordinate();
            double elev = CoverageUtilities.getValue(pit, point);
            double skyview = CoverageUtilities.getValue(sky, point);

            // Extracting raster data for each basin
            ReferencedEnvelope basinRefEnvelope = new ReferencedEnvelope(basinEnvelope, crs);
            GridCoverage2D clipped = CoverageUtilities.clipCoverage(subBasins, basinRefEnvelope);
            WritableRaster clippedWR = CoverageUtilities.renderedImage2IntWritableRaster(clipped.getRenderedImage(), false);

            // we need to consider the lakes and lake cuts, so the polygon needs to be used
            PreparedGeometry preparedBasinPolygon = PreparedGeometryFactory.prepare(basinPolygon);
            RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(clipped);
            GridGeometry2D clippedGG = clipped.getGridGeometry();
            int cols = regionMap.getCols();
            int rows = regionMap.getRows();
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    Coordinate coord = CoverageUtilities.coordinateFromColRow(c, r, clippedGG);
                    if (preparedBasinPolygon.intersects(GeometryUtilities.gf().createPoint(coord))) {
                        clippedWR.setSample(c, r, 0, basinNum);
                        // TODO check
                        // int value = clippedWR.getSample(c, r, 0);
                        // if (value != basinNum) {
                        // clippedWR.setSample(c, r, 0, HMConstants.intNovalue);
                        // }
                    } else {
                        clippedWR.setSample(c, r, 0, HMConstants.intNovalue);
                    }
                }
            }

            File basinFolder = makeBasinFolder(basinNum);

            GridCoverage2D maskCoverage = CoverageUtilities.buildCoverage("basin" + basinNum, clippedWR, regionMap, crs);

            GridCoverage2D clippedPit = CoverageUtilities.clipCoverage(pit, basinRefEnvelope);
            GridCoverage2D cutPit = CoverageUtilities.coverageValuesMapper(clippedPit, maskCoverage);
            File pitFile = new File(basinFolder, "dtm_" + basinNum + ".asc");
            if (!pitFile.exists() || doOverWrite) {
                dumpRaster(cutPit, pitFile.getAbsolutePath());
            }

            double[] minMaxAvgSum = OmsRasterSummary.getMinMaxAvgSum(cutPit);
            double avgElev = minMaxAvgSum[2];

            GridCoverage2D clippedSky = CoverageUtilities.clipCoverage(sky, basinRefEnvelope);
            GridCoverage2D cutSky = CoverageUtilities.coverageValuesMapper(clippedSky, maskCoverage);
            File skyFile = new File(basinFolder, "sky_" + basinNum + ".asc");
            if (!skyFile.exists() || doOverWrite) {
                dumpRaster(cutSky, skyFile.getAbsolutePath());
            }

            GridCoverage2D clippedDrain = CoverageUtilities.clipCoverage(drain, basinRefEnvelope);
            GridCoverage2D cutDrain = CoverageUtilities.coverageValuesMapper(clippedDrain, maskCoverage);
            File drainFile = new File(basinFolder, "drain_" + basinNum + ".asc");
            if (!drainFile.exists() || doOverWrite) {
                dumpRaster(cutDrain, drainFile.getAbsolutePath());
            }

            GridCoverage2D clippedNet = CoverageUtilities.clipCoverage(net, basinRefEnvelope);
            GridCoverage2D cutNet = CoverageUtilities.coverageValuesMapper(clippedNet, maskCoverage);
            File netFile = new File(basinFolder, "net_" + basinNum + ".asc");
            if (!netFile.exists() || doOverWrite) {
                dumpRaster(cutNet, netFile.getAbsolutePath());
            }

//            OmsRescaledDistance rescaledDistance1 = new OmsRescaledDistance();
//            rescaledDistance1.pm = pm;
//            rescaledDistance1.inElev = cutPit;
//            rescaledDistance1.inFlow = cutDrain;
//            rescaledDistance1.inNet = cutNet;
//            rescaledDistance1.pRatio = 1;
//            rescaledDistance1.process();
//            File rescaled1File = new File(basinFolder, "rescaleddistance_1_" + basinNum + ".asc");
//            if (!rescaled1File.exists() || doOverWrite) {
//                dumpRaster(rescaledDistance1.outRescaled, rescaled1File.getAbsolutePath());
//            }
//
//            OmsRescaledDistance rescaledDistance = new OmsRescaledDistance();
//            rescaledDistance.pm = pm;
//            rescaledDistance.inElev = cutPit;
//            rescaledDistance.inFlow = cutDrain;
//            rescaledDistance.inNet = cutNet;
//            rescaledDistance.pRatio = pRatio;
//            rescaledDistance.process();
//            File rescaledRatioFile = new File(basinFolder, "rescaleddistance_" + pRatio + "_" + basinNum + ".asc");
//            if (!rescaledRatioFile.exists() || doOverWrite) {
//                dumpRaster(rescaledDistance.outRescaled, rescaledRatioFile.getAbsolutePath());
//            }

            // finalize feature writing

            // BASINS
            Geometry dumpBasin = basinPolygon;
            if (basinPolygon instanceof Polygon) {
                dumpBasin = GeometryUtilities.gf().createMultiPolygon(new Polygon[]{(Polygon) basinPolygon});
            }
            Object[] basinValues = new Object[]{dumpBasin, basinNum, point.x, point.y, elev, avgElev, areaKm2, mainNetLength,
                    skyview, isLake ? 1 : 0};
            basinsBuilder.addAll(basinValues);
            SimpleFeature basinFeature = basinsBuilder.buildFeature(null);
            allBasinsFC.add(basinFeature);

            // dump single subbasin
            DefaultFeatureCollection singleBasin = new DefaultFeatureCollection();
            singleBasin.add(basinFeature);
            File basinShpFile = new File(basinFolder, "subbasins_complete_ID_" + basinNum + ".shp");
            if (!basinShpFile.exists() || doOverWrite) {
                dumpVector(singleBasin, basinShpFile.getAbsolutePath());
            }

            Object[] centroidValues = new Object[]{basinCentroid, basinNum, point.x, point.y, elev, avgElev, areaKm2,
                    mainNetLength, skyview};
            basinCentroidsBuilder.addAll(centroidValues);
            SimpleFeature basinCentroidFeature = basinCentroidsBuilder.buildFeature(null);

            // dump single centroid
            DefaultFeatureCollection singleCentroid = new DefaultFeatureCollection();
            singleCentroid.add(basinCentroidFeature);
            File centroidShpFile = new File(basinFolder, "centroid_ID_" + basinNum + ".shp");
            if (!centroidShpFile.exists() || doOverWrite) {
                dumpVector(singleCentroid, centroidShpFile.getAbsolutePath());
            }

            // CHANNELS
            if (!netPieces.isEmpty()) {
                DefaultFeatureCollection singleNet = new DefaultFeatureCollection();
                for( int i = 0; i < netPieces.size(); i++ ) {
                    LineString netLine = netPieces.get(i);
                    Integer checkValue = checkValueList.get(i);
                    Object[] netValues = new Object[]{netLine, basinNum, netLine.getLength(), checkValue};
                    singleNetBuilder.addAll(netValues);
                    SimpleFeature singleNetFeature = singleNetBuilder.buildFeature(null);

                    allNetworksFC.add(singleNetFeature);
                    singleNet.add(singleNetFeature);
                }
                File netShpFile = new File(basinFolder, "network_complete_ID_" + basinNum + ".shp");
                if (!netShpFile.exists() || doOverWrite) {
                    dumpVector(singleNet, netShpFile.getAbsolutePath());
                }
            }

            csvText.append(basinNum).append(";");
            csvText.append(point.x).append(";");
            csvText.append(point.y).append(";");
            csvText.append(elev).append(";");
            csvText.append(avgElev).append(";");
            csvText.append(areaKm2).append(";");
            csvText.append(mainNetLength).append(";");
            csvText.append(skyview).append("\n");

            pm.worked(1);
        }
        pm.done();

        File folder = new File(outFolder);
        File basinShpFile = new File(folder, "subbasins_complete.shp");
        if (!basinShpFile.exists() || doOverWrite) {
            dumpVector(allBasinsFC, basinShpFile.getAbsolutePath());
        }
        File netShpFile = new File(folder, "network_complete.shp");
        if (!netShpFile.exists() || doOverWrite) {
            dumpVector(allNetworksFC, netShpFile.getAbsolutePath());
        }
        File csvFile = new File(folder, "subbasins.csv");
        if (!csvFile.exists() || doOverWrite) {
            FileUtilities.writeFile(csvText.toString(), csvFile);
        }

    }

    private Geometry getMaxArea( Geometry newGeom ) {
        if (newGeom.getNumGeometries() > 1) {
            // we take only the basin with major area
            Geometry biggest = null;
            double maxArea = -1;
            for( int i = 0; i < newGeom.getNumGeometries(); i++ ) {
                Geometry geometryN = newGeom.getGeometryN(i);
                double a = geometryN.getArea();
                if (a > maxArea) {
                    maxArea = a;
                    biggest = geometryN;
                }
            }
            newGeom = biggest;
        }
        return newGeom;
    }

    private void writeTopology( List<String> topology, Basin basin ) {
        int downid = 0;
        if (basin.downStreamBasin != null) {
            downid = basin.downStreamBasinId;
        }
        String record = basin.id + " " + downid;
        if (!topology.contains(record)) {
            topology.add(record);
        } else {
            System.out.println("Not adding again: " + record);
        }

        for( Basin upBasin : basin.upStreamBasins ) {
            writeTopology(topology, upBasin);
        }
    }

    private Basin findFirstIntersecting( Basin basin, Geometry lakeGeom ) {
        if (basin.basinGeometry.intersects(lakeGeom)) {
            return basin;
        }
        // move upstream and check
        for( Basin upBasin : basin.upStreamBasins ) {
            Basin firstIntersecting = findFirstIntersecting(upBasin, lakeGeom);
            if (firstIntersecting != null) {
                return firstIntersecting;
            }
        }
        return null;
    }

    private File makeBasinFolder( int basinNum ) throws ModelsIOException {
        File folder = new File(outFolder);
        File basinFolder = new File(folder, String.valueOf(basinNum));
        FileUtilities.folderCheckMakeOrDie(basinFolder.getAbsolutePath());
        return basinFolder;
    }

    private SimpleFeatureBuilder getBasinsBuilder( CoordinateReferenceSystem crs ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("basin");
        b.setCRS(crs);
        b.add("the_geom", MultiPolygon.class);
        b.add("basinid", Integer.class);
        b.add("centrx", Double.class);
        b.add("centry", Double.class);
        b.add("elev_m", Double.class);
        b.add("avgelev_m", Double.class);
        b.add("area_km2", Double.class);
        b.add("length_m", Double.class);
        b.add("skyview", Double.class);
        b.add("islake", Integer.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        return builder;
    }

    private SimpleFeatureBuilder getBasinCentroidsBuilder( CoordinateReferenceSystem crs ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("basin");
        b.setCRS(crs);
        b.add("the_geom", Point.class);
        b.add("basinid", Integer.class);
        b.add("centrx", Double.class);
        b.add("centry", Double.class);
        b.add("elev_m", Double.class);
        b.add("avgelev_m", Double.class);
        b.add("area_km2", Double.class);
        b.add("length_m", Double.class);
        b.add("skyview", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        return builder;
    }

//    private SimpleFeatureBuilder getNetBuilder( CoordinateReferenceSystem crs ) {
//        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
//        b.setName("net");
//        b.setCRS(crs);
//        b.add("the_geom", MultiLineString.class);
//        b.add("basinid", Integer.class);
//        b.add("length_m", Double.class);
//        SimpleFeatureType type = b.buildFeatureType();
//        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
//        return builder;
//    }
    private SimpleFeatureBuilder getSingleNetBuilder( CoordinateReferenceSystem crs ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("net");
        b.setCRS(crs);
        b.add("the_geom", LineString.class);
        b.add("basinid", Integer.class);
        b.add("length_m", Double.class);
        if (useHack) {
            b.add("hack", Double.class);
        } else {
            b.add("pfaforder", Double.class);
        }
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        return builder;
    }

    private Basin getRootBasin( Map<Integer, Geometry> basinId2geomMap, List<Basin> allBasins ) throws IOException {

        if (inGeoframeTopology != null) {
            HashMap<Integer, Basin> id2BasinMap = new HashMap<>();
            List<String> topologyLines = FileUtilities.readFileToLinesList(inGeoframeTopology);
            for( String line : topologyLines ) {
                String[] lineSplit = line.trim().split("\\s+");
                if (lineSplit.length != 2) {
                    throw new ModelsIllegalargumentException("The topology file format is not recognised for line: " + line,
                            this);
                }

                int currentBasinId = Integer.parseInt(lineSplit[0]);
                int childBasinId = Integer.parseInt(lineSplit[1]);
                Basin currentBasin = id2BasinMap.get(currentBasinId);
                if (currentBasin == null) {
                    currentBasin = new Basin();
                    currentBasin.id = currentBasinId;
                    id2BasinMap.put(currentBasinId, currentBasin);
                }
                if (childBasinId > 0) { // this makes the root stay with null down basin
                    Basin childBasin = id2BasinMap.get(childBasinId);
                    if (childBasin != null) {
                        currentBasin.downStreamBasin = childBasin;
                        currentBasin.downStreamBasinId = childBasinId;
                    } else {
                        currentBasin.downStreamBasinId = childBasinId;
                    }
                }
            }

            // find missing downstream basins
            for( Entry<Integer, Basin> entry : id2BasinMap.entrySet() ) {
                Basin basin = entry.getValue();
                if (basin.downStreamBasin == null && basin.downStreamBasinId > 0) {
                    // still need to link it
                    Basin downBasin = id2BasinMap.get(basin.downStreamBasinId);
                    basin.downStreamBasin = downBasin;
                }
            }
            // find missing upstream basins
            for( Entry<Integer, Basin> entry : id2BasinMap.entrySet() ) {
                int id = entry.getKey();
                Basin basin = entry.getValue();
                // find basins that have me as downstream and add them as parents
                for( Basin tmpBasin : id2BasinMap.values() ) {
                    if (id != tmpBasin.id) {
                        if (tmpBasin.downStreamBasinId == id && !basin.upStreamBasins.contains(tmpBasin)) {
                            basin.upStreamBasins.add(tmpBasin);
                        }
                    }
                }
                // also add geometry
                Geometry basinGeometry = basinId2geomMap.get(id);
                basin.basinGeometry = basinGeometry;

                // also add to complete list
                allBasins.add(basin);
            }
            Basin rootBasin = id2BasinMap.values().stream().filter(b -> b.downStreamBasinId < 0).findFirst().get();
            return rootBasin;
        } else {
            return null;
        }

    }

    private static class Basin {
        int id = -1;
        int downStreamBasinId = -1;
        Geometry basinGeometry;

        Basin downStreamBasin;
        List<Basin> upStreamBasins = new ArrayList<>();
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + id;
            return result;
        }
        @Override
        public boolean equals( Object obj ) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Basin other = (Basin) obj;
            if (id != other.id)
                return false;
            return true;
        }
        @Override
        public String toString() {
            return "Basin [\nid=" + id + //
                    "\ndownStreamBasinId=" + downStreamBasinId + //
                    "\nupStreamBasins=" + upStreamBasins.stream().map(b -> String.valueOf(b.id)).collect(Collectors.joining(","))
                    + "\n]";
        }

    }

    public static void main( String[] args ) throws Exception {
        String path = "/Users/hydrologis/lavori_tmp/UNITN/D_basin_issue/";

        String pit = path + "pf.asc";
        String drain = path + "dd.asc";
        String tca = path + "tca.asc";
        String net = path + "net.asc";
        String sky = path + "sky.asc";
        String basins = path + "dsb.asc";
        String topology = path + "tp";

        String lakes = path + "lago.shp";
//        String path = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_projects/15_uniTN_basins/brenta/brenta_063basins_5M_20/";
//        
//        String pit = path + "brenta_pit.asc";
//        String drain = path + "brenta_drain.asc";
//        String tca = path + "brenta_tca.asc";
//        String net = path + "brenta_net_10000.asc";
//        String sky = path + "brenta_skyview.asc";
//        String basins = path + "mytest_desiredbasins_5000000.0_20.0.asc";
//        String topology = path + "mytest_geoframe.txt";
//        
//        String lakes = path + "../laghi/laghiBrenta.shp";

        String outfolder = path + "geoframe";

        GeoframeInputsBuilder g = new GeoframeInputsBuilder();
        g.inPitfiller = pit;
        g.inDrain = drain;
        g.inTca = tca;
        g.inNet = net;
        g.inSkyview = sky;
        g.inBasins = basins;
        g.inLakes = lakes;
        g.inGeoframeTopology = topology;
        g.outFolder = outfolder;
        g.process();
    }

}
