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
package org.hortonmachine.gears.modules.v.smoothing;

import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherMcMaster.OMSLINESMOOTHERMCMASTER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherMcMaster.OMSLINESMOOTHERMCMASTER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherMcMaster.OMSLINESMOOTHERMCMASTER_BIBLIOGRAPHY;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherMcMaster.OMSLINESMOOTHERMCMASTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherMcMaster.OMSLINESMOOTHERMCMASTER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherMcMaster.OMSLINESMOOTHERMCMASTER_KEYWORDS;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherMcMaster.OMSLINESMOOTHERMCMASTER_LABEL;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherMcMaster.OMSLINESMOOTHERMCMASTER_LICENSE;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherMcMaster.OMSLINESMOOTHERMCMASTER_NAME;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherMcMaster.OMSLINESMOOTHERMCMASTER_STATUS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureWriter;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.PrintStreamProgressMonitor;
import org.hortonmachine.gears.utils.features.FeatureGeometrySubstitutor;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.geotools.api.feature.simple.SimpleFeature;

import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

@Description(OMSLINESMOOTHERMCMASTER_DESCRIPTION)
@Documentation(OMSLINESMOOTHERMCMASTER_DOCUMENTATION)
@Bibliography(OMSLINESMOOTHERMCMASTER_BIBLIOGRAPHY)
@Author(name = OMSLINESMOOTHERMCMASTER_AUTHORNAMES, contact = OMSLINESMOOTHERMCMASTER_AUTHORCONTACTS)
@Keywords(OMSLINESMOOTHERMCMASTER_KEYWORDS)
@Label(OMSLINESMOOTHERMCMASTER_LABEL)
@Name(OMSLINESMOOTHERMCMASTER_NAME)
@Status(OMSLINESMOOTHERMCMASTER_STATUS)
@License(OMSLINESMOOTHERMCMASTER_LICENSE)
public class OmsLineSmootherMcMaster extends HMModel {

