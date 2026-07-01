package org.hortonmachine.gears;

import java.io.File;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.dbs.TestUtilities;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.crs.CrsUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.geotools.api.feature.simple.SimpleFeature;
/**
 * Test Geopackage.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestGeopackage extends HMTestCase {

    @SuppressWarnings("nls")
    public void testMultiVectorGeopackageIO() throws Exception {
        String polygonTable = "polygontest";
        String lineTable = "linetest";
        String multiPolygonTable = "multipolygontest";

        SimpleFeatureCollection polygonFC = HMTestMaps.getTestLeftFC();
        LineString line = GeometryUtilities.createDummyLine();
        SimpleFeatureCollection lineFc = FeatureUtilities.featureCollectionFromGeometry(DefaultGeographicCRS.WGS84, line);
        Polygon polygon1 = GeometryUtilities.createDummyPolygon();
        Polygon polygon2 = (Polygon) GeometryUtilities.createDummyPolygon().copy();
        polygon2.apply(org.locationtech.jts.geom.util.AffineTransformation.translationInstance(20, 20));
        polygon2.geometryChanged();
        MultiPolygon multiPolygon = GeometryUtilities.gf().createMultiPolygon(new Polygon[]{polygon1, polygon2});
        SimpleFeatureCollection multiPolygonFc = FeatureUtilities.featureCollectionFromGeometry(DefaultGeographicCRS.WGS84,
                multiPolygon);

        File tmpGpkg = File.createTempFile("hm_test_multi_vector_", "." + HMConstants.GPKG);
        OmsVectorWriter.writeVector(tmpGpkg.getAbsolutePath() + HMConstants.DB_TABLE_PATH_SEPARATOR + polygonTable, polygonFC);
        OmsVectorWriter.writeVector(tmpGpkg.getAbsolutePath() + HMConstants.DB_TABLE_PATH_SEPARATOR + lineTable, lineFc);
        OmsVectorWriter.writeVector(tmpGpkg.getAbsolutePath() + HMConstants.DB_TABLE_PATH_SEPARATOR + multiPolygonTable,
                multiPolygonFc);

        SimpleFeatureCollection readPolygonFC = OmsVectorReader
                .readVector(tmpGpkg.getAbsolutePath() + HMConstants.DB_TABLE_PATH_SEPARATOR + polygonTable);
        int srid = CrsUtilities.getSrid(readPolygonFC.getSchema().getCoordinateReferenceSystem());
        assertEquals(32632, srid);
        List<SimpleFeature> features = FeatureUtilities.featureCollectionToList(readPolygonFC);
        assertEquals(1, features.size());

        SimpleFeatureCollection readLinesFC = OmsVectorReader
                .readVector(tmpGpkg.getAbsolutePath() + HMConstants.DB_TABLE_PATH_SEPARATOR + lineTable);
        srid = CrsUtilities.getSrid(readLinesFC.getSchema().getCoordinateReferenceSystem());
        assertEquals(4326, srid);
        features = FeatureUtilities.featureCollectionToList(readLinesFC);
        assertEquals(1, features.size());

        SimpleFeatureCollection readMultiPolygonFC = OmsVectorReader
                .readVector(tmpGpkg.getAbsolutePath() + HMConstants.DB_TABLE_PATH_SEPARATOR + multiPolygonTable);
        srid = CrsUtilities.getSrid(readMultiPolygonFC.getSchema().getCoordinateReferenceSystem());
        assertEquals(4326, srid);
        features = FeatureUtilities.featureCollectionToList(readMultiPolygonFC);
        assertEquals(1, features.size());
        assertTrue(features.get(0).getDefaultGeometry() instanceof MultiPolygon);

	}

	public void testInsertionOf2FeatureCollectionsInSameTable() throws Exception {

		var geomsPointsList1 = new Geometry[] { g("POINT(1 1)"), g("POINT(2 2)"), g("POINT(3 3)") };
		var geomsPointsList2 = new Geometry[] { g("POINT(4 4)"), g("POINT(5 5)"), g("POINT(6 6)") };

		File gpkgFile = TestUtilities.createTmpFile(".gpkg");
		gpkgFile.delete();
		try (GeopackageCommonDb db = (GeopackageCommonDb) EDb.GEOPACKAGE.getSpatialDb()) {
			db.open(gpkgFile.getAbsolutePath());
			db.initSpatialMetadata("WGS84");

			SimpleFeatureCollection fc1 = FeatureUtilities
					.featureCollectionFromGeometry(HMCrsRegistry.INSTANCE.getCrs("4326"), geomsPointsList1);
			SimpleFeatureCollection fc2 = FeatureUtilities
					.featureCollectionFromGeometry(HMCrsRegistry.INSTANCE.getCrs("4326"), geomsPointsList2);

			SqlName tableName = SqlName.m("pointstest");
			SpatialDbsImportUtils.createTableFromSchema(db, fc1.getSchema(), tableName, null, false);

			SpatialDbsImportUtils.importFeatureCollection(db, fc1, tableName, -1, false, pm);
			List<Geometry> geometries = db.getGeometries(tableName);
			assertEquals(3, geometries.size());

			SpatialDbsImportUtils.importFeatureCollection(db, fc2, tableName, -1, false, pm);
			geometries = db.getGeometries(tableName);
			assertEquals(6, geometries.size());

		} finally {
			gpkgFile.delete();
		}
	}

	private Geometry g(String wkt) throws ParseException {
		WKTReader wktReader = new WKTReader();
		Geometry geometry = wktReader.read(wkt);
		geometry.setSRID(4326);
		return geometry;
	}

	public static void main(String[] args) throws Exception {
		new TestGeopackage().testMultiVectorGeopackageIO();
	}

}
