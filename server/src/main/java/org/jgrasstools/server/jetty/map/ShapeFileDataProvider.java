package org.jgrasstools.server.jetty.map;

import java.io.File;
import java.io.StringWriter;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.features.FilterUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryType;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.style.SimpleStyle;
import org.jgrasstools.gears.utils.style.SimpleStyleUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

public class ShapeFileDataProvider implements NwwDataProvider {

    private SimpleFeatureCollection readVector;
    private GeometryDescriptor geometryDescriptor;
    private String name;
    private List<SimpleFeature> featuresList;
    private String shapefile;
    private String labelField;

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

        geometryDescriptor = readVector.getSchema().getGeometryDescriptor();

        featuresList = FeatureUtilities.featureCollectionToList(readVector);
    }

    public String asGeoJson() throws Exception {
        CoordinateReferenceSystem geojsonCRS = DefaultGeographicCRS.WGS84;
        ReprojectingFeatureCollection rfc = new ReprojectingFeatureCollection(readVector, geojsonCRS);
        FeatureJSON fjson = new FeatureJSON();
        StringWriter writer = new StringWriter();
        fjson.writeFeatureCollection(rfc, writer);
        String geojson = writer.toString();
        return geojson;
    }

    @Override
    public boolean isPoints() {
        return GeometryUtilities.isPoint(geometryDescriptor);
    }

    @Override
    public boolean isLines() {
        return GeometryUtilities.isLine(geometryDescriptor);
    }

    @Override
    public boolean isPolygon() {
        return GeometryUtilities.isPolygon(geometryDescriptor);
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
        SimpleStyle style = null;
        if (isPoints()) {
            style = SimpleStyleUtilities.getStyle(shapefile, GeometryType.POINT);
        } else if (isLines()) {
            style = SimpleStyleUtilities.getStyle(shapefile, GeometryType.LINE);
        } else if (isPolygon()) {
            style = SimpleStyleUtilities.getStyle(shapefile, GeometryType.POLYGON);
        }
        return style;
    }

    @Override
    public String getLabelAt( int index ) {
        if (labelField!=null) {
            featuresList.get(index).getAttribute(index).toString();
        }
        return "";
    }

}
