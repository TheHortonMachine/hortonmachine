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
package org.hortonmachine.gears.modules.v.vectoroverlayoperators;

import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_KEYWORDS;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_LABEL;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_LICENSE;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_NAME;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector.OMSVECTORINTERSECTOR_STATUS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.vectorclipper.OmsVectorClipper;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;

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

@Description(OMSVECTORINTERSECTOR_DESCRIPTION)
@Documentation(OMSVECTORINTERSECTOR_DOCUMENTATION)
@Author(name = OMSVECTORINTERSECTOR_AUTHORNAMES, contact = OMSVECTORINTERSECTOR_AUTHORCONTACTS)
@Keywords(OMSVECTORINTERSECTOR_KEYWORDS)
@Label(OMSVECTORINTERSECTOR_LABEL)
@Name(OMSVECTORINTERSECTOR_NAME)
@Status(OMSVECTORINTERSECTOR_STATUS)
@License(OMSVECTORINTERSECTOR_LICENSE)
public class OmsVectorIntersector extends HMModel {

    @Description(OMSVECTORINTERSECTOR_IN_MAP1_DESCRIPTION)
    @In
    public SimpleFeatureCollection inMap1 = null;

    @Description(OMSVECTORINTERSECTOR_IN_MAP2_DESCRIPTION)
    @In
    public SimpleFeatureCollection inMap2 = null;

    @Description(OMSVECTORINTERSECTOR_OUT_MAP_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outMap = null;

    public static final String OMSVECTORINTERSECTOR_DESCRIPTION = "Intersects the features of a vector with the polygons of another vector, keeping the attributes of both.";
    public static final String OMSVECTORINTERSECTOR_DOCUMENTATION = """
            Each feature is cut by each polygon it overlaps, and every piece becomes a feature with the \
            attributes of the feature and of the polygon: a river crossing three municipalities gives \
            three pieces, each with the name of its municipality. A field of the polygons with the name \
            of a field of the features, ignoring the case, gets the suffix _2. Where the polygons overlap, \
            the pieces overlap too. To cut the features to the area of the polygons, without their \
            attributes and with a single piece per feature, use VectorClipper.

            The result has the multi geometry type of the first vector, as MultiLineString for lines. \
            The parts of the cut of a lower dimension are left out, as the point where a line touches a \
            polygon.

            The two vectors must have the same coordinate reference system.
            """;
    public static final String OMSVECTORINTERSECTOR_KEYWORDS = "Vector, Intersect, Overlay, Attributes";
    public static final String OMSVECTORINTERSECTOR_LABEL = VECTORPROCESSING;
    public static final String OMSVECTORINTERSECTOR_NAME = "vectorintersector";
    public static final int OMSVECTORINTERSECTOR_STATUS = 40;
    public static final String OMSVECTORINTERSECTOR_LICENSE = "http://www.gnu.org/licenses/gpl-3.0.html";
    public static final String OMSVECTORINTERSECTOR_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSVECTORINTERSECTOR_AUTHORCONTACTS = "www.hydrologis.com";
    public static final String OMSVECTORINTERSECTOR_IN_MAP1_DESCRIPTION = "The vector to intersect: points, lines or polygons.";
    public static final String OMSVECTORINTERSECTOR_IN_MAP2_DESCRIPTION = "The polygons to intersect with.";
    public static final String OMSVECTORINTERSECTOR_OUT_MAP_DESCRIPTION = "The pieces of the features, with the attributes of the features and of the polygons.";

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMap == null, doReset)) {
            return;
        }
        checkNull(inMap1, inMap2);

        int dimension = OmsVectorClipper.getDimension(inMap1, this);
        OmsVectorClipper.checkClippingPolygons(inMap1, inMap2, this);

        SimpleFeatureType type1 = inMap1.getSchema();
        SimpleFeatureType type2 = inMap2.getSchema();
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("intersection");
        b.setCRS(type1.getCoordinateReferenceSystem());
        b.add("the_geom", OmsVectorClipper.multiClass(dimension));
        Set<String> names = new HashSet<>();
        names.add("the_geom");
        List<String> fields1 = new ArrayList<>();
        for( AttributeDescriptor descriptor : type1.getAttributeDescriptors() ) {
            if (descriptor instanceof GeometryDescriptor) {
                continue;
            }
            String name = descriptor.getLocalName();
            fields1.add(name);
            names.add(name.toLowerCase(Locale.ROOT));
            b.add(descriptor);
        }
        List<String> fields2 = new ArrayList<>();
        for( AttributeDescriptor descriptor : type2.getAttributeDescriptors() ) {
            if (descriptor instanceof GeometryDescriptor) {
                continue;
            }
            String name = descriptor.getLocalName();
            fields2.add(name);
            String newName = name;
            for( int i = 2; names.contains(newName.toLowerCase(Locale.ROOT)); i++ ) {
                newName = name + "_" + i;
            }
            names.add(newName.toLowerCase(Locale.ROOT));
            AttributeTypeBuilder atb = new AttributeTypeBuilder();
            atb.init(descriptor);
            b.add(atb.buildDescriptor(newName));
        }
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        DefaultFeatureCollection outFC = new DefaultFeatureCollection("intersection", type);
        outMap = outFC;

        STRtree tree = new STRtree();
        for( SimpleFeature polygonFeature : FeatureUtilities.featureCollectionToList(inMap2) ) {
            Geometry polygon = (Geometry) polygonFeature.getDefaultGeometry();
            if (polygon != null && !polygon.isEmpty()) {
                tree.insert(polygon.getEnvelopeInternal(), new IndexedPolygon(polygonFeature,PreparedGeometryFactory.prepare(polygon)));
            }
        }

        List<SimpleFeature> features = FeatureUtilities.featureCollectionToList(inMap1);
        int touching = 0;
        pm.beginTask("Intersecting...", features.size());
        for( SimpleFeature feature : features ) {
            pm.worked(1);
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            if (geometry == null || geometry.isEmpty()) {
                continue;
            }
            for( Object item : tree.query(geometry.getEnvelopeInternal()) ) {
                IndexedPolygon polygon = (IndexedPolygon) item;
                if (!polygon.prepared.intersects(geometry)) {
                    continue;
                }
                Geometry cut;
                if (polygon.prepared.covers(geometry)) {
                    cut = geometry;
                } else {
                    cut = geometry.intersection(polygon.prepared.getGeometry());
                }
                Geometry parts = OmsVectorClipper.partsOfDimension(cut, dimension);
                if (parts == null) {
                    touching++;
                    continue;
                }
                builder.add(parts);
                for( String field : fields1 ) {
                    builder.add(feature.getAttribute(field));
                }
                for( String field : fields2 ) {
                    builder.add(polygon.feature.getAttribute(field));
                }
                outFC.add(builder.buildFeature(null));
            }
        }
        pm.done();
        if (touching > 0) {
            pm.message(touching + " features only touch a polygon along its border: those pieces have been left out.");
        }
    }

    private static class IndexedPolygon {
        final SimpleFeature feature;
        final PreparedGeometry prepared;

        IndexedPolygon( SimpleFeature feature, PreparedGeometry prepared ) {
            this.feature = feature;
            this.prepared = prepared;
        }
    }

}
