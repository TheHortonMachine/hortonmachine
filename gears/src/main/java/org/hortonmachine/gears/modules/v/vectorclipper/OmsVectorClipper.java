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
package org.hortonmachine.gears.modules.v.vectorclipper;

import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.modules.v.vectorclipper.OmsVectorClipper.OMSVECTORCLIPPER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.vectorclipper.OmsVectorClipper.OMSVECTORCLIPPER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.vectorclipper.OmsVectorClipper.OMSVECTORCLIPPER_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorclipper.OmsVectorClipper.OMSVECTORCLIPPER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.vectorclipper.OmsVectorClipper.OMSVECTORCLIPPER_KEYWORDS;
import static org.hortonmachine.gears.modules.v.vectorclipper.OmsVectorClipper.OMSVECTORCLIPPER_LABEL;
import static org.hortonmachine.gears.modules.v.vectorclipper.OmsVectorClipper.OMSVECTORCLIPPER_LICENSE;
import static org.hortonmachine.gears.modules.v.vectorclipper.OmsVectorClipper.OMSVECTORCLIPPER_NAME;
import static org.hortonmachine.gears.modules.v.vectorclipper.OmsVectorClipper.OMSVECTORCLIPPER_STATUS;

import java.util.ArrayList;
import java.util.List;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.features.FeatureGeometrySubstitutor;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.geom.util.LineStringExtracter;
import org.locationtech.jts.geom.util.PointExtracter;
import org.locationtech.jts.geom.util.PolygonExtracter;
import org.locationtech.jts.operation.union.UnaryUnionOp;

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

@Description(OMSVECTORCLIPPER_DESCRIPTION)
@Documentation(OMSVECTORCLIPPER_DOCUMENTATION)
@Author(name = OMSVECTORCLIPPER_AUTHORNAMES, contact = OMSVECTORCLIPPER_AUTHORCONTACTS)
@Keywords(OMSVECTORCLIPPER_KEYWORDS)
@Label(OMSVECTORCLIPPER_LABEL)
@Name(OMSVECTORCLIPPER_NAME)
@Status(OMSVECTORCLIPPER_STATUS)
@License(OMSVECTORCLIPPER_LICENSE)
public class OmsVectorClipper extends HMModel {

    @Description(OMSVECTORCLIPPER_IN_MAP_DESCRIPTION)
    @In
    public SimpleFeatureCollection inMap = null;

    @Description(OMSVECTORCLIPPER_IN_CLIPPER_DESCRIPTION)
    @In
    public SimpleFeatureCollection inClipper = null;

