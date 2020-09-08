package org.hortonmachine.dbs;

import java.io.File;
import java.io.IOException;

import org.hortonmachine.dbs.compat.ADatabaseSyntaxHelper;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IGeometryParser;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

public class TestUtilities {
        public static final String MPOLY_TABLE = "multipoly";
        public static final String POLY_TABLE = "poly";
        public static final String MPOINTS_TABLE = "mpoints";
        public static final String POINTS_TABLE = "points";
        public static final String MLINES_TABLE = "mlines";
        public static final String LINES_TABLE = "lines";
        public static final String GEOMCOLL_TABLE = "geomcoll";

        public static File createTmpFile( String ext ) throws IOException {
                return File.createTempFile("hm-dbs-", ext);
        }

        public static Object sqlObjectFromWkt( String wkt, ASpatialDb db ) throws Exception {
                IGeometryParser gp = db.getType().getGeometryParser();
                Geometry geometry = new WKTReader().read(wkt);
                geometry.setSRID(4326);
                return gp.toSqlObject(geometry);
        }
        public static Object geomFromWkt( boolean withWkt, String wkt, ASpatialDb db ) throws Exception {
                if (withWkt) {
                        Geometry geometry = new WKTReader().read(wkt);
                        geometry.setSRID(4326);
                        return geometry;
                } else {
                        return sqlObjectFromWkt(wkt, db);
                }
        }

        public static void createGeomTablesAndPopulate( ASpatialDb db, boolean withWkt ) throws Exception {
                String geomQM = "ST_GeomFromText(?, 4326)";
                if (!withWkt) {
                        geomQM = "?";
                }

                Object[][] insertsWithWkt = {//
                                // MPOLY_TABLE
                                {"INSERT INTO " + MPOLY_TABLE + " (id, name, temperature, the_geom) VALUES(1, 'Tscherms', 36.0, "
                                                + geomQM + ");",
                                                geomFromWkt(withWkt, "MULTIPOLYGON (((0 10, 10 10, 10 0, 0 0, 0 10)))", db)},
                                {"INSERT INTO " + MPOLY_TABLE + " (id, name, temperature, the_geom) VALUES(2, 'Meran', 34.0, "
                                                + geomQM + ");",
                                                geomFromWkt(withWkt,
                                                                "MULTIPOLYGON (((0 20, 20 20, 20 0, 0 0, 0 20)),((21 11, 21 6, 25 6, 25 11, 21 11)), ((21 18, 21 16, 23 16, 23 18, 21 18)))",
                                                                db)},
                                // POLY_TABLE
                                {"INSERT INTO " + POLY_TABLE + " (id, name, temperature, the_geom) VALUES(1, 'Tscherms', 36.0, "
                                                + geomQM + ");",
                                                geomFromWkt(withWkt, "POLYGON ((0 10, 10 10, 10 0, 0 0, 0 10))", db)},
                                {"INSERT INTO " + POLY_TABLE + " (id, name, temperature, the_geom) VALUES(2, 'Meran', 34.0, "
                                                + geomQM + ");",
                                                geomFromWkt(withWkt, "POLYGON ((0 20, 20 20, 20 0, 0 0, 0 20))", db)},
                                // MPOINTS_TABLE
                                {"INSERT INTO " + MPOINTS_TABLE + " (id, table1id, the_geom) VALUES(1, 1, " + geomQM + ");",
                                                geomFromWkt(withWkt, "MULTIPOINT ((5 5), (7 7))", db)}, //
                                // POINTS_TABLE
                                {"INSERT INTO " + POINTS_TABLE + " (id, table1id, the_geom) VALUES(1, 1, " + geomQM + ");",
                                                geomFromWkt(withWkt, "POINT (5 5)", db)}, //
                                {"INSERT INTO " + POINTS_TABLE + " (id, table1id, the_geom) VALUES(2, 2, " + geomQM + ");",
                                                geomFromWkt(withWkt, "POINT (10 10)", db)}, //
                                // MLINES_TABLE
                                {"INSERT INTO " + MLINES_TABLE + " (id, table1id, the_geom) VALUES(1, 1, " + geomQM + ");",
                                                geomFromWkt(withWkt, "MULTILINESTRING ((-1 2, 21 2), (-1 4, 23 4, 23 12))", db)}, //
                                // LINES_TABLE
                                {"INSERT INTO " + LINES_TABLE + " (id, table1id, the_geom) VALUES(1, 1, " + geomQM + ");",
                                                geomFromWkt(withWkt, "LINESTRING (-1 3, 12 3, 12 -0.8)", db)}, //
                                //
                };
                Object[][] insertsStraight = {//
                                // MPOLY_TABLE
                                {"INSERT INTO " + MPOLY_TABLE + " (id, name, temperature, the_geom) VALUES(3, 'Bozen', 42.0, ?);",
                                                sqlObjectFromWkt("MULTIPOLYGON (((50 80, 100 80, 100 50, 50 50, 50 80)))", db)},
                                // POLY_TABLE
                                {"INSERT INTO " + POLY_TABLE + " (id, name, temperature, the_geom) VALUES(3, 'Bozen', 42.0, ?);",
                                                sqlObjectFromWkt("POLYGON ((50 80, 100 80, 100 50, 50 50, 50 80))", db)},
                                // MPOINTS_TABLE
                                {"INSERT INTO " + MPOINTS_TABLE + " (id, table1id, the_geom) VALUES(2, 2, ?);",
                                                sqlObjectFromWkt("MULTIPOINT ((10 10))", db)}, //
                                // POINTS_TABLE
                                {"INSERT INTO " + POINTS_TABLE + " (id, table1id, the_geom) VALUES(3, 3, ?);",
                                                sqlObjectFromWkt("POINT (75 75)", db)}, //
                                // LINES_TABLE
                                {"INSERT INTO " + LINES_TABLE + " (id, table1id, the_geom) VALUES(2, 2, ?);",
                                                sqlObjectFromWkt("LINESTRING (20.5 20, 20.5 5)", db)}, //

                                //
                };

                createGeometryTables(db);

                for( Object[] insert : insertsWithWkt ) {
                        String sql = (String) insert[0];
                        if (withWkt) {
                                Geometry geom = (Geometry) insert[1];
                                db.executeInsertUpdateDeletePreparedSql(sql, new Object[]{geom.toText()});
                        } else {
                                Object geom = insert[1];
                                db.executeInsertUpdateDeletePreparedSql(sql, new Object[]{geom});
                        }
                }
                for( Object[] insert : insertsStraight ) {
                        String sql = (String) insert[0];
                        Object geom = insert[1];
                        db.executeInsertUpdateDeletePreparedSql(sql, new Object[]{geom});
                }
        }

