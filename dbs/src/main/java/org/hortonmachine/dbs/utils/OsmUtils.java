/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.dbs.utils;

import java.io.File;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;

/**
 * Utilities to handle OSM data.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class OsmUtils {

    private static final String BUILDING = "building";

    public static void downloadOsmOnRegion( String polygonWGS84WKT, File outXmlFile, File optionalDb ) throws Exception {
        File tmpDb = optionalDb;
        if (optionalDb == null)
            File.createTempFile("osmextract", "tmp");
        try (ASpatialDb db = EDb.H2GIS.getSpatialDb()) {
            db.open(tmpDb.getAbsolutePath());
            db.initSpatialMetadata(null);

            String call = "CALL ST_OSMDownloader(st_setsrid('" + polygonWGS84WKT + "'::geometry, 4326), '"
                    + outXmlFile.getAbsolutePath() + "');";
            db.executeInsertUpdateDeleteSql(call);
        } finally {
            if (optionalDb != null)
                tmpDb.delete();
        }
    }

    public static String osm2H2gisDb( File osmXmlFile, File outDb ) throws Exception {
        String tablePrefix = osmXmlFile.getName().replace('.', '_');
        String call = "CALL OSMRead('" + osmXmlFile.getAbsolutePath() + "', '" + tablePrefix + "', true);";

        try (ASpatialDb db = EDb.H2GIS.getSpatialDb()) {
            boolean existed = db.open(outDb.getAbsolutePath());
            if (!existed)
                db.initSpatialMetadata(null);

            db.executeInsertUpdateDeleteSql(call);
        }
        return tablePrefix;
    }

    public static void createBuildings( File dbFile, String tablePrefix, String newTableName ) throws Exception {
        String[] sqls = {
                "DROP TABLE IF EXISTS " + newTableName + ";", "CREATE TABLE " + newTableName
                        + "(ID_WAY BIGINT PRIMARY KEY) AS SELECT DISTINCT ID_WAY FROM " + tablePrefix + "_WAY_TAG WT, "
                        + tablePrefix + "_TAG T WHERE WT.ID_TAG = T.ID_TAG AND T.TAG_KEY IN ('" + BUILDING + "');",
                "DROP TABLE IF EXISTS " + newTableName + "_geom;",
                "CREATE TABLE " + newTableName
                        + "_geom AS SELECT ID_WAY, ST_MAKEPOLYGON(ST_MAKELINE(THE_GEOM)) THE_GEOM FROM (SELECT (SELECT ST_ACCUM(THE_GEOM) "
                        + "THE_GEOM FROM (SELECT N.ID_NODE, N.THE_GEOM,WN.ID_WAY IDWAY FROM " + tablePrefix + "_NODE N,"
                        + tablePrefix
                        + "_WAY_NODE WN WHERE N.ID_NODE = WN.ID_NODE ORDER BY WN.NODE_ORDER) WHERE  IDWAY = W.ID_WAY) THE_GEOM ,W.ID_WAY FROM "
                        + tablePrefix + "_WAY W," + newTableName
                        + " B WHERE W.ID_WAY = B.ID_WAY) GEOM_TABLE WHERE ST_GEOMETRYN(THE_GEOM,1) = ST_GEOMETRYN(THE_GEOM, ST_NUMGEOMETRIES(THE_GEOM)) AND ST_NUMGEOMETRIES(THE_GEOM) > 2;"};
        try (ASpatialDb db = EDb.H2GIS.getSpatialDb()) {
            boolean existed = db.open(dbFile.getAbsolutePath());
            if (existed) {
                for( String sql : sqls ) {
                    System.out.println("RUN: " + sql);
                    db.runSql(sql);
                }
            }
        }
    }

    public static void main( String[] args ) throws Exception {
        
        File osmFile = new File("/home/hydrologis/TMP/export_rome.osm");
        // osmFile.delete();
        File h2gisDb = new File("/home/hydrologis/TMP/rome_db");
        // String polyWKT = "POLYGON ((12.428254028992626 42.15874346697887, 12.435226608204589
        // 42.16393108440939, 12.473963159382151 42.169694601267054, 12.513474441583266
        // 42.16681290895843, 12.560733034019895 42.14894346508291, 12.588623350867737
        // 42.120111070633094, 12.604117971338765 42.087803091542696, 12.592497005985496
        // 42.06702491881296, 12.570029806302509 42.034689695061466, 12.544463682525317
        // 42.00233788982357, 12.49720509008869 42.00060428949245, 12.43677607025169
        // 42.007538405322215, 12.408885753403844 42.01447175982285, 12.400363712144783
        // 42.02775856120024, 12.39339113293282 42.05143679179608, 12.397264788050576
        // 42.07626050835503, 12.397264788050576 42.0999205272566, 12.394940594979921
        // 42.123571656120696, 12.402687905215435 42.14606082066878, 12.428254028992626
        // 42.15874346697887))";
        // downloadOsmOnRegion(polyWKT, osmFile, h2gisDb);
        String tablePrefix = osm2H2gisDb(osmFile, h2gisDb);
        createBuildings(h2gisDb, tablePrefix, "buildings");

        
        
        
    }

}
