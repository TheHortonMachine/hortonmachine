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
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherJaitools.OMSLINESMOOTHERJAITOOLS_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherJaitools.OMSLINESMOOTHERJAITOOLS_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherJaitools.OMSLINESMOOTHERJAITOOLS_BIBLIOGRAPHY;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherJaitools.OMSLINESMOOTHERJAITOOLS_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherJaitools.OMSLINESMOOTHERJAITOOLS_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherJaitools.OMSLINESMOOTHERJAITOOLS_KEYWORDS;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherJaitools.OMSLINESMOOTHERJAITOOLS_LABEL;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherJaitools.OMSLINESMOOTHERJAITOOLS_LICENSE;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherJaitools.OMSLINESMOOTHERJAITOOLS_NAME;
import static org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherJaitools.OMSLINESMOOTHERJAITOOLS_STATUS;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.imagen.media.contour.LineSmoother;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureGeometrySubstitutor;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

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

@Description(OMSLINESMOOTHERJAITOOLS_DESCRIPTION)
@Documentation(OMSLINESMOOTHERJAITOOLS_DOCUMENTATION)
@Bibliography(OMSLINESMOOTHERJAITOOLS_BIBLIOGRAPHY)
@Author(name = OMSLINESMOOTHERJAITOOLS_AUTHORNAMES, contact = OMSLINESMOOTHERJAITOOLS_AUTHORCONTACTS)
@Keywords(OMSLINESMOOTHERJAITOOLS_KEYWORDS)
@Label(OMSLINESMOOTHERJAITOOLS_LABEL)
@Name(OMSLINESMOOTHERJAITOOLS_NAME)
@Status(OMSLINESMOOTHERJAITOOLS_STATUS)
@License(OMSLINESMOOTHERJAITOOLS_LICENSE)
public class OmsLineSmootherJaitools extends HMModel {

    @Description(OMSLINESMOOTHERJAITOOLS_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector;

    @Description(OMSLINESMOOTHERJAITOOLS_P_ALPHA_DESCRIPTION)
    @In
    public double pAlpha = 0;

    public static final String OMSLINESMOOTHERJAITOOLS_DESCRIPTION = "Smooths lines with cubic Bezier curves through their vertices, with the line smoother of the jaitools project.";
    public static final String OMSLINESMOOTHERJAITOOLS_DOCUMENTATION = """
            The smoothed line passes through all the vertices of the original one: each segment is replaced \
            by a cubic Bezier curve of 10 vertices, with the control points set by the neighbouring segments, \
            so the line gets 9 new vertices for each segment. With an alpha of 1 the curves are the straight \
            segments, so the line keeps its shape, only with more vertices; with 0 they are the roundest. \
            Since the curve doesn't cut the corners, on the staircase lines of a vectorized raster it rounds \
            the steps instead of removing them: simplify the lines first, with VectorSimplifier, to smooth \
            their general shape.

            Only the lines of the features are smoothed, as multi lines: features without lines are left \
            out.
            """;
    public static final String OMSLINESMOOTHERJAITOOLS_BIBLIOGRAPHY = "Shemanarev, M. Interpolation with Bezier Curves: a very simple method of smoothing polygons. http://www.antigrain.com/research/bezier_interpolation/index.html";
    public static final String OMSLINESMOOTHERJAITOOLS_KEYWORDS = "Smoothing, Vector, Bezier";
    public static final String OMSLINESMOOTHERJAITOOLS_LABEL = VECTORPROCESSING;
    public static final String OMSLINESMOOTHERJAITOOLS_NAME = "linesmootherjai";
    public static final int OMSLINESMOOTHERJAITOOLS_STATUS = 40;
    public static final String OMSLINESMOOTHERJAITOOLS_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSLINESMOOTHERJAITOOLS_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSLINESMOOTHERJAITOOLS_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSLINESMOOTHERJAITOOLS_IN_VECTOR_DESCRIPTION = "The vector containing the lines to be smoothed.";
    public static final String OMSLINESMOOTHERJAITOOLS_P_ALPHA_DESCRIPTION = "A value between 0 and 1 (inclusive) specifying the tightness of fit of the smoothed line: 0 is the loosest, 1 keeps the straight segments.";
    public static final String OMSLINESMOOTHERJAITOOLS_OUT_VECTOR_DESCRIPTION = "The smoothed features.";

    @Description(OMSLINESMOOTHERJAITOOLS_OUT_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector;

    private GeometryFactory gF = GeometryUtilities.gf();

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }
        outVector = new DefaultFeatureCollection();

        pm.message("Collecting geometries...");
        List<SimpleFeature> linesList = FeatureUtilities.featureCollectionToList(inVector);
        int size = inVector.size();
        FeatureGeometrySubstitutor fGS = new FeatureGeometrySubstitutor(inVector.getSchema());
        pm.beginTask("Smoothing features...", size);
        LineSmoother smoother = new LineSmoother(gF);
        for( SimpleFeature line : linesList ) {
            Geometry geometry = (Geometry) line.getDefaultGeometry();
            int numGeometries = geometry.getNumGeometries();
            List<LineString> smoothedList = new ArrayList<LineString>();
            for( int i = 0; i < numGeometries; i++ ) {
                Geometry geometryN = geometry.getGeometryN(i);
                if (geometryN instanceof LineString) {
                    LineString lineString = (LineString) geometryN;
                    LineString smoothed = smoother.smooth(lineString, pAlpha);
                    smoothedList.add(smoothed);
                }
            }
            if (smoothedList.size() != 0) {
                LineString[] lsArray = (LineString[]) smoothedList.toArray(new LineString[smoothedList.size()]);
                MultiLineString multiLineString = gF.createMultiLineString(lsArray);
                SimpleFeature newFeature = fGS.substituteGeometry(line, multiLineString);
                ((DefaultFeatureCollection) outVector).add(newFeature);
            }
            pm.worked(1);
        }
        pm.done();
    }

}