        public static void createGeometryTables( ASpatialDb db ) throws Exception {
                ADatabaseSyntaxHelper dt = db.getType().getDatabaseSyntaxHelper();

                db.createSpatialTable(MPOLY_TABLE, 4326, "the_geom MULTIPOLYGON",
                                arr("id " + dt.INTEGER() + " PRIMARY KEY", "name VARCHAR(255)", "temperature REAL"));
                db.createSpatialTable(POLY_TABLE, 4326, "the_geom POLYGON",
                                arr("id " + dt.INTEGER() + " PRIMARY KEY", "name VARCHAR(255)", "temperature REAL"));

                db.createSpatialTable(MPOINTS_TABLE, 4326, "the_geom MULTIPOINT",
                                arr("id " + dt.INTEGER() + " PRIMARY KEY", "table1id " + dt.INTEGER() + ""),
                                new String[]{"FOREIGN KEY (table1id) REFERENCES " + MPOLY_TABLE + "(id)"}, false);
                db.createSpatialTable(POINTS_TABLE, 4326, "the_geom POINT",
                                arr("id " + dt.INTEGER() + " PRIMARY KEY", "table1id " + dt.INTEGER() + ""),
                                new String[]{"FOREIGN KEY (table1id) REFERENCES " + POLY_TABLE + "(id)"}, false);

                db.createSpatialTable(MLINES_TABLE, 4326, "the_geom MULTILINESTRING",
                                arr("id " + dt.INTEGER() + " PRIMARY KEY", "table1id " + dt.INTEGER() + ""),
                                new String[]{"FOREIGN KEY (table1id) REFERENCES " + MPOLY_TABLE + "(id)"}, false);
                db.createSpatialTable(LINES_TABLE, 4326, "the_geom LINESTRING",
                                arr("id " + dt.INTEGER() + " PRIMARY KEY", "table1id " + dt.INTEGER() + ""),
                                new String[]{"FOREIGN KEY (table1id) REFERENCES " + POLY_TABLE + "(id)"}, false);
        }

        public static String[] arr( String... strings ) {
                return strings;
        }

        public static void deletePrevious( String tempDir, String dbPathOnCreation, EDb dbType ) {
                String dbPath = dbPathOnCreation + "." + dbType.getExtension();
                new File(dbPath).delete();
                new File(dbPathOnCreation).delete();
        }
}
