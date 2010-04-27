/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.gears.modules.v.smoothing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.features.FeatureGeometrySubstitutor;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

@Description("Collection of Smoothing Algorithms. Type 0: McMasters Sliding Averaging "
        + "Algorithm. The new position of each point "
        + "is the average of the pLookahead  points around. Parameter pSlide is used for "
        + "linear interpolation between old and new position.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Smoothing, Vector")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class LineSmoother extends JGTModel {

    @Description("The features to be smoothed.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> inFeatures;

    @Description("The smoothing type: McMasters smoothing average (0 = default).")
    @In
    public int pType = 0;

    @Description("The number of points to consider in every smoothing step (default = 7).")
    @In
    public int pLookahead = 7;

    @Description("Slide parameter.")
    @In
    public double pSlide = 0.9;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The smoothed features.")
    @Out
    public FeatureCollection<SimpleFeatureType, SimpleFeature> outFeatures;

    private GeometryFactory gF = GeometryUtilities.gf();

    @Execute
    public void process() throws Exception {
        if (!concatOr(outFeatures == null, doReset)) {
            return;
        }
        FeatureIterator<SimpleFeature> inFeatureIterator = inFeatures.features();

        outFeatures = FeatureCollections.newCollection();

        FeatureGeometrySubstitutor fGS = new FeatureGeometrySubstitutor(inFeatures.getSchema());

        int id = 0;
        int size = inFeatures.size();
        pm.beginTask("Smoothing features...", size);
        while( inFeatureIterator.hasNext() ) {
            SimpleFeature feature = inFeatureIterator.next();

            if (fGS == null) {
            }

            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            int numGeometries = geometry.getNumGeometries();

            List<LineString> lsList = new ArrayList<LineString>();
            for( int i = 0; i < numGeometries; i++ ) {
                Geometry geometryN = geometry.getGeometryN(i);

                List<Coordinate> smoothedCoords = Collections.emptyList();;
                switch( pType ) {
                case 0:
                default:
                    FeatureSlidingAverage fSA = new FeatureSlidingAverage(geometryN);
                    smoothedCoords = fSA.smooth(pLookahead, false, pSlide);
                    break;
                }

                Coordinate[] smoothedArray = null;
                if (smoothedCoords != null) {
                    smoothedArray = (Coordinate[]) smoothedCoords
                            .toArray(new Coordinate[smoothedCoords.size()]);
                } else {
                    smoothedArray = geometryN.getCoordinates();
                }
                LineString lineString = gF.createLineString(smoothedArray);
                lsList.add(lineString);
            }
            LineString[] lsArray = (LineString[]) lsList.toArray(new LineString[lsList.size()]);

            MultiLineString mlString = gF.createMultiLineString(lsArray);

            SimpleFeature newFeature = fGS.extendFeature(feature, mlString, id);
            id++;

            outFeatures.add(newFeature);
            pm.worked(1);
        }
        pm.done();

        inFeatures.close(inFeatureIterator);

    }

}
