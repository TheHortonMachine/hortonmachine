/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.server.jetty.providers.data;

import java.io.File;
import java.io.StringWriter;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.features.FilterUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.geometry.EGeometryType;
import org.jgrasstools.gears.utils.style.SimpleStyle;
import org.jgrasstools.gears.utils.style.SimpleStyleUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Data provider for shapefiles.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ShapeFileDataProvider implements NwwDataProvider {

    private SimpleFeatureCollection readVector;
    private GeometryDescriptor geometryDescriptor;
    private String name;
    private List<SimpleFeature> featuresList;
    private String shapefile;
    private String labelField;
    private Envelope bounds;
    private CoordinateReferenceSystem crs;

    public ShapeFileDataProvider( String shapefile, String cqlFilterString, String labelField ) throws Exception {
        this.shapefile = shapefile;
        this.labelField = labelField;
        File file = new File(shapefile);
        name = FileUtilities.getNameWithoutExtention(file);

        readVector = OmsVectorReader.readVector(shapefile);
        if (cqlFilterString != null) {
            Filter filter = FilterUtilities.getCQLFilter(cqlFilterString);
            readVector = new SubFeatureCollection(readVector, filter);
        }

        crs = readVector.getBounds().getCoordinateReferenceSystem();

        geometryDescriptor = readVector.getSchema().getGeometryDescriptor();

        featuresList = FeatureUtilities.featureCollectionToList(readVector);

        bounds = readVector.getBounds();
    }

    public String asGeoJson() throws Exception {
        CoordinateReferenceSystem geojsonCRS = DefaultGeographicCRS.WGS84;
        SimpleFeatureCollection fc = readVector;
        if (!CRS.equalsIgnoreMetadata(geojsonCRS, crs)) {
            fc = new ReprojectingFeatureCollection(readVector, geojsonCRS);
        }
        FeatureJSON fjson = new FeatureJSON(new GeometryJSON(7));
        StringWriter writer = new StringWriter();
        fjson.writeFeatureCollection(fc, writer);
        String geojson = writer.toString();
        return geojson;
    }

    @Override
    public boolean isPoints() {
        return EGeometryType.isPoint(geometryDescriptor);
    }

    @Override
    public boolean isLines() {
        return EGeometryType.isLine(geometryDescriptor);
    }

    @Override
    public boolean isPolygon() {
        return EGeometryType.isPolygon(geometryDescriptor);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int size() {
        return featuresList.size();
    }

    @Override
    public Geometry getGeometryAt( int index ) {
        return (Geometry) featuresList.get(index).getDefaultGeometry();
    }

    @Override
    public SimpleStyle getStyle() throws Exception {
        SimpleStyle style = new SimpleStyle();
        if (isPoints()) {
            style = SimpleStyleUtilities.getStyle(shapefile, EGeometryType.POINT);
        } else if (isLines()) {
            style = SimpleStyleUtilities.getStyle(shapefile, EGeometryType.LINE);
        } else if (isPolygon()) {
            style = SimpleStyleUtilities.getStyle(shapefile, EGeometryType.POLYGON);
        }
        return style;
    }

    @Override
    public String getLabelAt( int index ) {
        if (labelField != null) {
            return featuresList.get(index).getAttribute(labelField).toString();
        }
        return "";
    }

    @Override
    public Envelope getBounds() {
        return bounds;
    }

    @Override
    public SimpleFeatureCollection subCollection( String cqlFilterString ) throws Exception {
        if (cqlFilterString == null || cqlFilterString.trim().length() == 0) {
            return readVector;
        } else {
            Filter filter = FilterUtilities.getCQLFilter(cqlFilterString);
            SubFeatureCollection subCollection = new SubFeatureCollection(readVector, filter);
            return subCollection;
        }
    }

}
