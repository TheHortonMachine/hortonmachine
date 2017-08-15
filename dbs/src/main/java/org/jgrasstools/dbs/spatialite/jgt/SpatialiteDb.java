/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.dbs.spatialite.jgt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Clob;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.compat.GeometryColumn;
import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTResultSetMetaData;
import org.jgrasstools.dbs.compat.IJGTStatement;
import org.jgrasstools.dbs.compat.objects.ForeignKey;
import org.jgrasstools.dbs.compat.objects.QueryResult;
import org.jgrasstools.dbs.spatialite.ESpatialiteGeometryType;
import org.jgrasstools.dbs.spatialite.RasterCoverage;
import org.jgrasstools.dbs.spatialite.SpatialiteCommonMethods;
import org.jgrasstools.dbs.spatialite.SpatialiteTableNames;
import org.jgrasstools.dbs.spatialite.SpatialiteWKBReader;
import org.jgrasstools.dbs.utils.OsCheck;
import org.jgrasstools.dbs.utils.OsCheck.OSType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * A spatialite database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialiteDb extends ASpatialDb {
    private static final Logger logger = LoggerFactory.getLogger(SpatialiteDb.class);
    private SqliteDb sqliteDb;

    private SpatialiteWKBReader wkbReader = new SpatialiteWKBReader();

    public SpatialiteDb() {
        sqliteDb = new SqliteDb();
    }

    public boolean open( String dbPath ) throws Exception {
        boolean dbExists = sqliteDb.open(dbPath);
        this.mDbPath = sqliteDb.getDatabasePath();
        mConn = sqliteDb.getConnection();
        try (IJGTStatement stmt = mConn.createStatement()) {
            // set timeout to 30 sec.
            stmt.setQueryTimeout(30);
            // load SpatiaLite
            try {
                OSType operatingSystemType = OsCheck.getOperatingSystemType();
                switch( operatingSystemType ) {
                case Linux:
                case MacOS:
                    try {
                        stmt.execute("SELECT load_extension('mod_rasterlite2.so', 'sqlite3_modrasterlite_init')");
                    } catch (Exception e) {
                        if (mPrintInfos) {
                            logger.info("Unable to load mod_rasterlite2.so: " + e.getMessage());
                        }
                        try {
                            stmt.execute("SELECT load_extension('mod_rasterlite2', 'sqlite3_modrasterlite_init')");
                        } catch (Exception e1) {
                            logger.info("Unable to load mod_rasterlite2: " + e1.getMessage());
                        }
                    }
                    try {
                        stmt.execute("SELECT load_extension('mod_spatialite.so', 'sqlite3_modspatialite_init')");
                    } catch (Exception e) {
                        if (mPrintInfos) {
                            logger.info("Unable to load mod_spatialite.so: " + e.getMessage());
                        }
                        try {
                            stmt.execute("SELECT load_extension('mod_spatialite', 'sqlite3_modspatialite_init')");
                        } catch (Exception e1) {
                            logger.info("Unable to load mod_spatialite: " + e1.getMessage());
                        }
                        throw e;
                    }
                    break;
                default:
                    try {
                        stmt.execute("SELECT load_extension('mod_rasterlite2', 'sqlite3_modrasterlite_init')");
                    } catch (Exception e) {
                        if (mPrintInfos) {
                            logger.info("Unable to load mod_rasterlite2: " + e.getMessage());
                        }
                    }
                    try {
                        stmt.execute("SELECT load_extension('mod_spatialite', 'sqlite3_modspatialite_init')");
                    } catch (Exception e) {
                        if (mPrintInfos) {
                            logger.info("Unable to load mod_spatialite: " + e.getMessage());
                        }
                        throw e;
                    }
                    break;
                }
            } catch (Exception e) {
                throw e;
                // Map<String, String> getenv = System.getenv();
                // for( Entry<String, String> entry : getenv.entrySet() ) {
                // System.out.println(entry.getKey() + ": " + entry.getValue());
                // }
            }
        }
        if (mPrintInfos) {
            String[] dbInfo = getDbInfo();
            logger.info("Spatialite Version: " + dbInfo[0]);
            logger.info("Spatialite Target CPU: " + dbInfo[1]);
        }
        return dbExists;
    }

    public Connection getJdbcConnection() {
        return sqliteDb.getJdbcConnection();
    }

    @Override
    public void initSpatialMetadata( String options ) throws Exception {
        SpatialiteCommonMethods.initSpatialMetadata(this, options);
    }

    public String[] getDbInfo() throws Exception {
        // checking SQLite and SpatiaLite version + target CPU
        String sql = "SELECT spatialite_version(), spatialite_target_cpu()";
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            String[] info = new String[2];
            while( rs.next() ) {
                // read the result set
                info[0] = rs.getString(1);
                info[1] = rs.getString(2);
            }
            return info;
        }
    }

    public void createSpatialTable( String tableName, int tableSrid, String geometryFieldData, String[] fieldData,
            String[] foreignKeys, boolean avoidIndex ) throws Exception {
        SpatialiteCommonMethods.createSpatialTable(this, tableName, tableSrid, geometryFieldData, fieldData, foreignKeys,
                avoidIndex);
    }
    
    public String checkSqlCompatibilityIssues( String sql ) {
        return SpatialiteCommonMethods.checkCompatibilityIssues(sql);
    }
    
    @Override
    public Envelope getTableBounds( String tableName ) throws Exception {
        return SpatialiteCommonMethods.getTableBounds(this, tableName);
    }

    public QueryResult getTableRecordsMapIn( String tableName, Envelope envelope, boolean alsoPK_UID, int limit,
            int reprojectSrid ) throws Exception {
        return SpatialiteCommonMethods.getTableRecordsMapIn(this, tableName, envelope, alsoPK_UID, limit, reprojectSrid);
    }

    @Override
    protected void logWarn( String message ) {
        logger.warn(message);
    }

    @Override
    protected void logInfo( String message ) {
        logger.info(message);
    }

    @Override
    protected void logDebug( String message ) {
        logger.debug(message);
    }

    @Override
    public Geometry getGeometryFromResultSet( IJGTResultSet resultSet, int position ) throws Exception {
        byte[] geomBytes = resultSet.getBytes(position);
        Geometry geometry = wkbReader.read(geomBytes);
        return geometry;
    }

    public HashMap<String, List<String>> getTablesMap( boolean doOrder ) throws Exception {
        List<String> tableNames = getTables(doOrder);
        HashMap<String, List<String>> tablesMap = SpatialiteTableNames.getTablesSorted(tableNames, doOrder);
        return tablesMap;
    }

    public String getSpatialindexBBoxWherePiece( String tableName, String alias, double x1, double y1, double x2, double y2 )
            throws Exception {
        return SpatialiteCommonMethods.getSpatialindexBBoxWherePiece(this, tableName, alias, x1, y1, x2, y2);
    }

    public String getSpatialindexGeometryWherePiece( String tableName, String alias, Geometry geometry ) throws Exception {
        return SpatialiteCommonMethods.getSpatialindexGeometryWherePiece(this, tableName, alias, geometry);
    }

    public GeometryColumn getGeometryColumnsForTable( String tableName ) throws Exception {
        return SpatialiteCommonMethods.getGeometryColumnsForTable(mConn, tableName);
    }

    @Override
    public List<String> getTables( boolean doOrder ) throws Exception {
        return sqliteDb.getTables(doOrder);
    }

    @Override
    public boolean hasTable( String tableName ) throws Exception {
        return sqliteDb.hasTable(tableName);
    }

    @Override
    public List<String[]> getTableColumns( String tableName ) throws Exception {
        return sqliteDb.getTableColumns(tableName);
    }

    @Override
    public List<ForeignKey> getForeignKeys( String tableName ) throws Exception {
        return sqliteDb.getForeignKeys(tableName);
    }

    public List<Geometry> getGeometriesIn( String tableName, Envelope envelope ) throws Exception {
        return SpatialiteCommonMethods.getGeometriesIn(this, tableName, envelope);
    }

    public List<Geometry> getGeometriesIn( String tableName, Geometry intersectionGeometry ) throws Exception {
        return SpatialiteCommonMethods.getGeometriesIn(this, tableName, intersectionGeometry);
    }

    public String getGeojsonIn( String tableName, String[] fields, String wherePiece, Integer precision ) throws Exception {
        return SpatialiteCommonMethods.getGeojsonIn(this, tableName, fields, wherePiece, precision);
    }

    /**
     * Get the list of available raster coverages.
     * 
     * @param doOrder
     *            if <code>true</code>, the names are ordered.
     * @return the list of raster coverages.
     * @throws Exception
     */
    public List<RasterCoverage> getRasterCoverages( boolean doOrder ) throws Exception {
        List<RasterCoverage> rasterCoverages = new ArrayList<RasterCoverage>();
        String orderBy = " ORDER BY name";
        if (!doOrder) {
            orderBy = "";
        }

        String sql = "SELECT " + RasterCoverage.COVERAGE_NAME + ", " + RasterCoverage.TITLE + ", " + RasterCoverage.SRID + ", "
                + RasterCoverage.COMPRESSION + ", " + RasterCoverage.EXTENT_MINX + ", " + RasterCoverage.EXTENT_MINY + ", "
                + RasterCoverage.EXTENT_MAXX + ", " + RasterCoverage.EXTENT_MAXY + " FROM " + RasterCoverage.TABLENAME + orderBy;
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                RasterCoverage rc = new RasterCoverage();
                int i = 1;
                rc.coverage_name = rs.getString(i++);
                rc.title = rs.getString(i++);
                rc.srid = rs.getInt(i++);
                rc.compression = rs.getString(i++);
                rc.extent_minx = rs.getDouble(i++);
                rc.extent_miny = rs.getDouble(i++);
                rc.extent_maxx = rs.getDouble(i++);
                rc.extent_maxy = rs.getDouble(i++);
                rasterCoverages.add(rc);
            }
            return rasterCoverages;
        }
    }

    /**
     * Delete a geo-table with all attached indexes and stuff.
     * 
     * @param tableName
     * @throws Exception
     */
    public void deleteGeoTable( String tableName ) throws Exception {
        String sql = "SELECT DropGeoTable('" + tableName + "');";

        try (IJGTStatement stmt = mConn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void addGeometryXYColumnAndIndex( String tableName, String geomColName, String geomType, String epsg,
            boolean avoidIndex ) throws Exception {
        SpatialiteCommonMethods.addGeometryXYColumnAndIndex(this, tableName, geomColName, geomType, epsg, avoidIndex);
    }

    public void addGeometryXYColumnAndIndex( String tableName, String geomColName, String geomType, String epsg )
            throws Exception {
        addGeometryXYColumnAndIndex(tableName, geomColName, geomType, epsg, false);
    }

    public QueryResult getTableRecordsMapFromRawSql( String sql, int limit ) throws Exception {
        QueryResult queryResult = new QueryResult();
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            IJGTResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            int geometryIndex = -1;
            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = rsmd.getColumnName(i);
                queryResult.names.add(columnName);
                String columnTypeName = rsmd.getColumnTypeName(i);
                queryResult.types.add(columnTypeName);
                if (ESpatialiteGeometryType.isGeometryName(columnTypeName)) {
                    geometryIndex = i;
                    queryResult.geometryIndex = i - 1;
                }
            }
            int count = 0;
            SpatialiteWKBReader wkbReader = new SpatialiteWKBReader();
            while( rs.next() ) {
                Object[] rec = new Object[columnCount];
                for( int j = 1; j <= columnCount; j++ ) {
                    if (j == geometryIndex) {
                        byte[] geomBytes = rs.getBytes(j);
                        Geometry geometry = wkbReader.read(geomBytes);
                        rec[j - 1] = geometry;
                    } else {
                        Object object = rs.getObject(j);
                        if (object instanceof Clob) {
                            object = rs.getString(j);
                        }
                        rec[j - 1] = object;
                    }
                }
                queryResult.data.add(rec);
                if (limit > 0 && ++count > (limit - 1)) {
                    break;
                }
            }
            return queryResult;
        }
    }

    /**
     * Execute a query from raw sql and put the result in a csv file.
     * 
     * @param sql
     *            the sql to run.
     * @param csvFile
     *            the output file.
     * @param doHeader
     *            if <code>true</code>, the header is written.
     * @param separator
     *            the separator (if null, ";" is used).
     * @throws Exception
     */
    public void runRawSqlToCsv( String sql, File csvFile, boolean doHeader, String separator ) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile))) {
            SpatialiteWKBReader wkbReader = new SpatialiteWKBReader();
            try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
                IJGTResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                int geometryIndex = -1;
                for( int i = 1; i <= columnCount; i++ ) {
                    if (i > 1) {
                        bw.write(separator);
                    }
                    String columnTypeName = rsmd.getColumnTypeName(i);
                    String columnName = rsmd.getColumnName(i);
                    bw.write(columnName);
                    if (ESpatialiteGeometryType.isGeometryName(columnTypeName)) {
                        geometryIndex = i;
                    }
                }
                bw.write("\n");
                while( rs.next() ) {
                    for( int j = 1; j <= columnCount; j++ ) {
                        if (j > 1) {
                            bw.write(separator);
                        }
                        byte[] geomBytes = null;
                        if (j == geometryIndex) {
                            geomBytes = rs.getBytes(j);
                        }
                        if (geomBytes != null) {
                            try {
                                Geometry geometry = wkbReader.read(geomBytes);
                                bw.write(geometry.toText());
                            } catch (Exception e) {
                                // write it as it comes
                                Object object = rs.getObject(j);
                                if (object instanceof Clob) {
                                    object = rs.getString(j);
                                }
                                if (object != null) {
                                    bw.write(object.toString());
                                } else {
                                    bw.write("");
                                }
                            }
                        } else {
                            Object object = rs.getObject(j);
                            if (object instanceof Clob) {
                                object = rs.getString(j);
                            }
                            if (object != null) {
                                bw.write(object.toString());
                            } else {
                                bw.write("");
                            }
                        }
                    }
                    bw.write("\n");
                }
            }
        }
    }

}
