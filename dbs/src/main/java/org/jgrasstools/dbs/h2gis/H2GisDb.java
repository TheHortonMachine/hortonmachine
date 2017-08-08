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
package org.jgrasstools.dbs.h2gis;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;

import org.h2gis.ext.H2GISExtension;
import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.compat.GeometryColumn;
import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTStatement;
import org.jgrasstools.dbs.compat.objects.ForeignKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * A spatialite database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class H2GisDb extends ASpatialDb {
    private static final Logger logger = LoggerFactory.getLogger(H2GisDb.class);
    private String user = "sa";
    private String password = "";
    private Connection jdbcConn;
    private H2Db h2Db;
    
    public H2GisDb() {
     h2Db = new H2Db();
    }

    public void setCredentials( String user, String password ) {
        this.user = user;
        this.password = password;
    }

    public boolean open( String dbPath ) throws Exception {
        h2Db.setCredentials(user, password);
        boolean dbExists = h2Db.open(dbPath);
        
        this.mDbPath = h2Db.getDatabasePath();
        mConn = h2Db.getConnection();
        if (mPrintInfos) {
            String[] dbInfo = getDbInfo();
            logger.info("H2 Version: " + dbInfo[0]);
            logger.info("H2GIS Version: " + dbInfo[1]);
        }
        return dbExists;
    }

    @Override
    public void initSpatialMetadata( String options ) throws Exception {
        H2GISExtension.load(jdbcConn);
    }

    @Override
    public Envelope getTableBounds( String tableName ) throws Exception {
        GeometryColumn gCol = getGeometryColumnsForTable(tableName);
        if (gCol == null)
            return null;
        String geomFieldName = gCol.geometryColumnName;
        // String geomFieldName;
        // if (gCol != null) {
        // geomFieldName = gCol.f_geometry_column;
        // String trySql = "SELECT extent_min_x, extent_min_y, extent_max_x, extent_max_y FROM
        // vector_layers_statistics WHERE table_name='"
        // + tableName + "' AND geometry_column='" + geomFieldName + "'";
        // try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs =
        // stmt.executeQuery(trySql)) {
        // if (rs.next()) {
        // double minX = rs.getDouble(1);
        // double minY = rs.getDouble(2);
        // double maxX = rs.getDouble(3);
        // double maxY = rs.getDouble(4);
        //
        // Envelope env = new Envelope(minX, maxX, minY, maxY);
        // if (env.getWidth() != 0.0 && env.getHeight() != 0.0) {
        // return env;
        // }
        // }
        // }
        // } else {
        // // try geometry if virtual table
        // geomFieldName = "geometry";
        // }

        // OR DO FULL GEOMETRIES SCAN

        String sql = "SELECT ST_XMin(" + geomFieldName + ") , ST_YMin(" + geomFieldName + ")," + "ST_XMax(" + geomFieldName
                + "), ST_YMax(" + geomFieldName + ") " + "FROM " + tableName;

        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                double minX = rs.getDouble(1);
                double minY = rs.getDouble(2);
                double maxX = rs.getDouble(3);
                double maxY = rs.getDouble(4);

                Envelope env = new Envelope(minX, maxX, minY, maxY);
                return env;
            }
            return null;
        }
    }

    public String[] getDbInfo() throws Exception {
        // checking h2 version
        String sql = "SELECT H2VERSION(), H2GISVERSION();";
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
    public GeometryColumn getGeometryColumnsForTable( String tableName ) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSpatialindexGeometryWherePiece( String tableName, String alias, Geometry geometry ) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSpatialindexBBoxWherePiece( String tableName, String alias, double x1, double y1, double x2, double y2 )
            throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public static void main( String[] args ) throws Exception {
        try (H2GisDb db = new H2GisDb()) {
            // db.setCredentials("asd", "asd");
            boolean existed = db.open("/home/hydrologis/TMP/H2GIS/h2_test1");
            if (!existed)
                db.initSpatialMetadata(null);

            db.createTable("ROADS", "the_geom MULTILINESTRING", "speed_limit INT");
            db.createIndex(PK_UID, PKUID, existed);
            
        }
    }

    @Override
    public List<String> getTables( boolean doOrder ) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasTable( String tableName ) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<String[]> getTableColumns( String tableName ) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ForeignKey> getForeignKeys( String tableName ) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HashMap<String, List<String>> getTablesMap( boolean doOrder ) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
