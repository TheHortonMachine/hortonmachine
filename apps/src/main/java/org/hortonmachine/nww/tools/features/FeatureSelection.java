/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.nww.tools.features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.nww.layers.defaults.NwwFeatureLayer;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;

/**
 * The features selected on a layer of the viewer, as by the identify or the select by box modes.
 * It doesn't change: a new selection replaces it.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class FeatureSelection {

    private final Layer layer;
    private final List<SimpleFeature> features;
    private final Position point;

    /**
     * @param layer the layer of the features.
     * @param features the selected features, in longitude/latitude, possibly none.
     * @param point the clicked point, for the features identified there, or <code>null</code>.
     */
    public FeatureSelection( Layer layer, List<SimpleFeature> features, Position point ) {
        this.layer = layer;
        this.features = Collections.unmodifiableList(new ArrayList<>(features));
        this.point = point;
    }

    /**
     * @return the layer of the features.
     */
    public Layer getLayer() {
        return layer;
    }

    /**
     * @return the selected features, in longitude/latitude.
     */
    public List<SimpleFeature> getFeatures() {
        return features;
    }

    /**
     * @return the clicked point, for the features identified there, or <code>null</code>.
     */
    public Position getPoint() {
        return point;
    }

    public boolean isEmpty() {
        return features.isEmpty();
    }

    /**
     * @return the features as a collection, <code>null</code> if there are none.
     */
    public SimpleFeatureCollection toFeatureCollection() {
        if (features.isEmpty()) {
            return null;
        }
        ListFeatureCollection collection = new ListFeatureCollection(features.get(0).getFeatureType());
        collection.addAll(features);
        return collection;
    }

    /**
     * @return the envelope of the geometries, in longitude/latitude, empty if there are none.
     */
    public Envelope getEnvelope() {
        Envelope envelope = new Envelope();
        for( SimpleFeature feature : features ) {
            Object geometry = feature.getDefaultGeometry();
            if (geometry instanceof Geometry) {
                envelope.expandToInclude(((Geometry) geometry).getEnvelopeInternal());
            }
        }
        return envelope;
    }

    /**
     * Select the same features on the layer that replaced the layer of the selection, as after a
     * filter or a new style, by their ids.
     *
     * @param replacement the layer that replaced the one of the selection.
     * @return the selection of the features found on the replacement, possibly none.
     */
    public FeatureSelection onReplacement( Layer replacement ) {
        List<SimpleFeature> found = new ArrayList<>();
        if (replacement instanceof NwwFeatureLayer && !features.isEmpty()) {
            Set<String> ids = new HashSet<>();
            for( SimpleFeature feature : features ) {
                ids.add(feature.getID());
            }
            // the features are looked for only where the selected ones are, so that the layers
            // reading from a database don't read all of them
            Envelope envelope = getEnvelope();
            if (!envelope.isNull()) {
                try {
                    Geometry area = new GeometryFactory().toGeometry(envelope);
                    for( SimpleFeature feature : ((NwwFeatureLayer) replacement).queryFeatures(area) ) {
                        if (ids.contains(feature.getID())) {
                            found.add(feature);
                        }
                    }
                } catch (IllegalStateException e) {
                    // too many features to look among: the selection is cleared
                    e.printStackTrace();
                }
            }
        }
        return new FeatureSelection(replacement, found, point);
    }

    /**
     * @param collection features in longitude/latitude.
     * @param area an area in longitude/latitude.
     * @return the features whose geometry touches the area.
     * @see NwwFeatureLayer#queryFeatures(Geometry), which reads only the features in the area
     *          for the layers reading from a database.
     */
    public static List<SimpleFeature> query( SimpleFeatureCollection collection, Geometry area ) {
        return NwwFeatureLayer.query(collection, area);
    }
}
