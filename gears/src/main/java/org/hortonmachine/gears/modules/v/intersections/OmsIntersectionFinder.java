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
package org.hortonmachine.gears.modules.v.intersections;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_IN_MAP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_OUT_LINES_MAP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_OUT_POINTS_MAP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_STATUS;

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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

@Description(OMSINTERSECTIONFINDER_DESCRIPTION)
@Author(name = OMSINTERSECTIONFINDER_AUTHORNAMES, contact = OMSINTERSECTIONFINDER_AUTHORCONTACTS)
@Keywords(OMSINTERSECTIONFINDER_KEYWORDS)
@Label(OMSINTERSECTIONFINDER_LABEL)
@Name(OMSINTERSECTIONFINDER_NAME)
@Status(OMSINTERSECTIONFINDER_STATUS)
@License(OMSINTERSECTIONFINDER_LICENSE)
public class OmsIntersectionFinder extends HMModel {

    @Description(OMSINTERSECTIONFINDER_IN_MAP_DESCRIPTION)
    @In
    public SimpleFeatureCollection inMap = null;

    @Description(OMSINTERSECTIONFINDER_OUT_POINTS_MAP_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outPointsMap = null;

    @Description(OMSINTERSECTIONFINDER_OUT_LINES_MAP_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outLinesMap = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outPointsMap == null && outLinesMap == null, doReset)) {
            return;
        }

        outPointsMap = new DefaultFeatureCollection();
        outLinesMap = new DefaultFeatureCollection();

        EGeometryType geometryType = EGeometryType.forGeometryType(inMap.getSchema().getGeometryDescriptor().getType());
        switch( geometryType ) {
        case LINESTRING:
        case MULTILINESTRING:
            intersectLines();
            break;
        case POLYGON:
        case MULTIPOLYGON:
            throw new ModelsIllegalargumentException("The module doesn't work for polygons yet.", this, pm);
        default:
            throw new ModelsIllegalargumentException("The module doesn't work for points.", this, pm);
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

        List<Geometry> geometriesList = FeatureUtilities.featureCollectionToGeometriesList(inMap, true, null);

        int id = 0;
        pm.beginTask("Checking intersections...", size);
        for( int i = 0; i < size; i++ ) {
            LineString line = (LineString) geometriesList.get(i);
            PreparedGeometry preparedLine = PreparedGeometryFactory.prepare(line);
            for( int j = i + 1; j < size; j++ ) {
                LineString otherLine = (LineString) geometriesList.get(j);

                if (preparedLine.intersects(otherLine)) {
                    Geometry intersection = line.intersection(otherLine);
                    int numGeometries = intersection.getNumGeometries();
                    if (numGeometries < 3) {
                        Point start1 = line.getStartPoint();
                        Point end1 = line.getEndPoint();
                        Point start2 = otherLine.getStartPoint();
                        Point end2 = otherLine.getEndPoint();
                        if (numGeometries == 1) {
                            // single intersection, control if it is not just two connected lines
                            if (start1.distance(end2) < NumericsUtilities.D_TOLERANCE
                                    || start1.distance(start2) < NumericsUtilities.D_TOLERANCE
                                    || end1.distance(start2) < NumericsUtilities.D_TOLERANCE
                                    || end1.distance(end2) < NumericsUtilities.D_TOLERANCE) {
                                // it is the same point
                                continue;
                            }
                        } else if (numGeometries == 2) {
                            // could still be connected lines
                            if ((start1.distance(end2) < NumericsUtilities.D_TOLERANCE && start2.distance(end1) < NumericsUtilities.D_TOLERANCE)
                                    || (start1.distance(start2) < NumericsUtilities.D_TOLERANCE && end1.distance(end2) < NumericsUtilities.D_TOLERANCE)) {
                                // it is the same point
                                continue;
                            }
                        }
                    }

                    for( int k = 0; k < numGeometries; k++ ) {
                        Geometry geometryN = intersection.getGeometryN(k);

                        if (geometryN instanceof Point) {
                            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(pointType);
                            Point p = (Point) geometryN;
                            Object[] values = new Object[]{p};
                            builder.addAll(values);
                            SimpleFeature feature = builder.buildFeature(pointType.getTypeName() + "." + id++);
                            ((DefaultFeatureCollection) outPointsMap).add(feature);
                        } else if (geometryN instanceof LineString) {
                            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(linesType);
                            LineString l = (LineString) geometryN;
                            Object[] values = new Object[]{l};
                            builder.addAll(values);
                            SimpleFeature feature = builder.buildFeature(linesType.getTypeName() + "." + id++);
                            ((DefaultFeatureCollection) outLinesMap).add(feature);
                        }

                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
    }
}
