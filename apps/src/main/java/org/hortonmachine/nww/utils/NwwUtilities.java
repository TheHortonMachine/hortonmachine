package org.hortonmachine.nww.utils;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.nww.layers.defaults.NwwVectorLayer.GEOMTYPE;
import org.hortonmachine.nww.layers.objects.BasicMarkerWithInfo;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.Polygon;
import gov.nasa.worldwindx.hm.ToolTipController;

public class NwwUtilities {

    public static final String[] SUPPORTED_EXTENSIONS = {"shp", "mbtiles", "map", "rl2", "sqlite", "asc", "tiff", "gpkg"};

    public static final CoordinateReferenceSystem GPS_CRS = DefaultGeographicCRS.WGS84;
    public static final int GPS_CRS_SRID = 4326;

    public static double DEFAULT_ELEV = 10000.0;

    public static List<String> LAYERS_TO_KEEP_FROM_ORIGNALNWW = Arrays.asList("Scale bar", "Compass", "Bing Imagery");

    public static LatLon getEnvelopeCenter( Envelope bounds ) {
        Coordinate centre = bounds.centre();
        LatLon latLon = new LatLon(Angle.fromDegrees(centre.y), Angle.fromDegrees(centre.x));
        return latLon;
    }

    public static SimpleFeatureCollection readAndReproject( String path ) throws Exception {
        SimpleFeatureCollection fc = OmsVectorReader.readVector(path);
        return reprojectToWGS84(fc);
    }

    public static ReferencedEnvelope readAndReprojectBounds( String path ) throws Exception {
        ReferencedEnvelope env = OmsVectorReader.readEnvelope(path);
        return env.transform(GPS_CRS, true);
    }

    private static SimpleFeatureCollection reprojectToWGS84( SimpleFeatureCollection fc ) {
        // BOUNDS
        ReferencedEnvelope bounds = fc.getBounds();
        CoordinateReferenceSystem crs = bounds.getCoordinateReferenceSystem();
        if (!CRS.equalsIgnoreMetadata(crs, GPS_CRS)) {
            try {
                fc = new ReprojectingFeatureCollection(fc, GPS_CRS);
            } catch (Exception e) {
                throw new IllegalArgumentException("The data need to be of WGS84 lat/lon projection.", e);
            }
        }
        return fc;
    }

    public static SimpleFeatureCollection readAndReproject( SimpleFeatureSource featureSource ) throws Exception {
        SimpleFeatureCollection fc = featureSource.getFeatures();
        return reprojectToWGS84(fc);
    }

    /**
     * Get the feature source from a file.
     * 
     * @param path
     *            the path to the shapefile.
     * @return the feature source.
     * @throws Exception
     */
    public static SimpleFeatureSource readFeatureSource( String path ) throws Exception {
        File shapeFile = new File(path);
        FileDataStore store = FileDataStoreFinder.getDataStore(shapeFile);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        return featureSource;
    }

    /**
     * Get the geometry type from a featurecollection.
     * 
     * @param featureCollection
     *            the collection.
     * @return the {@link GEOMTYPE}.
     */
    public static GEOMTYPE getGeometryType( SimpleFeatureCollection featureCollection ) {
        GeometryDescriptor geometryDescriptor = featureCollection.getSchema().getGeometryDescriptor();
        if (EGeometryType.isPolygon(geometryDescriptor)) {
            return GEOMTYPE.POLYGON;
        } else if (EGeometryType.isLine(geometryDescriptor)) {
            return GEOMTYPE.LINE;
        } else if (EGeometryType.isPoint(geometryDescriptor)) {
            return GEOMTYPE.POINT;
        } else {
            return GEOMTYPE.UNKNOWN;
        }
    }