    @Description(OMSLINESMOOTHERMCMASTER_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector;

    @Description(OMSLINESMOOTHERMCMASTER_P_LOOK_AHEAD_DESCRIPTION)
    @In
    public int pLookahead = 7;

    @Description(OMSLINESMOOTHERMCMASTER_P_LIMIT_DESCRIPTION)
    @In
    public int pLimit = 0;

    @Description(OMSLINESMOOTHERMCMASTER_P_SLIDE_DESCRIPTION)
    @In
    public double pSlide = 0.9;

    @Description(OMSLINESMOOTHERMCMASTER_P_DENSIFY_DESCRIPTION)
    @In
    public Double pDensify = null;

    @Description(OMSLINESMOOTHERMCMASTER_P_SIMPLIFY_DESCRIPTION)
    @In
    public Double pSimplify = null;

    @Description(OMSLINESMOOTHERMCMASTER_OUT_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector;

    public static final String OMSLINESMOOTHERMCMASTER_DESCRIPTION = "Smooths lines with the sliding averaging of McMaster: each vertex moves towards the average of the vertices around it.";
    public static final String OMSLINESMOOTHERMCMASTER_DOCUMENTATION = """
            Each vertex moves to (1 - slide) x its position + slide x the average position of the look ahead \
            vertices centered on it: with a slide of 1 it goes to the average, with 0 it stays. An even look \
            ahead is increased by one. The window is a number of vertices, not a distance, so on lines with \
            unevenly spaced vertices the densify interval, applied before the smoothing, makes it a length as \
            well: the window is then about look ahead x interval long. Lines with fewer vertices than four \
            times the look ahead use a window of a fifth of their vertices, and lines with fewer than 10 \
            vertices are not smoothed.

            The first and the last half window of vertices stay in place, so the lines keep their ends and \
            stay connected to each other. Closed lines are smoothed across their closing vertex.

            The lines not longer than the limit are not smoothed. Among them, the closed ones and the ones \
            not touching another line at their ends are removed, as the small rings and the isolated bits of \
            a vectorized raster. The optional simplification, after the smoothing, drops the vertices closer \
            than its tolerance to the line, without changing the topology.
            """;
    public static final String OMSLINESMOOTHERMCMASTER_BIBLIOGRAPHY = "McMaster, R. B. (1989). The integration of simplification and smoothing algorithms in line generalization. Cartographica, 26(1), 101-121.";
    public static final String OMSLINESMOOTHERMCMASTER_KEYWORDS = "Smoothing, Vector, Generalization";
    public static final String OMSLINESMOOTHERMCMASTER_LABEL = VECTORPROCESSING;
    public static final String OMSLINESMOOTHERMCMASTER_NAME = "linesmoother";
    public static final int OMSLINESMOOTHERMCMASTER_STATUS = 40;
    public static final String OMSLINESMOOTHERMCMASTER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSLINESMOOTHERMCMASTER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSLINESMOOTHERMCMASTER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSLINESMOOTHERMCMASTER_IN_VECTOR_DESCRIPTION = "The vector containing the lines to be smoothed.";
    public static final String OMSLINESMOOTHERMCMASTER_P_LOOK_AHEAD_DESCRIPTION = "The number of vertices averaged for each vertex, an odd number (default = 7).";
    public static final String OMSLINESMOOTHERMCMASTER_P_LIMIT_DESCRIPTION = "The length up to which the lines are not smoothed: of them, the closed ones and the ones not connected to other lines are removed.";
    public static final String OMSLINESMOOTHERMCMASTER_P_SLIDE_DESCRIPTION = "How far each vertex moves towards the average, from 0 (it stays) to 1 (it goes to the average).";
    public static final String OMSLINESMOOTHERMCMASTER_P_DENSIFY_DESCRIPTION = "The optional maximum distance between the vertices, added before the smoothing.";
    public static final String OMSLINESMOOTHERMCMASTER_P_SIMPLIFY_DESCRIPTION = "The optional tolerance of a simplification after the smoothing.";
    public static final String OMSLINESMOOTHERMCMASTER_OUT_VECTOR_DESCRIPTION = "The vector with smoothed features.";

    public boolean doParallel = false;

    private static final double SAMEPOINTTHRESHOLD = 0.1;
    private GeometryFactory gF = GeometryUtilities.gf();

    private double densify = -1;
    private double simplify = -1;

    private List<SimpleFeature> linesList;

    private STRtree linesIndex = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }

        if (pDensify != null) {
            densify = pDensify;
        }
        if (pSimplify != null) {
            simplify = pSimplify;
        }

        outVector = new DefaultFeatureCollection();

        pm.message("Collecting geometries...");
        linesList = FeatureUtilities.featureCollectionToList(inVector);
        pm.message("Creating indexes...");
        linesIndex = FeatureUtilities.featureCollectionToSTRtree(inVector);
        int size = inVector.size();
        FeatureGeometrySubstitutor fGS = new FeatureGeometrySubstitutor(inVector.getSchema());

        List<SimpleFeature> smoothedLines = Collections.synchronizedList(new ArrayList<>(linesList.size()));



        pm.beginTask("Smoothing features...", size);
        
        Stream<SimpleFeature> stream;
        if(doParallel) {
            stream = linesList.parallelStream();
        }else {
            stream = linesList.stream();
        }
        stream.forEach(line -> {
            List<LineString> lsList = smoothGeometries(line);
            if (lsList.size() != 0) {
                LineString[] lsArray = (LineString[]) lsList.toArray(new LineString[lsList.size()]);
                MultiLineString multiLineString = gF.createMultiLineString(lsArray);
                SimpleFeature newFeature = fGS.substituteGeometry(line, multiLineString);
                smoothedLines.add(newFeature);
            }
            pm.worked(1);
        });
        
        ((DefaultFeatureCollection) outVector).addAll(smoothedLines);
        pm.done();
    }

