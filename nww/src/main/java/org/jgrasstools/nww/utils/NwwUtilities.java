package org.jgrasstools.nww.utils;

import java.awt.Color;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import com.vividsolutions.jts.geom.Envelope;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.nww.layers.BasicMarkerWithInfo;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwindx.examples.util.ToolTipController;

public class NwwUtilities {

    public static final String[] SUPPORTED_EXTENSIONS = { "shp", "mbtiles", "map" };

    private static final CoordinateReferenceSystem GPS_CRS = DefaultGeographicCRS.WGS84;

    public static double DEFAULT_ELEV = 10000.0;

    public static List<String> LAYERS_TO_KEEP_FROM_ORIGNALNWW = Arrays.asList("Scale bar", "Compass", "Bing Imagery");

    public static LatLon getEnvelopeCenter(Envelope bounds) {
        double x = bounds.getMinX() + (bounds.getMaxX() - bounds.getMinX()) / 2.0;
        double y = bounds.getMinY() + (bounds.getMaxY() - bounds.getMinY()) / 2.0;
        LatLon latLon = new LatLon(Angle.fromDegrees(y), Angle.fromDegrees(x));
        return latLon;
    }

    public static SimpleFeatureCollection readAndReproject(String path) throws Exception {
        SimpleFeatureCollection fc = OmsVectorReader.readVector(path);
        // BOUNDS
        ReferencedEnvelope bounds = fc.getBounds();
        CoordinateReferenceSystem crs = bounds.getCoordinateReferenceSystem();
        if (!CRS.equalsIgnoreMetadata(crs, GPS_CRS)) {
            try {
                fc = new ReprojectingFeatureCollection(fc, GPS_CRS);
            } catch (Exception e) {
                throw new IllegalArgumentException("The pipes data need to be of WGS84 lat/lon projection.", e);
            }
        }
        return fc;
    }

    public static LinkedHashMap<String, String> feature2AlphanumericToHashmap(SimpleFeature feature) {
        LinkedHashMap<String, String> attributes = new LinkedHashMap<>();
        List<AttributeDescriptor> attributeDescriptors = feature.getFeatureType().getAttributeDescriptors();
        int index = 0;
        for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
            if (!(attributeDescriptor instanceof GeometryDescriptor)) {
                String fieldName = attributeDescriptor.getLocalName();
                Object attribute = feature.getAttribute(index);
                if (attribute == null) {
                    attribute = "";
                }
                String value = attribute.toString();
                attributes.put(fieldName, value);
            }
            index++;
        }
        return attributes;
    }

    public static void addTooltipController(WorldWindow wwd) {
        new ToolTipController(wwd) {

            @Override
            public void selected(SelectEvent event) {
                // Intercept the selected position and assign its display name
                // the position's data value.
                if (event.getTopObject() instanceof BasicMarkerWithInfo) {
                    BasicMarkerWithInfo marker = (BasicMarkerWithInfo) event.getTopObject();
                    String info = marker.getInfo();
                    marker.setValue(AVKey.DISPLAY_NAME, info);
                }
                super.selected(event);
            }
        };
    }

    public static LatLon toLatLon(double lat, double lon) {
        LatLon latLon = new LatLon(Angle.fromDegrees(lat), Angle.fromDegrees(lon));
        return latLon;
    }

    public static Position toPosition(double lat, double lon, double elev) {
        LatLon latLon = toLatLon(lat, lon);
        return new Position(latLon, elev);
    }

    public static Position toPosition(double lat, double lon) {
        return toPosition(lat, lon, DEFAULT_ELEV);
    }

    public static Color darkenColor(Color color) {
        float factor = 0.8f;
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        Color darkerColor = new Color(//
                Math.max((int) (r * factor), 0), Math.max((int) (g * factor), 0), Math.max((int) (b * factor), 0));
        return darkerColor;
    }

}
