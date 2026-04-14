package org.hortonmachine.gears;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.imagen.Interpolation;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
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

        HMCrsTransformer transformerFromCodes = new HMCrsTransformer("EPSG:32632", "EPSG:4326", true);
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
