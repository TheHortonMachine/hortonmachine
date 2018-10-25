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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_OUT_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_P_DENSIFY_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_P_LIMIT_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_P_LOOK_AHEAD_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_P_SIMPLIFY_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_P_SLIDE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_STATUS;

import java.util.ArrayList;
import java.util.Collections;
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
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

@Description(OMSLINESMOOTHERMCMASTER_DESCRIPTION)
@Documentation(OMSLINESMOOTHERMCMASTER_DOCUMENTATION)
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

    private static final double SAMEPOINTTHRESHOLD = 0.1;
    private GeometryFactory gF = GeometryUtilities.gf();

    private double densify = -1;
    private double simplify = -1;

    private List<SimpleFeature> linesList;

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
        int size = inVector.size();
        FeatureGeometrySubstitutor fGS = new FeatureGeometrySubstitutor(inVector.getSchema());
        pm.beginTask("Smoothing features...", size);
        for( SimpleFeature line : linesList ) {
            Geometry geometry = (Geometry) line.getDefaultGeometry();
            List<LineString> lsList = smoothGeometries(geometry);
            if (lsList.size() != 0) {
                LineString[] lsArray = (LineString[]) lsList.toArray(new LineString[lsList.size()]);
                MultiLineString multiLineString = gF.createMultiLineString(lsArray);
                SimpleFeature newFeature = fGS.substituteGeometry(line, multiLineString);
                ((DefaultFeatureCollection) outVector).add(newFeature);
            }
            pm.worked(1);
        }
        pm.done();
    }

    private List<LineString> smoothGeometries( Geometry geometry ) {
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
                if (isAlone(geometryN)) {
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
     * @param geometryN the geometry to test.
     * @return true if the geometry is alone in the space, i.e. not connected at
     *              one of the ends to any other geometry.
     */
    private boolean isAlone( Geometry geometryN ) {
        Coordinate[] coordinates = geometryN.getCoordinates();
        if (coordinates.length > 1) {
            Coordinate first = coordinates[0];
            Coordinate last = coordinates[coordinates.length - 1];
            for( SimpleFeature line : linesList ) {
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