    private List<LineString> smoothGeometries( SimpleFeature line ) {
        Geometry geometry = (Geometry) line.getDefaultGeometry();
        List<LineString> lsList = new ArrayList<LineString>();
        int numGeometries = geometry.getNumGeometries();
        for( int i = 0; i < numGeometries; i++ ) {
            Geometry geometryN = geometry.getGeometryN(i);
            double length = geometryN.getLength();
            Coordinate[] smoothedArray = geometryN.getCoordinates();
            Coordinate first = smoothedArray[0];
            Coordinate last = smoothedArray[smoothedArray.length - 1];
            if (length <= pLimit) {
                // if it is circle remove it, else just do not smooth it
                if (first.distance(last) < SAMEPOINTTHRESHOLD) {
                    continue;
                }
                // check if the line is an error lying around somewhere
                if (isAlone(line, geometryN)) {
                    continue;
                }
            } else {
                if (densify != -1) {
                    geometryN = Densifier.densify(geometryN, pDensify);
                }
                List<Coordinate> smoothedCoords = Collections.emptyList();;
                FeatureSlidingAverage fSA = new FeatureSlidingAverage(geometryN);
                smoothedCoords = fSA.smooth(pLookahead, false, pSlide);

                if (smoothedCoords != null) {
                    smoothedArray = (Coordinate[]) smoothedCoords.toArray(new Coordinate[smoothedCoords.size()]);
                } else {
                    smoothedArray = geometryN.getCoordinates();
                }
            }

            LineString lineString = gF.createLineString(smoothedArray);

            if (simplify != -1) {
                TopologyPreservingSimplifier tpSimplifier = new TopologyPreservingSimplifier(lineString);
                tpSimplifier.setDistanceTolerance(pSimplify);
                lineString = (LineString) tpSimplifier.getResultGeometry();
            }

            lsList.add(lineString);
        }
        return lsList;
    }

    /**
     * Checks if the given geometry is connected to any other line.
     *
     * @param feature the feature of the geometry, that is not checked against itself.
     * @param geometryN the geometry to test.
     * @return true if the geometry is alone in the space, i.e. not connected at
     *              one of the ends to any other geometry.
     */
    @SuppressWarnings("unchecked")
    private boolean isAlone( SimpleFeature feature, Geometry geometryN ) {
        Coordinate[] coordinates = geometryN.getCoordinates();
        if (coordinates.length > 1) {
            Coordinate first = coordinates[0];
            Coordinate last = coordinates[coordinates.length - 1];
            Envelope env = geometryN.getEnvelopeInternal();
            env.expandBy(SAMEPOINTTHRESHOLD*3/2);
            List<SimpleFeature> nearLinesList = linesIndex.query(env);
            for( SimpleFeature line : nearLinesList ) {
                if (line.getID().equals(feature.getID())) {
                    // the index has the line itself
                    continue;
                }
                Geometry lineGeom = (Geometry) line.getDefaultGeometry();
                int numGeometries = lineGeom.getNumGeometries();
                for( int i = 0; i < numGeometries; i++ ) {
                    Geometry subGeom = lineGeom.getGeometryN(i);
                    Coordinate[] lineCoordinates = subGeom.getCoordinates();
                    if (lineCoordinates.length < 2) {
                        continue;
                    } else {
                        Coordinate tmpFirst = lineCoordinates[0];
                        Coordinate tmpLast = lineCoordinates[lineCoordinates.length - 1];
                        if (tmpFirst.distance(first) < SAMEPOINTTHRESHOLD || tmpFirst.distance(last) < SAMEPOINTTHRESHOLD
                                || tmpLast.distance(first) < SAMEPOINTTHRESHOLD || tmpLast.distance(last) < SAMEPOINTTHRESHOLD) {
                            return false;
                        }
                    }
                }
            }
        }
        // 1 point line or no connection, mark it as alone for removal
        return true;
    }

    /**
     * An utility method to use the module with default values and shapefiles.
     * 
     * <p>
     * This will use the windowed average and a density of 0.2, simplification threshold of 0.1
     * and a lookahead of 13, as well as a length filter of 10.
     * </p>
     * 
     * @param shapePath the input file.
     * @param outPath the output smoothed path.
     * @throws Exception
     */
    public static void defaultSmoothShapefile( String shapePath, String outPath ) throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
        SimpleFeatureCollection initialFC = OmsShapefileFeatureReader.readShapefile(shapePath);

        OmsLineSmootherMcMaster smoother = new OmsLineSmootherMcMaster();
        smoother.pm = pm;
        smoother.pLimit = 10;
        smoother.inVector = initialFC;
        smoother.pLookahead = 13;
        // smoother.pSlide = 1;
        smoother.pDensify = 0.2;
        smoother.pSimplify = 0.01;
        smoother.process();

        SimpleFeatureCollection smoothedFeatures = smoother.outVector;

        OmsShapefileFeatureWriter.writeShapefile(outPath, smoothedFeatures, pm);
    }

}
