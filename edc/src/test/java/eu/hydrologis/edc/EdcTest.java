package eu.hydrologis.edc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hibernate.Criteria;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

import eu.hydrologis.edc.annotatedclasses.BasinTypeTable;
import eu.hydrologis.edc.annotatedclasses.LandslideBasinRelationTable;
import eu.hydrologis.edc.annotatedclasses.LandslidesClassificationsTable;
import eu.hydrologis.edc.annotatedclasses.LandslidesTable;
import eu.hydrologis.edc.annotatedclasses.PoiTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;
import eu.hydrologis.edc.databases.EdcSessionFactory;
import eu.hydrologis.edc.databases.QueryHandler;
import eu.hydrologis.edc.utils.Constants;

public class EdcTest {
    // with this the test can be used for postgres (default is H2)
    private static DatabaseType type = DatabaseType.H2;

    private enum DatabaseType {
        H2, POSTGRESQL
    };
    private static File dbFolder;
    private static EDC edc;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {

        Properties properties = new Properties();
        switch( type ) {
        case H2:
            String tempdir = System.getProperty("java.io.tmpdir");
            File tmp = new File(tempdir);
            dbFolder = new File(tmp, "edctest");
            if (!dbFolder.exists())
                dbFolder.mkdirs();
            File dbFile = new File(dbFolder, "database");
            System.out.println("Creating database in: " + dbFolder.getAbsolutePath());
            /*
             * create a test database in H2
             */
            properties.put(Constants.TYPE, "H2");
            properties.put(Constants.HOST, "localhost");
            properties.put(Constants.PORT, "9092");
            properties.put(Constants.DATABASE, dbFile.getAbsolutePath());
            properties.put(Constants.USER, "sa");
            properties.put(Constants.PASS, "");
            properties.put(Constants.SHOW_SQL, "false");
            properties.put(Constants.FORMAT_SQL, "false");

            edc = new EDC(properties, System.out);

            // generate database
            edc.generateDatabase(false, false, true);

            File[] filesArray = dbFolder.listFiles();
            assertTrue(filesArray.length > 0);
            break;
        case POSTGRESQL:
            /*
             * create a test database in Postgres
             */
            properties.put(Constants.TYPE, "POSTGRESQL");
            properties.put(Constants.HOST, "localhost");
            properties.put(Constants.PORT, "5432");
            properties.put(Constants.DATABASE, "test");
            properties.put(Constants.USER, "silli");
            properties.put(Constants.PASS, "akunamatata77");
            properties.put(Constants.SHOW_SQL, "false");
            properties.put(Constants.FORMAT_SQL, "false");

            edc = new EDC(properties, System.out);

            break;
        default:
            break;
        }

    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        if (edc.hasEdcSessionFactory()) {
            EdcSessionFactory edcSessionFactory = edc.getEdcSessionFactory();
            edcSessionFactory.closeSessionFactory();
        }

        if (type == DatabaseType.H2) {
            File[] filesArray = dbFolder.listFiles();
            for( File file : filesArray ) {
                boolean deleted = file.delete();
                assertTrue(deleted);
            }
            boolean deleted = dbFolder.delete();
            assertTrue(deleted);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInsert() throws URISyntaxException, Exception {
        // insert some example data
        File here = new File(".");
        File unitsCsv = new File(here, "src/test/java/eu/hydrologis/edc/units.csv");
        assertTrue(unitsCsv.exists());

        edc.insertFromCsv("units", unitsCsv);

        // query those data
        EdcSessionFactory sessionFactory = edc.getEdcSessionFactory();
        Session session = sessionFactory.openSession();
        try {
            Criteria criteria = session.createCriteria(UnitsTable.class);
            List<UnitsTable> resultsList = criteria.list();
            assertTrue(resultsList.size() == 11);
        } finally {
            session.close();
        }

        // add one programmatically
        UnitsTable newUnit = new UnitsTable();
        newUnit.setId(11l);
        newUnit.setName("Dummy unit");
        newUnit.setDescription("A dummy unit for testing purposes");
        newUnit.setToPrincipal(1.0);

        session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        try {
            session.save(newUnit);
            transaction.commit();
        } finally {
            session.close();
        }

        // query data again
        session = sessionFactory.openSession();
        try {
            Criteria criteria = session.createCriteria(UnitsTable.class);
            List<UnitsTable> resultsList = criteria.list();
            assertTrue(resultsList.size() == 12);
        } finally {
            session.close();
        }
    }

    @Test
    public void testPointInsert() throws Exception {
        EdcSessionFactory sessionFactory = edc.getEdcSessionFactory();
        QueryHandler queryHandler = sessionFactory.getQueryHandler();

        Long id1 = 1l;
        Long id2 = 2l;

        // we need a poi
        PoiTable poi1 = new PoiTable();
        poi1.setId(id1);
        poi1.setName("Test poi 1");
        poi1.setElevation(100.0);
        PoiTable poi2 = new PoiTable();
        poi2.setId(id2);
        poi2.setName("Test poi 2");
        poi2.setElevation(200.0);
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        try {
            session.save(poi1);
            session.save(poi2);
            transaction.commit();
        } finally {
            session.close();
        }

        // create a poi geometry
        session = sessionFactory.openSession();
        queryHandler.insertPointGeometry(session, "edcgeometries", "poigeometries", id1,
                new Coordinate(11.0, 45.0), DefaultGeographicCRS.WGS84);
        queryHandler.insertPointGeometry(session, "edcgeometries", "poigeometries", id2,
                new Coordinate(12.0, 46.0), "EPSG:4326");

        // check if it is there
        Map<Long, Geometry> geometries = queryHandler.getGeometries("edcgeometries",
                "poigeometries", "EPSG:4326", id1, id2);
        assertNotNull(geometries);
        Geometry geometry1 = geometries.get(id1);
        Geometry geometry2 = geometries.get(id2);
        assertNotNull(geometry1);
        assertNotNull(geometry2);

        Coordinate coordinate = geometry1.getCoordinate();
        assertEquals(coordinate.x, 11.0);
        assertEquals(coordinate.y, 45.0);
        coordinate = geometry2.getCoordinate();
        assertEquals(coordinate.x, 12.0);
        assertEquals(coordinate.y, 46.0);
    }

    @Test
    public void testLineInsert() throws Exception {
        EdcSessionFactory sessionFactory = edc.getEdcSessionFactory();
        QueryHandler queryHandler = sessionFactory.getQueryHandler();

        Long id3 = 3l;
        Long id4 = 4l;
        // we need a poi
        PoiTable poi1 = new PoiTable();
        poi1.setId(id3);
        poi1.setName("Test poi 3");
        poi1.setElevation(100.0);
        PoiTable poi2 = new PoiTable();
        poi2.setId(id4);
        poi2.setName("Test poi 4");
        poi2.setElevation(200.0);
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        try {
            session.save(poi1);
            session.save(poi2);
            transaction.commit();
        } finally {
            session.close();
        }

        // create a poi geometry
        session = sessionFactory.openSession();

        GeometryFactory gF = new GeometryFactory();
        Coordinate[] lineCoords = new Coordinate[2];
        lineCoords[0] = new Coordinate(11.0, 45.0, 100.0);
        lineCoords[1] = new Coordinate(12.0, 46.0, 200.0);
        LineString lineString = gF.createLineString(lineCoords);

        queryHandler.insertLinestringGeometry3D(session, "edcgeometries", "obstructiongeometries",
                id3, lineString, DefaultGeographicCRS.WGS84);
        queryHandler.insertLinestringGeometry3D(session, "edcgeometries", "obstructiongeometries",
                id4, lineString, "EPSG:4326");

        // check if it is there
        Map<Long, Geometry> geometries = queryHandler.getGeometries3D("edcgeometries",
                "obstructiongeometries", "EPSG:4326", id3, id4);
        assertNotNull(geometries);
        Geometry geometry1 = geometries.get(id3);
        Geometry geometry2 = geometries.get(id4);
        assertNotNull(geometry1);
        assertNotNull(geometry2);

        Coordinate[] coordinates = geometry1.getCoordinates();
        assertEquals(11.0, coordinates[0].x);
        assertEquals(45.0, coordinates[0].y);
        assertEquals(12.0, coordinates[1].x);
        assertEquals(46.0, coordinates[1].y);
        if (type == DatabaseType.POSTGRESQL) {
            assertEquals(100.0, coordinates[0].z);
            assertEquals(200.0, coordinates[1].z);
        }
        coordinates = geometry2.getCoordinates();
        assertEquals(11.0, coordinates[0].x);
        assertEquals(45.0, coordinates[0].y);
        assertEquals(12.0, coordinates[1].x);
        assertEquals(46.0, coordinates[1].y);
        if (type == DatabaseType.POSTGRESQL) {
            assertEquals(100.0, coordinates[0].z);
            assertEquals(200.0, coordinates[1].z);
        }
    }

    @Test
    public void testPoligonInsert() throws Exception {
        EdcSessionFactory sessionFactory = edc.getEdcSessionFactory();
        QueryHandler queryHandler = sessionFactory.getQueryHandler();

        LandslidesClassificationsTable lCT = new LandslidesClassificationsTable();
        lCT.setId(1l);
        lCT.setMaterialType("some material");
        lCT.setMovementType("some movement");

        LandslideBasinRelationTable lBRT = new LandslideBasinRelationTable();
        lBRT.setId(1l);
        lBRT.setName("fully contained");

        BasinTypeTable bTT = new BasinTypeTable();
        bTT.setId(1l);
        bTT.setName("some basin");

        LandslidesTable lT = new LandslidesTable();
        lT.setId(1l);
        lT.setBasinType(bTT);
        lT.setLandslideBasinRelation(lBRT);
        lT.setLandslideClassification(lCT);
        lT.setName("some landslide");
        lT.setSurveyDate(new DateTime());

        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        try {
            session.save(lCT);
            session.save(lBRT);
            session.save(bTT);
            session.save(lT);
            transaction.commit();
        } finally {
            session.close();
        }

        // create a landslide geometry
        session = sessionFactory.openSession();

        GeometryFactory gF = new GeometryFactory();
        Coordinate[] polygonCoords = new Coordinate[4];
        polygonCoords[0] = new Coordinate(11.0, 45.0, 100.0);
        polygonCoords[1] = new Coordinate(12.0, 46.0, 200.0);
        polygonCoords[2] = new Coordinate(11.0, 46.0, 300.0);
        polygonCoords[3] = new Coordinate(11.0, 45.0, 100.0);
        LinearRing linearRing = gF.createLinearRing(polygonCoords);
        Polygon polygon = gF.createPolygon(linearRing, null);

        queryHandler.insertPolygonalGeometry(session, "edcgeometries", "landslidesgeometries", 1l,
                polygon, DefaultGeographicCRS.WGS84);

        // check if it is there
        Map<Long, Geometry> geometries = queryHandler.getGeometries("edcgeometries",
                "landslidesgeometries", "EPSG:4326", 1l);
        assertNotNull(geometries);
        Geometry geometry = geometries.get(1l);
        assertNotNull(geometry);

        Coordinate[] coordinates = geometry.getCoordinates();
        assertEquals(11.0, coordinates[0].x);
        assertEquals(45.0, coordinates[0].y);
        assertEquals(12.0, coordinates[1].x);
        assertEquals(46.0, coordinates[1].y);
        assertEquals(11.0, coordinates[2].x);
        assertEquals(46.0, coordinates[2].y);
    }
}