    public static LinkedHashMap<String, String> feature2AlphanumericToHashmap( SimpleFeature feature ) {
        LinkedHashMap<String, String> attributes = new LinkedHashMap<>();
        List<AttributeDescriptor> attributeDescriptors = feature.getFeatureType().getAttributeDescriptors();
        int index = 0;
        for( AttributeDescriptor attributeDescriptor : attributeDescriptors ) {
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

    public static void addTooltipController( WorldWindow wwd ) {
        new ToolTipController(wwd){

            @Override
            public void selected( SelectEvent event ) {
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

    public static LatLon toLatLon( double lat, double lon ) {
        LatLon latLon = new LatLon(Angle.fromDegrees(lat), Angle.fromDegrees(lon));
        return latLon;
    }

    public static Position toPosition( double lat, double lon, double elev ) {
        LatLon latLon = toLatLon(lat, lon);
        return new Position(latLon, elev);
    }

    public static Position toPosition( double lat, double lon ) {
        return toPosition(lat, lon, DEFAULT_ELEV);
    }

    public static Sector envelope2Sector( ReferencedEnvelope env ) throws Exception {
        CoordinateReferenceSystem sourceCRS = env.getCoordinateReferenceSystem();
        CoordinateReferenceSystem targetCRS = GPS_CRS;

        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
        Envelope envLL = JTS.transform(env, transform);
        ReferencedEnvelope llEnv = new ReferencedEnvelope(envLL, targetCRS);
        Sector sector = Sector.fromDegrees(llEnv.getMinY(), llEnv.getMaxY(), llEnv.getMinX(), llEnv.getMaxX());
        return sector;
    }

    public static ReferencedEnvelope sector2Envelope( Sector sector ) throws Exception {
        ReferencedEnvelope env = new ReferencedEnvelope(sector.getMinLongitude().degrees, sector.getMaxLongitude().degrees,
                sector.getMinLatitude().degrees, sector.getMaxLatitude().degrees, GPS_CRS);
        return env;
    }

    public static Color darkenColor( Color color ) {
        float factor = 0.8f;
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        Color darkerColor = new Color(//
                Math.max((int) (r * factor), 0), Math.max((int) (g * factor), 0), Math.max((int) (b * factor), 0));
        return darkerColor;
    }

    public static int[] getTileNumber( final double lat, final double lon, final int zoom ) {
        int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int ytile = (int) Math.floor(
                (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom));
        if (xtile < 0)
            xtile = 0;
        if (xtile >= (1 << zoom))
            xtile = ((1 << zoom) - 1);
        if (ytile < 0)
            ytile = 0;
        if (ytile >= (1 << zoom))
            ytile = ((1 << zoom) - 1);
        return new int[]{xtile, ytile};
    }

    /**
     * Get the lat/long world geometry from two screen corner coordinates.
     * 
     * @param wwd
     *            the {@link WorldWindow} instance.
     * @param x1
     *            the first point screen x.
     * @param y1
     *            the first point screen y.
     * @param x2
     *            the second point screen x.
     * @param y2
     *            the second point screen y.
     * @return the world geomnetry.
     */
    public static Geometry getScreenPointsPolygon( WorldWindow wwd, int x1, int y1, int x2, int y2 ) {
        View view = wwd.getView();
        Position p1 = view.computePositionFromScreenPoint(x1, y1);
        Position p2 = view.computePositionFromScreenPoint(x1, y2);
        Position p3 = view.computePositionFromScreenPoint(x2, y2);
        Position p4 = view.computePositionFromScreenPoint(x2, y1);

        Coordinate[] coords = { //
                new Coordinate(p1.longitude.degrees, p1.latitude.degrees), //
                new Coordinate(p2.longitude.degrees, p2.latitude.degrees), //
                new Coordinate(p3.longitude.degrees, p3.latitude.degrees), //
                new Coordinate(p4.longitude.degrees, p4.latitude.degrees)//
        };
        Geometry convexHull = GeometryUtilities.gf().createMultiPoint(coords).convexHull();
        return convexHull;
    }

    public static Point getScreenPoint( WorldWindow wwd, int x1, int y1 ) {
        View view = wwd.getView();
        Position p = view.computePositionFromScreenPoint(x1, y1);
        Coordinate c = new Coordinate(p.longitude.degrees, p.latitude.degrees);
        return GeometryUtilities.gf().createPoint(c);
    }

    /**
     * Calculates distance between 2 lat/long points on WGS84.
     * 
     * <p>Taken from Android sources.</p>
     * 
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @return
     */
    public static double computeDistance( double lat1, double lon1, double lat2, double lon2 ) {
        // Based on http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
        // using the "Inverse Formula" (section 4)
        int MAXITERS = 20;
        // Convert lat/long to radians
        lat1 *= Math.PI / 180.0;
        lat2 *= Math.PI / 180.0;
        lon1 *= Math.PI / 180.0;
        lon2 *= Math.PI / 180.0;
        double a = 6378137.0; // WGS84 major axis
        double b = 6356752.3142; // WGS84 semi-major axis
        double f = (a - b) / a;
        double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);
        double L = lon2 - lon1;
        double A = 0.0;
        double U1 = Math.atan((1.0 - f) * Math.tan(lat1));
        double U2 = Math.atan((1.0 - f) * Math.tan(lat2));
        double cosU1 = Math.cos(U1);
        double cosU2 = Math.cos(U2);
        double sinU1 = Math.sin(U1);
        double sinU2 = Math.sin(U2);
        double cosU1cosU2 = cosU1 * cosU2;
        double sinU1sinU2 = sinU1 * sinU2;
        double sigma = 0.0;
        double deltaSigma = 0.0;
        double cosSqAlpha = 0.0;
        double cos2SM = 0.0;
        double cosSigma = 0.0;
        double sinSigma = 0.0;
        double cosLambda = 0.0;
        double sinLambda = 0.0;
        double lambda = L; // initial guess
        for( int iter = 0; iter < MAXITERS; iter++ ) {
            double lambdaOrig = lambda;
            cosLambda = Math.cos(lambda);
            sinLambda = Math.sin(lambda);
            double t1 = cosU2 * sinLambda;
            double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
            double sinSqSigma = t1 * t1 + t2 * t2; // (14)
            sinSigma = Math.sqrt(sinSqSigma);
            cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda; // (15)
            sigma = Math.atan2(sinSigma, cosSigma); // (16)
            double sinAlpha = (sinSigma == 0) ? 0.0 : cosU1cosU2 * sinLambda / sinSigma; // (17)
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
            cos2SM = (cosSqAlpha == 0) ? 0.0 : cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha; // (18)
            double uSquared = cosSqAlpha * aSqMinusBSqOverBSq; // defn
            A = 1 + (uSquared / 16384.0) * // (3)
                    (4096.0 + uSquared * (-768 + uSquared * (320.0 - 175.0 * uSquared)));
            double B = (uSquared / 1024.0) * // (4)
                    (256.0 + uSquared * (-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
            double C = (f / 16.0) * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha)); // (10)
            double cos2SMSq = cos2SM * cos2SM;
            deltaSigma = B * sinSigma * // (6)
                    (cos2SM + (B / 4.0) * (cosSigma * (-1.0 + 2.0 * cos2SMSq)
                            - (B / 6.0) * cos2SM * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SMSq)));
            lambda = L + (1.0 - C) * f * sinAlpha
                    * (sigma + C * sinSigma * (cos2SM + C * cosSigma * (-1.0 + 2.0 * cos2SM * cos2SM))); // (11)
            double delta = (lambda - lambdaOrig) / lambda;
            if (Math.abs(delta) < 1.0e-12) {
                break;
            }
        }
        float distance = (float) (b * A * (sigma - deltaSigma));
        return distance;
    }

    /**
     * Insert a layer before the compass layer.
     * 
     * @param wwd the {@link WorldWindow}.
     * @param layer the layer to insert.
     */
    public static void insertBeforeCompass( WorldWindow wwd, Layer layer ) {
        // Insert the layer into the layer list just before the compass.
        int compassPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for( Layer l : layers ) {
            if (l instanceof CompassLayer)
                compassPosition = layers.indexOf(l);
        }
        layers.add(compassPosition, layer);
    }

    public static void addGeometries( RenderableLayer layer, Geometry geometry ) {
        Material mFillMaterial = Material.BLUE;
        Material mStrokeMaterial = Material.RED;
        double mFillOpacity = 0.8;
        double mStrokeWidth = 2;
        double mMarkerSize = 15d;
        BasicShapeAttributes polyghonAttributes = new BasicShapeAttributes();
        polyghonAttributes.setInteriorMaterial(mFillMaterial);
        polyghonAttributes.setInteriorOpacity(mFillOpacity);
        polyghonAttributes.setOutlineMaterial(mStrokeMaterial);
        polyghonAttributes.setOutlineWidth(mStrokeWidth);

        BasicShapeAttributes lineAttributes = new BasicShapeAttributes();
        lineAttributes.setOutlineMaterial(mStrokeMaterial);
        lineAttributes.setOutlineWidth(mStrokeWidth);

        PointPlacemarkAttributes pointAttributes = new PointPlacemarkAttributes();
        pointAttributes.setLabelMaterial(mFillMaterial);
        pointAttributes.setLineMaterial(mFillMaterial);
        pointAttributes.setUsePointAsDefaultImage(true);
        pointAttributes.setScale(mMarkerSize);

        int mElevationMode = WorldWind.CLAMP_TO_GROUND;
        for( int i = 0; i < geometry.getNumGeometries(); i++ ) {
            Geometry geometryN = geometry.getGeometryN(i);
            if (geometryN instanceof org.locationtech.jts.geom.Polygon) {
                Coordinate[] coordinates = geometryN.getCoordinates();
                int numVertices = coordinates.length;
                if (numVertices < 4)
                    continue;
                org.locationtech.jts.geom.Polygon poly = (org.locationtech.jts.geom.Polygon) geometryN;

                Polygon polygon = new Polygon();

                Coordinate[] extCoords = poly.getExteriorRing().getCoordinates();
                int extSize = extCoords.length;
                List<Position> verticesList = new ArrayList<>(extSize);
                for( int n = 0; n < extSize; n++ ) {
                    Coordinate c = extCoords[n];
                    verticesList.add(Position.fromDegrees(c.y, c.x));
                }
                verticesList.add(verticesList.get(0));
                polygon.setOuterBoundary(verticesList);

                int numInteriorRings = poly.getNumInteriorRing();
                for( int k = 0; k < numInteriorRings; k++ ) {
                    LineString interiorRing = poly.getInteriorRingN(k);
                    Coordinate[] intCoords = interiorRing.getCoordinates();
                    int internalNumVertices = intCoords.length;
                    List<Position> internalVerticesList = new ArrayList<>(internalNumVertices);
                    for( int j = 0; j < internalNumVertices; j++ ) {
                        Coordinate c = intCoords[j];
                        internalVerticesList.add(Position.fromDegrees(c.y, c.x));
                    }
                    polygon.addInnerBoundary(internalVerticesList);
                }

                polygon.setAltitudeMode(mElevationMode);
                polygon.setAttributes(polyghonAttributes);

                layer.addRenderable(polygon);
            } else if (geometryN instanceof LineString) {
                Coordinate[] coordinates = geometryN.getCoordinates();
                if (coordinates.length < 2)
                    return;

                if (geometryN instanceof LineString) {
                    LineString line = (LineString) geometryN;
                    Coordinate[] lineCoords = line.getCoordinates();
                    int numVertices = lineCoords.length;
                    List<Position> verticesList = new ArrayList<>(numVertices);
                    for( int j = 0; j < numVertices; j++ ) {
                        Coordinate c = lineCoords[j];
                        verticesList.add(Position.fromDegrees(c.y, c.x));
                    }
                    Path path = new Path();
                    path.setAltitudeMode(mElevationMode);
                    path.setAttributes(lineAttributes);

                    layer.addRenderable(path);
                }
            } else if (geometryN instanceof Point) {
                Point point = (Point) geometryN;
                PointPlacemark marker = new PointPlacemark(Position.fromDegrees(point.getY(), point.getX(), 0));
                marker.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
                marker.setAttributes(pointAttributes);
                layer.addRenderable(marker);
            }
        }

    }

}
