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
package org.hortonmachine.nww.layers.defaults;

import java.util.ArrayList;
import java.util.List;

import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

/**
 * A layer drawn from a feature collection, as 3D shapes or as image tiles, whose features can be
 * queried, as by the identify mode of the viewer.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public interface NwwFeatureLayer {

    /**
     * @return the features drawn by the layer, in WGS84 longitude/latitude. Collections read from
     *          a store read it again at each use, so they give the edited attributes.
     */
    SimpleFeatureCollection getFeatureCollection();

    /**
     * @return the store the features are read from, to edit their attributes, or
     *          <code>null</code> if they can't be edited.
     */
    default SimpleFeatureStore getFeatureStore() {
        return null;
    }

    /**
     * The features touching an area, as for the identify and the selection. The layers reading
     * from a database read only the features in the area.
     *
     * @param area an area in WGS84 longitude/latitude.
     * @return the features whose geometry touches the area.
     * @throws IllegalStateException if the features can't be read, as when they are too many.
     */
    default List<SimpleFeature> queryFeatures( Geometry area ) {
        return query(getFeatureCollection(), area);
    }

    /**
     * @param collection features in longitude/latitude.
     * @param area an area in longitude/latitude.
     * @return the features of the collection whose geometry touches the area.
     */
    static List<SimpleFeature> query( SimpleFeatureCollection collection, Geometry area ) {
        Envelope box = area.getEnvelopeInternal();
        PreparedGeometry prepared = PreparedGeometryFactory.prepare(area);
        List<SimpleFeature> found = new ArrayList<>();
        try (SimpleFeatureIterator iterator = collection.features()) {
            while( iterator.hasNext() ) {
                SimpleFeature feature = iterator.next();
                Object geometry = feature.getDefaultGeometry();
                if (geometry instanceof Geometry && ((Geometry) geometry).getEnvelopeInternal().intersects(box)
                        && prepared.intersects((Geometry) geometry)) {
                    found.add(feature);
                }
            }
        }
        return found;
    }
}
