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
package org.hortonmachine.dbs.geopackage.hm;

import java.sql.Connection;

import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.geopackage.geom.GeoPkgGeomReader;
import org.hortonmachine.dbs.spatialite.hm.SqliteDb;
import org.sqlite.Function;

/**
 * A spatialite database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeopackageDb extends GeopackageCommonDb {
    public GeopackageDb() {
        sqliteDb = new SqliteDb();
    }

    public void createFunctions() throws Exception {
        Connection cx = sqliteDb.getJdbcConnection();
        // minx
        Function.create(cx, "ST_MinX", new GeometryFunction(){
            @Override
            public Object execute( GeoPkgGeomReader reader ) throws Exception {
                return reader.getEnvelope().getMinX();
            }
        });

        // maxx
        Function.create(cx, "ST_MaxX", new GeometryFunction(){
            @Override
            public Object execute( GeoPkgGeomReader reader ) throws Exception {
                return reader.getEnvelope().getMaxX();
            }
        });

        // miny
        Function.create(cx, "ST_MinY", new GeometryFunction(){
            @Override
            public Object execute( GeoPkgGeomReader reader ) throws Exception {
                return reader.getEnvelope().getMinY();
            }
        });

        // maxy
        Function.create(cx, "ST_MaxY", new GeometryFunction(){
            @Override
            public Object execute( GeoPkgGeomReader reader ) throws Exception {
                return reader.getEnvelope().getMaxY();
            }
        });

        // empty
        Function.create(cx, "ST_IsEmpty", new GeometryFunction(){
            @Override
            public Object execute( GeoPkgGeomReader reader ) throws Exception {
                return reader.getHeader().getFlags().isEmpty();
            }
        });
    }

    @Override
    public String getSldString( String tableName ) throws Exception {
        return getSldStringInternal(sqliteDb, tableName);
    }

    @Override
    public void updateSldStyle( String tableName, String sldString ) throws Exception {
        updateSldStyleInternal(sqliteDb, tableName, sldString);
    }

    @Override
    public String getFormString( String tableName ) throws Exception {
        return getFormStringInternal(sqliteDb, tableName);
    }

    @Override
    public void updateForm( String tableName, String form ) throws Exception {
        updateFormsInternal(sqliteDb, tableName, form);
    }
}
