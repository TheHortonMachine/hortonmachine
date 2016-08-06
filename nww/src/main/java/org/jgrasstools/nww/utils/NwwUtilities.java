package org.jgrasstools.nww.utils;

import java.awt.Color;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Style;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.utils.SldUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryType;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.style.FeatureTypeStyleWrapper;
import org.jgrasstools.gears.utils.style.LineSymbolizerWrapper;
import org.jgrasstools.gears.utils.style.PointSymbolizerWrapper;
import org.jgrasstools.gears.utils.style.PolygonSymbolizerWrapper;
import org.jgrasstools.gears.utils.style.RuleWrapper;
import org.jgrasstools.gears.utils.style.StyleWrapper;
import org.jgrasstools.nww.gui.style.SimpleStyle;
import org.jgrasstools.nww.layers.defaults.NwwVectorLayer.GEOMTYPE;
import org.jgrasstools.nww.layers.objects.BasicMarkerWithInfo;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwindx.examples.util.ToolTipController;

public class NwwUtilities {

    public static final String[] SUPPORTED_EXTENSIONS = {"shp", "mbtiles", "map", "rl2", "sqlite"};

    public static final CoordinateReferenceSystem GPS_CRS = DefaultGeographicCRS.WGS84;
    public static final int GPS_CRS_SRID = 4326;

    public static double DEFAULT_ELEV = 10000.0;

    public static List<String> LAYERS_TO_KEEP_FROM_ORIGNALNWW = Arrays.asList("Scale bar", "Compass", "Bing Imagery");

    public static LatLon getEnvelopeCenter( Envelope bounds ) {
        double x = bounds.getMinX() + (bounds.getMaxX() - bounds.getMinX()) / 2.0;
        double y = bounds.getMinY() + (bounds.getMaxY() - bounds.getMinY()) / 2.0;
        LatLon latLon = new LatLon(Angle.fromDegrees(y), Angle.fromDegrees(x));
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

    public static SimpleStyle getStyle( String path, GeometryType geomType ) throws Exception {
        SimpleStyle simpleStyle = new SimpleStyle();
        if (path == null) {
            return simpleStyle;
        }

        Style style = SldUtilities.getStyleFromFile(new File(path));
        if (style == null)
            return null;

        StyleWrapper styleWrapper = new StyleWrapper(style);
        List<FeatureTypeStyleWrapper> featureTypeStylesWrapperList = styleWrapper.getFeatureTypeStylesWrapperList();
        for( FeatureTypeStyleWrapper featureTypeStyleWrapper : featureTypeStylesWrapperList ) {
            List<RuleWrapper> rulesWrapperList = featureTypeStyleWrapper.getRulesWrapperList();
            for( RuleWrapper ruleWrapper : rulesWrapperList ) {

                switch( geomType ) {
                case POLYGON:
                case MULTIPOLYGON:
                    PolygonSymbolizerWrapper polygonSymbolizerWrapper = ruleWrapper.getGeometrySymbolizersWrapper()
                            .adapt(PolygonSymbolizerWrapper.class);

                    simpleStyle.fillColor = Color.decode(polygonSymbolizerWrapper.getFillColor());
                    simpleStyle.fillOpacity = Double.parseDouble(polygonSymbolizerWrapper.getFillOpacity());
                    simpleStyle.strokeColor = Color.decode(polygonSymbolizerWrapper.getStrokeColor());
                    simpleStyle.strokeWidth = Double.parseDouble(polygonSymbolizerWrapper.getStrokeWidth());
                    break;
                case LINE:
                case MULTILINE:
                    LineSymbolizerWrapper lineSymbolizerWrapper = ruleWrapper.getGeometrySymbolizersWrapper()
                            .adapt(LineSymbolizerWrapper.class);

                    simpleStyle.strokeColor = Color.decode(lineSymbolizerWrapper.getStrokeColor());
                    simpleStyle.strokeWidth = Double.parseDouble(lineSymbolizerWrapper.getStrokeWidth());

                    break;
                case POINT:
                case MULTIPOINT:
                    PointSymbolizerWrapper pointSymbolizerWrapper = ruleWrapper.getGeometrySymbolizersWrapper()
                            .adapt(PointSymbolizerWrapper.class);

                    simpleStyle.fillColor = Color.decode(pointSymbolizerWrapper.getFillColor());
                    simpleStyle.fillOpacity = Double.parseDouble(pointSymbolizerWrapper.getFillOpacity());
                    simpleStyle.strokeColor = Color.decode(pointSymbolizerWrapper.getStrokeColor());
                    simpleStyle.strokeWidth = Double.parseDouble(pointSymbolizerWrapper.getStrokeWidth());
                    simpleStyle.shapeSize = Double.parseDouble(pointSymbolizerWrapper.getSize());
                    String markName = pointSymbolizerWrapper.getMarkName();
                    if (markName != null && markName.trim().length() != 0) {
                        switch( markName ) {
                        case "square":
                            simpleStyle.shapeType = BasicMarkerShape.CUBE;
                            break;
                        case "triangle":
                            simpleStyle.shapeType = BasicMarkerShape.CONE;
                            break;
                        case "circle":
                        default:
                            simpleStyle.shapeType = BasicMarkerShape.SPHERE;
                            break;
                        }
                    }
                    break;

                default:
                    break;
                }

                // only one rule supported for now
                return simpleStyle;
            }
        }

        return null;
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
        if (GeometryUtilities.isPolygon(geometryDescriptor)) {
            return GEOMTYPE.POLYGON;
        } else if (GeometryUtilities.isLine(geometryDescriptor)) {
            return GEOMTYPE.LINE;
        } else if (GeometryUtilities.isPoint(geometryDescriptor)) {
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
        ReferencedEnvelope llEnv = env.transform(GPS_CRS, true);
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

}
