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
package org.hortonmachine.dbs.spatialite.hm;

import java.util.concurrent.locks.ReentrantReadWriteLock;


import org.locationtech.jts.geom.Geometry;

/**
 * A threadsafe spatialite database in which writing is blocking.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialiteThreadsafeDb extends SpatialiteDb {
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    public void lockWrite( String tag ) {
        // logger.debug("Locking: " + tag);
        rwLock.writeLock().lock();
    }

    public void unlockWrite( String tag ) {
        rwLock.writeLock().unlock();
        // logger.debug("Unlocked: " + tag);
    }

    public void deleteGeoTable( String tableName ) throws Exception {
        try {
            lockWrite("deleteGeoTable");
            super.deleteGeoTable(tableName);
        } finally {
            unlockWrite("deleteGeoTable");
        }
    }

    public void addGeometryXYColumnAndIndex( String tableName, String geomColName, String geomType, String epsg, boolean avoidIndex )
            throws Exception {
        try {
            lockWrite("addGeometryXYColumnAndIndex");
            super.addGeometryXYColumnAndIndex(tableName, geomColName, geomType, epsg, avoidIndex);
        } finally {
            unlockWrite("addGeometryXYColumnAndIndex");
        }
    }

    public void addGeometryXYColumnAndIndex( String tableName, String geomColName, String geomType, String epsg)
            throws Exception {
        try {
            lockWrite("addGeometryXYColumnAndIndex");
            super.addGeometryXYColumnAndIndex(tableName, geomColName, geomType, epsg);
        } finally {
            unlockWrite("addGeometryXYColumnAndIndex");
        }
    }

    public void insertGeometry( String tableName, Geometry geometry, String epsg, String where ) throws Exception {
        try {
            lockWrite("insertGeometry");
            super.insertGeometry(tableName, geometry, epsg, where);
        } finally {
            unlockWrite("insertGeometry");
        }
    }

    public void createTable( String tableName, String... fieldData ) throws Exception {
        long cm = System.currentTimeMillis();
        try {
            lockWrite("start createTable " + cm + ":" + tableName);
            super.createTable(tableName, fieldData);
        } finally {
            unlockWrite("end createTable " + cm + ":" + tableName);
        }
    }

    public void createIndex( String tableName, String column, boolean isUnique ) throws Exception {
        long cm = System.currentTimeMillis();
        try {
            lockWrite("start createIndex " + cm + ":" + tableName + "," + column);
            super.createIndex(tableName, column, isUnique);
        } finally {
            unlockWrite("end createIndex " + cm + ":" + tableName + "," + column);
        }
    }

    public int executeInsertUpdateDeleteSql( String sql ) throws Exception {
        long cm = System.currentTimeMillis();
        try {
            lockWrite("start executeInsertUpdateDeleteSql " + cm + ":" + sql);
            return super.executeInsertUpdateDeleteSql(sql);
        } finally {
            unlockWrite("end executeInsertUpdateDeleteSql " + cm + ":" + sql);
        }
    }
}
