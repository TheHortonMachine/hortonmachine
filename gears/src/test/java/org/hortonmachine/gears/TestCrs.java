package org.hortonmachine.gears;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

import org.eclipse.imagen.Interpolation;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.utils.crs.CrsUtilities;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.gears.utils.crs.fixes.HMCylindricalEqualArea;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class TestCrs {

    private static final double TOL_DEGREES = 1e-7;
    private static final double TOL_METERS = 1e-2;

    @Test
    public void test6933To4326AtOrigin() throws Exception {
    	HMCrsRegistry.INSTANCE.init();
		CoordinateReferenceSystem sourceCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:6933", true);
        CoordinateReferenceSystem targetCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true);

        assertNotNull(sourceCrs);
        assertNotNull(targetCrs);

        MathTransform transform = CRS.findMathTransform(sourceCrs, targetCrs, true);
        assertNotNull(transform);

        GeometryFactory gf = new GeometryFactory();

        // In EPSG:6933, (0,0) should map to lon=0, lat=0
        Point sourcePoint = gf.createPoint(new Coordinate(0.0, 0.0));
        Point targetPoint = (Point) JTS.transform(sourcePoint, transform);

        assertEquals(0.0, targetPoint.getX(), TOL_DEGREES); // longitude
        assertEquals(0.0, targetPoint.getY(), TOL_DEGREES); // latitude
    }

    @Test
    public void test6933To4326RoundTrip() throws Exception {
    	HMCrsRegistry.INSTANCE.init();
		CoordinateReferenceSystem sourceCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:6933", true);
        CoordinateReferenceSystem targetCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true);

        MathTransform to4326 = CRS.findMathTransform(sourceCrs, targetCrs, true);
        MathTransform to4326fixed = HMCylindricalEqualArea.createEpsg6933Inverse();
        MathTransform to6933 = CRS.findMathTransform(targetCrs, sourceCrs, true);
        MathTransform to6933fixed = HMCylindricalEqualArea.createEpsg6933Forward();

        GeometryFactory gf = new GeometryFactory();

        // THIS SHOWS THE CURRENT GEOTOOLS BUG BEHAVIOR
        Point p6933 = gf.createPoint(new Coordinate(964862.802509, 1269436.7435378));
        Point p4326 = (Point) JTS.transform(p6933, to4326);
        Point p6933Back = (Point) JTS.transform(p4326, to6933);
        assertNotEquals(p4326.getX(), 10.0, TOL_DEGREES);
        assertNotEquals(p4326.getY(), 10.0, TOL_DEGREES);
        assertEquals(p6933.getX(), p6933Back.getX(), TOL_METERS);
        assertEquals(p6933.getY(), p6933Back.getY(), TOL_METERS);

        // THIS SHOWS THE FIXED BEHAVIOR
        Point p4326fixed = (Point) JTS.transform(p6933, to4326fixed);
        Point p6933BackFixed = (Point) JTS.transform(p4326fixed, to6933fixed);
        assertEquals(p4326fixed.getX(), 10.0, TOL_DEGREES);
        assertEquals(p4326fixed.getY(), 10.0, TOL_DEGREES);
        assertEquals(p6933.getX(), p6933BackFixed.getX(), TOL_METERS);
        assertEquals(p6933.getY(), p6933BackFixed.getY(), TOL_METERS);
    }

    @Test
    public void testHMCrsTransformerCoordinateGeometryAndEnvelopeMethods() throws Exception {
        HMCrsRegistry.INSTANCE.init();

        CoordinateReferenceSystem sourceCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:32632", true);
        CoordinateReferenceSystem targetCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true);
        MathTransform expectedTransform = CRS.findMathTransform(sourceCrs, targetCrs, true);

        HMCrsTransformer transformer = new HMCrsTransformer(sourceCrs, targetCrs);
        transformer.setAcceptLenientDatumShift(true);

        HMCrsTransformer transformerFromCodes = new HMCrsTransformer("EPSG:32632", "EPSG:4326", false, true);
        transformerFromCodes.setAcceptLenientDatumShift(true);

        MathTransform mathTransform = transformer.getMathTransform();
        assertNotNull(mathTransform);
        assertNotNull(transformerFromCodes.getMathTransform());

        Coordinate sourceCoordinate = HMTestMaps.getCenterCoord();
        Coordinate expectedCoordinate = JTS.transform(sourceCoordinate, null, expectedTransform);
        Coordinate transformedCoordinate = transformer.transform(sourceCoordinate);
        Coordinate transformedCoordinateFromCodes = transformerFromCodes.transform(sourceCoordinate);

        assertEquals(expectedCoordinate.x, transformedCoordinate.x, TOL_DEGREES);
        assertEquals(expectedCoordinate.y, transformedCoordinate.y, TOL_DEGREES);
        assertEquals(expectedCoordinate.x, transformedCoordinateFromCodes.x, TOL_DEGREES);
        assertEquals(expectedCoordinate.y, transformedCoordinateFromCodes.y, TOL_DEGREES);

        GeometryFactory gf = new GeometryFactory();
        Point point = gf.createPoint(sourceCoordinate);
        Point transformedPoint = (Point) transformer.transform(point);
        assertEquals(expectedCoordinate.x, transformedPoint.getX(), TOL_DEGREES);
        assertEquals(expectedCoordinate.y, transformedPoint.getY(), TOL_DEGREES);

        RegionMap region = HMTestMaps.getEnvelopeparams();
        Envelope sourceEnvelope = new Envelope(region.getWest(), region.getEast(), region.getSouth(), region.getNorth());
        Envelope expectedEnvelope = JTS.transform(sourceEnvelope, expectedTransform);
        Envelope transformedEnvelope = transformer.transform(sourceEnvelope);
        Envelope densifiedEnvelope = transformer.transformDensified(sourceEnvelope, 10);

        assertEquals(expectedEnvelope.getMinX(), transformedEnvelope.getMinX(), TOL_DEGREES);
        assertEquals(expectedEnvelope.getMaxX(), transformedEnvelope.getMaxX(), TOL_DEGREES);
        assertEquals(expectedEnvelope.getMinY(), transformedEnvelope.getMinY(), TOL_DEGREES);
        assertEquals(expectedEnvelope.getMaxY(), transformedEnvelope.getMaxY(), TOL_DEGREES);
        assertTrue(densifiedEnvelope.getWidth() >= transformedEnvelope.getWidth());
        assertTrue(densifiedEnvelope.getHeight() >= transformedEnvelope.getHeight());
    }

    @Test
    public void testHMCrsTransformerFeatureMethods() throws Exception {
        HMCrsRegistry.INSTANCE.init();

        CoordinateReferenceSystem sourceCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:32632", true);
        CoordinateReferenceSystem targetCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true);
        HMCrsTransformer transformer = new HMCrsTransformer(sourceCrs, targetCrs);

        SimpleFeatureCollection sourceCollection = HMTestMaps.getTestFC();
        SimpleFeatureCollection transformedCollection = transformer.transform(sourceCollection);
        assertTrue(HMCrsRegistry.crsEquals(targetCrs, transformedCollection.getSchema().getCoordinateReferenceSystem()));

        SimpleFeatureIterator sourceIterator = sourceCollection.features();
        SimpleFeatureIterator transformedIterator = transformedCollection.features();
        try {
            while( sourceIterator.hasNext() ) {
                SimpleFeature sourceFeature = sourceIterator.next();
                SimpleFeature transformedFeature = transformedIterator.next();
                Coordinate expected = transformer.transform(((Point) sourceFeature.getDefaultGeometry()).getCoordinate());
                Point transformedPoint = (Point) transformedFeature.getDefaultGeometry();
                assertEquals(expected.x, transformedPoint.getX(), TOL_DEGREES);
                assertEquals(expected.y, transformedPoint.getY(), TOL_DEGREES);
                assertEquals(sourceFeature.getAttribute("cat"), transformedFeature.getAttribute("cat"));
            }
        } finally {
            sourceIterator.close();
            transformedIterator.close();
        }

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("singleFeature");
        builder.setCRS(sourceCrs);
        builder.add("the_geom", Point.class);
        builder.add("id", Integer.class);
        SimpleFeatureType type = builder.buildFeatureType();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
        Coordinate sourceCoordinate = HMTestMaps.getCenterCoord();
        featureBuilder.addAll(new Object[]{new GeometryFactory().createPoint(sourceCoordinate), Integer.valueOf(7)});
        SimpleFeature singleFeature = featureBuilder.buildFeature("singleFeature.1");

        SimpleFeature transformedFeature = transformer.transform(singleFeature);
        assertSame(singleFeature, transformedFeature);
        Coordinate expected = transformer.transform(sourceCoordinate);
        Point transformedPoint = (Point) transformedFeature.getDefaultGeometry();
        assertEquals(expected.x, transformedPoint.getX(), TOL_DEGREES);
        assertEquals(expected.y, transformedPoint.getY(), TOL_DEGREES);
        assertEquals(Integer.valueOf(7), transformedFeature.getAttribute("id"));
    }

    @Test
    public void testHMCrsTransformer4326To102700() throws Exception {
        HMCrsRegistry.INSTANCE.init();

        // EPSG:4326 uses (lat, lon) axis order by default in GeoTools.
        // fromLongitudeFirst=true lets us pass (lon, lat) as expected by most callers.
        // EPSG:102700 (Montana State Plane, NAD83) is a projected CRS whose default
        // axis order is already (easting, northing), so toLongitudeFirst=false.
        HMCrsTransformer transformer = new HMCrsTransformer("EPSG:4326", "EPSG:102700", true, false);
        transformer.setAcceptLenientDatumShift(true);

        // A point near the centre of Montana: lon=-109.5, lat=47.0
        Coordinate source4326 = new Coordinate(-109.5, 47.0);
        Coordinate projected = transformer.transform(source4326);

        // Montana State Plane values should be in the hundreds-of-thousands of metres range
        assertTrue("Easting should be positive", projected.x > 0);
        assertTrue("Northing should be positive", projected.y > 0);

        // Round-trip back to 4326
        HMCrsTransformer inverse = new HMCrsTransformer("EPSG:102700", "EPSG:4326", false, true);
        inverse.setAcceptLenientDatumShift(true);
        Coordinate roundTrip = inverse.transform(projected);

        assertEquals(source4326.x, roundTrip.x, TOL_DEGREES);
        assertEquals(source4326.y, roundTrip.y, TOL_DEGREES);
    }

    // -----------------------------------------------------------------------
    // HMCrsRegistry – EPSG codes, bare integers, lowercase authority
    // -----------------------------------------------------------------------

    @Test
    public void testRegistry_epsg4326() throws Exception {
        HMCrsRegistry.INSTANCE.init();
        assertNotNull(HMCrsRegistry.INSTANCE.getCrs("EPSG:4326"));
    }

    @Test
    public void testRegistry_epsg32632() throws Exception {
        HMCrsRegistry.INSTANCE.init();
        assertNotNull(HMCrsRegistry.INSTANCE.getCrs("EPSG:32632"));
    }

    @Test
    public void testRegistry_lowercaseAuthority_epsg4326() throws Exception {
        HMCrsRegistry.INSTANCE.init();
        CoordinateReferenceSystem byUpper = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326");
        CoordinateReferenceSystem byLower = HMCrsRegistry.INSTANCE.getCrs("epsg:4326");
        assertNotNull(byLower);
        assertTrue(CRS.equalsIgnoreMetadata(byUpper, byLower));
    }

    @Test
    public void testRegistry_bareInt4326_treatedAsEpsg() throws Exception {
        HMCrsRegistry.INSTANCE.init();
        CoordinateReferenceSystem byFull = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326");
        CoordinateReferenceSystem byInt  = HMCrsRegistry.INSTANCE.getCrs("4326");
        assertNotNull(byInt);
        assertTrue(CRS.equalsIgnoreMetadata(byFull, byInt));
    }

    @Test
    public void testRegistry_bareInt32632_treatedAsEpsg() throws Exception {
        HMCrsRegistry.INSTANCE.init();
        CoordinateReferenceSystem byFull = HMCrsRegistry.INSTANCE.getCrs("EPSG:32632");
        CoordinateReferenceSystem byInt  = HMCrsRegistry.INSTANCE.getCrs("32632");
        assertNotNull(byInt);
        assertTrue(CRS.equalsIgnoreMetadata(byFull, byInt));
    }

    // -----------------------------------------------------------------------
    // HMCrsRegistry – longitudeFirst axis-order semantics
    //
    // For geographic CRS (EPSG:4326):
    //   longitudeFirst=true  → axis[0] is longitude (East direction)
    //   longitudeFirst=false → axis[0] is latitude  (North direction)
    // -----------------------------------------------------------------------

    @Test
    public void testRegistry_longitudeFirst_true_givesLongitudeAsFirstAxis() throws Exception {
        HMCrsRegistry.INSTANCE.init();
        CoordinateReferenceSystem crs = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true);
        String axis0Name = crs.getCoordinateSystem().getAxis(0).getName().getCode().toLowerCase();
        assertTrue("axis[0] should be longitude when longitudeFirst=true, got: " + axis0Name,
                axis0Name.contains("lon") || axis0Name.contains("east"));
    }

    @Test
    public void testRegistry_longitudeFirst_false_givesLatitudeAsFirstAxis() throws Exception {
        HMCrsRegistry.INSTANCE.init();
        CoordinateReferenceSystem crs = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", false);
        String axis0Name = crs.getCoordinateSystem().getAxis(0).getName().getCode().toLowerCase();
        assertTrue("axis[0] should be latitude when longitudeFirst=false, got: " + axis0Name,
                axis0Name.contains("lat") || axis0Name.contains("north"));
    }

    // -----------------------------------------------------------------------
    // HMCrsRegistry – ESRI authority
    //
    // If the ESRI factory is absent the test is skipped via assumeNoException.
    // -----------------------------------------------------------------------

    @Test
    public void testRegistry_esri102700() {
        HMCrsRegistry.INSTANCE.init();
        CoordinateReferenceSystem crs;
        try {
            crs = HMCrsRegistry.INSTANCE.getCrs("ESRI:102700");
        } catch (FactoryException e) {
            assumeNoException("ESRI CRS factory not available on this classpath", e);
            return;
        }
        assertNotNull(crs);
    }

    @Test
    public void testRegistry_esri102700_andEpsg102700_bothDecodable() throws Exception {
        HMCrsRegistry.INSTANCE.init();
        CoordinateReferenceSystem epsgCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:102700");
        assertNotNull("EPSG:102700 should decode to a non-null CRS", epsgCrs);

        CoordinateReferenceSystem esriCrs;
        try {
            esriCrs = HMCrsRegistry.INSTANCE.getCrs("ESRI:102700");
        } catch (FactoryException e) {
            assumeNoException("ESRI CRS factory not available on this classpath", e);
            return;
        }
        assertNotNull("ESRI:102700 should decode to a non-null CRS", esriCrs);

        // getCodeFromCrs prefers EPSG when an EPSG lookup succeeds — both may
        // produce "EPSG:102700", which is the expected and correct behaviour.
        String epsgId = CrsUtilities.getCodeFromCrs(epsgCrs);
        String esriId = CrsUtilities.getCodeFromCrs(esriCrs);
        assertTrue("authority prefix must be present", epsgId.contains(":"));
        assertTrue("authority prefix must be present", esriId.contains(":"));
    }

    // -----------------------------------------------------------------------
    // CrsUtilities.getCodeFromCrs – encoding round-trips (not deprecated)
    // -----------------------------------------------------------------------

    @Test
    public void testGetCodeFromCrs_epsg4326_roundTrip() throws Exception {
        CoordinateReferenceSystem crs = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326");
        assertEquals("EPSG:4326", CrsUtilities.getCodeFromCrs(crs));
    }

    @Test
    public void testGetCodeFromCrs_epsg32632_roundTrip() throws Exception {
        CoordinateReferenceSystem crs = HMCrsRegistry.INSTANCE.getCrs("EPSG:32632");
        assertEquals("EPSG:32632", CrsUtilities.getCodeFromCrs(crs));
    }

    @Test
    public void testGetCodeFromCrs_alwaysIncludesAuthorityPrefix() throws Exception {
        CoordinateReferenceSystem crs = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326");
        assertTrue(CrsUtilities.getCodeFromCrs(crs).contains(":"));
    }

    // -----------------------------------------------------------------------
    // CrsUtilities.getCrsFromSrid (no-flag variant) – backwards compatibility
    // -----------------------------------------------------------------------

    @Test
    public void testGetCrsFromSrid_4326_backwardsCompat() {
        // getCrsFromSrid(4326) uses a fast-path shortcut that returns CrsUtilities.WGS84 directly
        assertSame(CrsUtilities.WGS84, CrsUtilities.getCrsFromSrid(4326));
    }

    @Test
    public void testGetCrsFromSrid_32632_backwardsCompat() throws Exception {
        CoordinateReferenceSystem byRegistry = HMCrsRegistry.INSTANCE.getCrs("EPSG:32632");
        CoordinateReferenceSystem bySrid = CrsUtilities.getCrsFromSrid(32632);
        assertNotNull(bySrid);
        assertTrue(CRS.equalsIgnoreMetadata(byRegistry, bySrid));
    }

    // -----------------------------------------------------------------------
    // CrsUtilities deprecated methods – verify they still work and that
    // the doLatitudeFirst flag is now semantically correct (true = lat first).
    // -----------------------------------------------------------------------

    @Test
    @SuppressWarnings("deprecation")
    public void testGetCrsFromEpsg_noFlag_backwardsCompat() throws Exception {
        CoordinateReferenceSystem byRegistry = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326");
        CoordinateReferenceSystem byOld = CrsUtilities.getCrsFromEpsg("EPSG:4326");
        assertNotNull(byOld);
        assertTrue(CRS.equalsIgnoreMetadata(byRegistry, byOld));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGetCrsFromSrid_doLatitudeFirstTrue_givesLatitudeAsFirstAxis() throws Exception {
        // doLatitudeFirst=true must produce a CRS with latitude on axis[0]
        CoordinateReferenceSystem crs = CrsUtilities.getCrsFromSrid(4326, true);
        String axis0Name = crs.getCoordinateSystem().getAxis(0).getName().getCode().toLowerCase();
        assertTrue("doLatitudeFirst=true must give latitude as axis[0], got: " + axis0Name,
                axis0Name.contains("lat") || axis0Name.contains("north"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGetCrsFromSrid_doLatitudeFirstFalse_givesLongitudeAsFirstAxis() throws Exception {
        // doLatitudeFirst=false must produce a CRS with longitude on axis[0]
        CoordinateReferenceSystem crs = CrsUtilities.getCrsFromSrid(4326, false);
        String axis0Name = crs.getCoordinateSystem().getAxis(0).getName().getCode().toLowerCase();
        assertTrue("doLatitudeFirst=false must give longitude as axis[0], got: " + axis0Name,
                axis0Name.contains("lon") || axis0Name.contains("east"));
    }

    // -----------------------------------------------------------------------
    // (existing raster test follows)
    // -----------------------------------------------------------------------

    @Test
    public void testHMCrsTransformerRasterMethods() throws Exception {
        HMCrsRegistry.INSTANCE.init();

        CoordinateReferenceSystem sourceCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:32632", true);
        CoordinateReferenceSystem targetCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true);
        HMCrsTransformer transformer = new HMCrsTransformer(sourceCrs, targetCrs);

        RegionMap region = HMTestMaps.getEnvelopeparams();
        GridCoverage2D sourceCoverage = CoverageUtilities.buildCoverage("test", HMTestMaps.mapData, region, sourceCrs, true);
        HMRaster sourceRaster = HMRaster.fromGridCoverage(sourceCoverage);

        HMRaster transformedRaster = transformer.transform(sourceRaster);
        assertTrue(HMCrsRegistry.crsEquals(targetCrs, transformedRaster.getCrs()));
        assertTrue(transformedRaster.getCols() > 0);
        assertTrue(transformedRaster.getRows() > 0);

        Coordinate sourceCoordinate = HMTestMaps.getCenterCoord();
        Coordinate targetCoordinate = transformer.transform(sourceCoordinate);
        double transformedValue = transformedRaster.getValue(targetCoordinate);
        assertTrue(!transformedRaster.isNovalue(transformedValue));

        HMRaster transformedRasterWithGrid = transformer.transform(sourceRaster, transformedRaster.getGridGeometry(),
                Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        assertTrue(CRS.equalsIgnoreMetadata(targetCrs, transformedRasterWithGrid.getCrs()));
        assertEquals(transformedRaster.getCols(), transformedRasterWithGrid.getCols());
        assertEquals(transformedRaster.getRows(), transformedRasterWithGrid.getRows());
        assertEquals(transformedValue, transformedRasterWithGrid.getValue(targetCoordinate), 0.1);
    }

}