    @Description(OMSVECTORCLIPPER_OUT_MAP_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outMap = null;

    public static final String OMSVECTORCLIPPER_DESCRIPTION = "Cuts the features of a vector to the area of the polygons of another vector, keeping their attributes.";
    public static final String OMSVECTORCLIPPER_DOCUMENTATION = """
            The clipping polygons are merged before the cut, so a feature comes out once, with its part \
            inside any of them, also when it crosses several. Only the attributes of the clipped vector \
            are kept: to also get those of the polygons, with a piece of the feature for each polygon, \
            use VectorIntersector.

            The result has the multi geometry type of the clipped vector, as MultiLineString for lines. \
            The parts of the cut of a lower dimension are left out, as the point where a line touches a \
            polygon, and so are the features that only touch the polygons along their border.

            The two vectors must have the same coordinate reference system.
            """;
    public static final String OMSVECTORCLIPPER_KEYWORDS = "JTS, Clip, Vector";
    public static final String OMSVECTORCLIPPER_LABEL = VECTORPROCESSING;
    public static final String OMSVECTORCLIPPER_NAME = "clipper";
    public static final int OMSVECTORCLIPPER_STATUS = 40;
    public static final String OMSVECTORCLIPPER_LICENSE = "http://www.gnu.org/licenses/gpl-3.0.html";
    public static final String OMSVECTORCLIPPER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSVECTORCLIPPER_AUTHORCONTACTS = "www.hydrologis.com";
    public static final String OMSVECTORCLIPPER_IN_MAP_DESCRIPTION = "The vector to clip: points, lines or polygons.";
    public static final String OMSVECTORCLIPPER_IN_CLIPPER_DESCRIPTION = "The polygons to clip with.";
    public static final String OMSVECTORCLIPPER_OUT_MAP_DESCRIPTION = "The clipped features, with their attributes.";

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMap == null, doReset)) {
            return;
        }
        checkNull(inMap, inClipper);

        int dimension = getDimension(inMap, this);
        checkClippingPolygons(inMap, inClipper, this);

        pm.beginTask("Merging the clipping polygons...", IHMProgressMonitor.UNKNOWN);
        List<Geometry> clipperGeometries = FeatureUtilities.featureCollectionToGeometriesList(inClipper, false, null);
        Geometry clipper = UnaryUnionOp.union(clipperGeometries);
        pm.done();

        FeatureGeometrySubstitutor substitutor = new FeatureGeometrySubstitutor(inMap.getSchema(), multiClass(dimension));
        DefaultFeatureCollection outFC = new DefaultFeatureCollection("clipped", substitutor.getNewFeatureType());
        outMap = outFC;
        if (clipper == null || clipper.isEmpty()) {
            return;
        }
        PreparedGeometry preparedClipper = PreparedGeometryFactory.prepare(clipper);

        List<SimpleFeature> features = FeatureUtilities.featureCollectionToList(inMap);
        int touching = 0;
        pm.beginTask("Clipping...", features.size());
        for( SimpleFeature feature : features ) {
            pm.worked(1);
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            if (geometry == null || geometry.isEmpty() || !preparedClipper.intersects(geometry)) {
                continue;
            }
            Geometry clipped;
            if (preparedClipper.covers(geometry)) {
                clipped = geometry;
            } else {
                clipped = geometry.intersection(clipper);
            }
            Geometry parts = partsOfDimension(clipped, dimension);
            if (parts == null) {
                touching++;
                continue;
            }
            outFC.add(substitutor.substituteGeometry(feature, parts));
        }
        pm.done();
        if (touching > 0) {
            pm.message(touching + " features only touch the clipping polygons and have been left out.");
        }
    }

    /**
     * The dimension of the geometries of a vector: 0 for points, 1 for lines, 2 for polygons.
     *
     * <p>For a vector of generic geometries, the one of its first geometry.</p>
     *
     * @param vector the vector.
     * @param model the calling module, for the errors.
     * @return the dimension.
     */
    public static int getDimension( SimpleFeatureCollection vector, HMModel model ) {
        EGeometryType type = EGeometryType.forGeometryDescriptor(vector.getSchema().getGeometryDescriptor());
        if (type != null) {
            switch( type ) {
            case POINT:
            case MULTIPOINT:
                return 0;
            case LINESTRING:
            case MULTILINESTRING:
                return 1;
            default:
                return 2;
            }
        }
        for( Geometry geometry : FeatureUtilities.featureCollectionToGeometriesList(vector, false, null) ) {
            return geometry.getDimension();
        }
        throw new ModelsIllegalargumentException("The vector has no geometries: " + vector.getSchema().getTypeName(), model);
    }

    /**
     * Checks that the cutting vector is of polygons and in the crs of the vector to cut.
     *
     * @param vector the vector to cut.
     * @param polygons the cutting vector.
     * @param model the calling module, for the errors.
     */
    public static void checkClippingPolygons( SimpleFeatureCollection vector, SimpleFeatureCollection polygons, HMModel model ) {
        if (getDimension(polygons, model) != 2) {
            throw new ModelsIllegalargumentException("The vector to cut with needs to be of polygons.", model);
        }
        CoordinateReferenceSystem crs = vector.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem polygonsCrs = polygons.getSchema().getCoordinateReferenceSystem();
        if (crs != null && polygonsCrs != null && !HMCrsRegistry.crsEquals(crs, polygonsCrs)) {
            throw new ModelsIllegalargumentException(
                    "The two vectors have different coordinate reference systems: reproject one of them first.", model);
        }
    }

    /**
     * @param dimension 0, 1 or 2.
     * @return the multi geometry class of the dimension.
     */
    public static Class< ? > multiClass( int dimension ) {
        switch( dimension ) {
        case 0:
            return MultiPoint.class;
        case 1:
            return MultiLineString.class;
        default:
            return MultiPolygon.class;
        }
    }

    /**
     * The parts of a geometry of a dimension, as a multi geometry.
     *
     * @param geometry the geometry, as the result of a cut, also a collection of mixed types.
     * @param dimension the dimension of the parts to keep: 0, 1 or 2.
     * @return the multi geometry of the parts or <code>null</code>, if there are none.
     */
    @SuppressWarnings("unchecked")
    public static Geometry partsOfDimension( Geometry geometry, int dimension ) {
        GeometryFactory gf = geometry.getFactory();
        // the extracters return a read only list for a single geometry
        List<Geometry> parts = new ArrayList<>();
        switch( dimension ) {
        case 0:
            parts.addAll(PointExtracter.getPoints(geometry));
            break;
        case 1:
            parts.addAll(LineStringExtracter.getLines(geometry));
            break;
        default:
            parts.addAll(PolygonExtracter.getPolygons(geometry));
            break;
        }
        parts.removeIf(Geometry::isEmpty);
        if (parts.isEmpty()) {
            return null;
        }
        switch( dimension ) {
        case 0:
            return gf.createMultiPoint(GeometryFactory.toPointArray(parts));
        case 1:
            return gf.createMultiLineString(GeometryFactory.toLineStringArray(parts));
        default:
            return gf.createMultiPolygon(GeometryFactory.toPolygonArray(parts));
        }
    }

}
