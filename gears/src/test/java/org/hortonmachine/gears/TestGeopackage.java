package org.hortonmachine.gears;

import java.io.File;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.simple.SimpleFeature;
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

        SimpleFeatureCollection polygonFC = HMTestMaps.getTestLeftFC();
        LineString line = GeometryUtilities.createDummyLine();
        SimpleFeatureCollection lineFc = FeatureUtilities.featureCollectionFromGeometry(DefaultGeographicCRS.WGS84, line);

        File tmpGpkg = File.createTempFile("hm_test_multi_vector_", "." + HMConstants.GPKG);
        OmsVectorWriter.writeVector(tmpGpkg.getAbsolutePath() + HMConstants.DB_TABLE_PATH_SEPARATOR + polygonTable, polygonFC);
        OmsVectorWriter.writeVector(tmpGpkg.getAbsolutePath() + HMConstants.DB_TABLE_PATH_SEPARATOR + lineTable, lineFc);

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

    }

    public static void main( String[] args ) throws Exception {
        new TestGeopackage().testMultiVectorGeopackageIO();
    }

}
