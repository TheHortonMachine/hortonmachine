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
package org.jgrasstools.gears.modules.v.intersections;

import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities.GEOMETRYTYPE;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

@Description("Finds intersection geometries in feature collections")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Vector")
@Label(JGTConstants.VECTORPROCESSING)
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class IntersectionFinder extends JGTModel {

    @Description("The map to test for intersections.")
    @In
    public SimpleFeatureCollection inMap = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The intersections points map.")
    @Out
    public SimpleFeatureCollection outPointsMap = null;

    @Description("The intersections lines map.")
    @Out
    public SimpleFeatureCollection outLinesMap = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outPointsMap == null && outLinesMap == null, doReset)) {
            return;
        }

        outPointsMap = FeatureCollections.newCollection();
        outLinesMap = FeatureCollections.newCollection();

        GEOMETRYTYPE geometryType = GeometryUtilities.getGeometryType(inMap.getSchema()
                .getGeometryDescriptor().getType());
        switch( geometryType ) {
        case LINE:
        case MULTILINE:
            intersectLines();
            break;
        case POLYGON:
        case MULTIPOLYGON:
            throw new ModelsIllegalargumentException("The module doesn't work for polygons yet.",
                    this);
        default:
            throw new ModelsIllegalargumentException("The module doesn't work for points.", this
                    .getClass().getSimpleName());
        }

    }

    private void intersectLines() {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("pointintersections");
        b.setCRS(inMap.getSchema().getCoordinateReferenceSystem());
        b.add("the_geom", Point.class);
        SimpleFeatureType pointType = b.buildFeatureType();

        b = new SimpleFeatureTypeBuilder();
        b.setName("lineintersections");
        b.setCRS(inMap.getSchema().getCoordinateReferenceSystem());
        b.add("the_geom", LineString.class);
        SimpleFeatureType linesType = b.buildFeatureType();

        int size = inMap.size();
        List<Geometry> geometriesList = new ArrayList<Geometry>();
        FeatureIterator<SimpleFeature> linesIterator = inMap.features();
        pm.beginTask("Collecting geometries...", size);
        while( linesIterator.hasNext() ) {
            SimpleFeature feature = linesIterator.next();
            Geometry line = (Geometry) feature.getDefaultGeometry();
            geometriesList.add(line);
            pm.worked(1);
        }
        pm.done();
        linesIterator.close();

        int id = 0;
        pm.beginTask("Checking intersections...", size);
        for( int i = 0; i < size; i++ ) {
            Geometry line = geometriesList.get(i);
            PreparedGeometry preparedLine = PreparedGeometryFactory.prepare(line);
            for( int j = i + 1; j < size; j++ ) {
                Geometry otherGeometry = geometriesList.get(j);

                if (preparedLine.intersects(otherGeometry)) {
                    Geometry intersection = line.intersection(otherGeometry);
                    int numGeometries = intersection.getNumGeometries();
                    for( int k = 0; k < numGeometries; k++ ) {
                        Geometry geometryN = intersection.getGeometryN(k);

                        if (geometryN instanceof Point) {
                            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(pointType);
                            Point p = (Point) geometryN;
                            Object[] values = new Object[]{p};
                            builder.addAll(values);
                            SimpleFeature feature = builder.buildFeature(pointType.getTypeName()
                                    + "." + id++);
                            outPointsMap.add(feature);
                        } else if (geometryN instanceof LineString) {
                            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(linesType);
                            LineString l = (LineString) geometryN;
                            Object[] values = new Object[]{l};
                            builder.addAll(values);
                            SimpleFeature feature = builder.buildFeature(linesType.getTypeName()
                                    + "." + id++);
                            outLinesMap.add(feature);
                        }

                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
    }
}
