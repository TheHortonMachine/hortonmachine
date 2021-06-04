package org.hortonmachine.dbs.geopackage.android;

import java.io.IOException;

import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.geopackage.geom.GeoPkgGeomReader;
import org.hortonmachine.dbs.spatialite.android.GPSpatialiteDb;

import jsqlite.Database;

public class GPGeopackageDb extends GeopackageCommonDb {

    public GPGeopackageDb() {
        sqliteDb = new GPSpatialiteDb();
    }

    // TODO, make this for android
    public void createFunctions() throws Exception {
        if (!(sqliteDb instanceof GPSpatialiteDb)) {

        }
        GPSpatialiteDb gpDb = (GPSpatialiteDb) sqliteDb;
        Database database = gpDb.getDatabase();

        database.create_function("ST_MinX", 1, new GPGeometryFunction(){
            @Override
            public Object execute( GeoPkgGeomReader reader ) throws Exception {
                return reader.getEnvelope().getMinX();
            }
        });
        database.create_function("ST_MaxX", 1, new GPGeometryFunction(){
            @Override
            public Object execute( GeoPkgGeomReader reader ) throws Exception {
                return reader.getEnvelope().getMaxX();
            }
        });
        database.create_function("ST_MinY", 1, new GPGeometryFunction(){
            @Override
            public Object execute( GeoPkgGeomReader reader ) throws Exception {
                return reader.getEnvelope().getMinY();
            }
        });
        database.create_function("ST_MaxY", 1, new GPGeometryFunction(){
            @Override
            public Object execute( GeoPkgGeomReader reader ) throws Exception {
                return reader.getEnvelope().getMaxY();
            }
        });

        database.create_function("ST_IsEmpty", 1, new GPGeometryFunction(){
            @Override
            public Object execute( GeoPkgGeomReader reader ) throws Exception {
                return reader.getHeader().getFlags().isEmpty();
            }
        });

    }

    @Override
    public String getSldString( String tableName ) throws Exception {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void updateSldStyle( String tableName, String sldString ) throws Exception {
        throw new RuntimeException("Not supported");
    }

    @Override
    public String getFormString( String tableName ) throws Exception {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void updateForm( String tableName, String form ) throws Exception {
        throw new RuntimeException("Not supported");
    }
}
