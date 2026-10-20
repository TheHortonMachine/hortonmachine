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
import java.util.List;

import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.style.Style;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.hortonmachine.gears.utils.images.ImageGenerator;

/**
 * Renders {@link SimpleFeatureCollection}s and {@link SimpleFeatureSource}s as an info line,
 * a quicklook and the attribute table, single {@link SimpleFeature}s as their attributes.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class FeatureDisplayRenderer implements IDisplayRenderer {

    @Override
    public boolean canRender( Object value ) {
        return value instanceof SimpleFeatureCollection || value instanceof SimpleFeatureSource
                || value instanceof SimpleFeature;
    }

    @Override
    public List<DisplayPart> render( Object value, DisplayOptions options ) throws Exception {
        return render(value, null, options);
    }

    /**
     * Render features with a given style.
     *
     * @param value the {@link SimpleFeatureCollection}, {@link SimpleFeatureSource} or {@link SimpleFeature}.
     * @param style the style to use, if <code>null</code> the default one for the geometry type is used.
     * @param options the sizes and limits to use.
     * @return the parts.
     * @throws Exception
     */
    public List<DisplayPart> render( Object value, Style style, DisplayOptions options ) throws Exception {
        if (value instanceof SimpleFeature) {
            return List.of(featureTable((SimpleFeature) value, true));
        }

        SimpleFeatureCollection featureCollection;
        if (value instanceof SimpleFeatureSource) {
            featureCollection = ((SimpleFeatureSource) value).getFeatures();
        } else {
            featureCollection = (SimpleFeatureCollection) value;
        }
        SimpleFeatureType schema = featureCollection.getSchema();
        int size = featureCollection.size();

        StringBuilder sb = new StringBuilder();
        sb.append(size).append(size == 1 ? " feature" : " features");
        GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
        if (geometryDescriptor != null) {
            sb.append(", ").append(geometryDescriptor.getType().getBinding().getSimpleName());
        }
        sb.append(", ").append(RasterDisplayRenderer.crsLabel(schema.getCoordinateReferenceSystem()));

        List<DisplayPart> parts = new ArrayList<>();
        parts.add(new DisplayText(sb.toString()));
        if (geometryDescriptor != null && size > 0) {
            ImageGenerator imgGen = new ImageGenerator(null, null);
            try {
                imgGen.addFeatures(featureCollection, style);
                parts.add(new DisplayImage(imgGen.drawFullExtent(options.imageWidth(), options.imageHeight(), 0)));
            } finally {
                imgGen.dispose();
            }
        }
        if (size == 1) {
            // one row with many columns reads badly, show the attributes one per row
            try (SimpleFeatureIterator iterator = featureCollection.features()) {
                if (iterator.hasNext()) {
                    parts.add(featureTable(iterator.next(), false));
                }
            }
        } else if (size > 1) {
            parts.add(attributesTable(featureCollection, size, options.maxTableRows()));
        }
        return parts;
    }

    private DisplayTable featureTable( SimpleFeature feature, boolean withGeometry ) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("fid", feature.getID()));
        for( AttributeDescriptor descriptor : feature.getFeatureType().getAttributeDescriptors() ) {
            if (withGeometry || !(descriptor instanceof GeometryDescriptor)) {
                String name = descriptor.getLocalName();
                Object attribute = feature.getAttribute(name);
                rows.add(List.of(name, attribute == null ? "" : String.valueOf(attribute)));
            }
        }
        return new DisplayTable(List.of("Name", "Value"), rows, rows.size());
    }

    private DisplayTable attributesTable( SimpleFeatureCollection featureCollection, int size, int maxRows ) {
        SimpleFeatureType schema = featureCollection.getSchema();
        List<String> names = new ArrayList<>();
        for( AttributeDescriptor descriptor : schema.getAttributeDescriptors() ) {
            if (!(descriptor instanceof GeometryDescriptor)) {
                names.add(descriptor.getLocalName());
            }
        }
        List<String> header = new ArrayList<>();
        header.add("fid");
        header.addAll(names);

        List<List<String>> rows = new ArrayList<>();
        try (SimpleFeatureIterator iterator = featureCollection.features()) {
            while( iterator.hasNext() && rows.size() < maxRows ) {
                SimpleFeature feature = iterator.next();
                List<String> row = new ArrayList<>();
                row.add(feature.getID());
                for( String name : names ) {
                    Object attribute = feature.getAttribute(name);
                    row.add(attribute == null ? "" : String.valueOf(attribute));
                }
                rows.add(row);
            }
        }
        return new DisplayTable(header, rows, size);
    }
}
