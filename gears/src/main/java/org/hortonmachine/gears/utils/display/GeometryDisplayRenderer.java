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
package org.hortonmachine.gears.utils.display;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.utils.images.ImageGenerator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Lineal;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.Polygonal;
import org.locationtech.jts.geom.Puntal;

/**
 * Renders JTS {@link Geometry}s, and collections made only of geometries, as a short
 * description and an image.
 *
 * <p>The geometries have no crs, they are drawn in their own coordinates.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class GeometryDisplayRenderer implements IDisplayRenderer {

    private static final int MAX_WKT_LENGTH = 300;

    @Override
    public boolean canRender( Object value ) {
        if (value instanceof Geometry) {
            return true;
        }
        if (value instanceof Collection && !((Collection< ? >) value).isEmpty()) {
            for( Object item : (Collection< ? >) value ) {
                if (!(item instanceof Geometry)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public List<DisplayPart> render( Object value, DisplayOptions options ) throws Exception {
        List<Geometry> geometries = new ArrayList<>();
        if (value instanceof Geometry) {
            geometries.add((Geometry) value);
        } else {
            for( Object item : (Collection< ? >) value ) {
                geometries.add((Geometry) item);
            }
        }

        String description;
        if (geometries.size() == 1) {
            Geometry geometry = geometries.get(0);
            String wkt = geometry.toText();
            if (wkt.length() <= MAX_WKT_LENGTH) {
                description = wkt;
            } else {
                description = geometry.getGeometryType() + " with " + geometry.getNumPoints() + " coordinates";
            }
        } else {
            description = geometries.size() + " geometries";
        }

        // split by type, so that each group gets the right default style
        List<Geometry> points = new ArrayList<>();
        List<Geometry> lines = new ArrayList<>();
        List<Geometry> polygons = new ArrayList<>();
        for( Geometry geometry : geometries ) {
            explode(geometry, points, lines, polygons);
        }

        if (lines.isEmpty() && polygons.isEmpty() && points.size() == 1 && points.get(0).getNumPoints() == 1) {
            // the image of a single point is just a dot, the text says it all
            return List.of(new DisplayText(description));
        }

        ImageGenerator imgGen = new ImageGenerator(null, null);
        try {
            // polygons below lines below points
            addGroup(imgGen, polygons, Polygon.class);
            addGroup(imgGen, lines, LineString.class);
            addGroup(imgGen, points, Point.class);
            if (polygons.isEmpty() && lines.isEmpty() && points.isEmpty()) {
                return List.of(new DisplayText(description));
            }
            return List.of(new DisplayText(description),
                    new DisplayImage(imgGen.drawFullExtent(options.imageWidth(), options.imageHeight(), 0)));
        } finally {
            imgGen.dispose();
        }
    }

    private static void explode( Geometry geometry, List<Geometry> points, List<Geometry> lines, List<Geometry> polygons ) {
        if (geometry == null || geometry.isEmpty()) {
            return;
        }
        if (geometry instanceof Puntal) {
            points.add(geometry);
        } else if (geometry instanceof Lineal) {
            lines.add(geometry);
        } else if (geometry instanceof Polygonal) {
            polygons.add(geometry);
        } else if (geometry instanceof GeometryCollection) {
            for( int i = 0; i < geometry.getNumGeometries(); i++ ) {
                explode(geometry.getGeometryN(i), points, lines, polygons);
            }
        }
    }

    private static void addGroup( ImageGenerator imgGen, List<Geometry> geometries, Class< ? extends Geometry> binding ) {
        if (geometries.isEmpty()) {
            return;
        }
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("geometries");
        b.add("the_geom", binding);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
        for( Geometry geometry : geometries ) {
            builder.add(geometry);
            featureCollection.add(builder.buildFeature(null));
        }
        imgGen.addFeatures(featureCollection, null);
    }
}
