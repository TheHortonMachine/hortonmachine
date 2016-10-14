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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A threadsafe spatialite database in which writing is blocking.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialiteThreadsafeDb extends SpatialiteDb {

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    /**
     * Getter for the write lock.
     * 
     * @return the write lock.
     */
    public Lock getWriteLock() {
        return writeLock;
    }

    /**
     * Getter for the read lock.
     * 
     * @return the read lock.
     */
    public Lock getReadLock() {
        return readLock;
    }

    public void deleteGeoTable( String tableName ) throws Exception {
        try {
            writeLock.lock();
            super.deleteGeoTable(tableName);
        } finally {
            writeLock.unlock();
        }
    }

    public void addGeometryXYColumnAndIndex( String tableName, String geomColName, String geomType, String epsg )
            throws Exception {
        try {
            writeLock.lock();
            super.addGeometryXYColumnAndIndex(tableName, geomColName, geomType, epsg);
        } finally {
            writeLock.unlock();
        }
    }

    public void insertGeometry( String tableName, Geometry geometry, String epsg ) throws Exception {
        try {
            writeLock.lock();
            super.insertGeometry(tableName, geometry, epsg);
        } finally {
            writeLock.unlock();
        }
    }

    public void createTable( String tableName, String... fieldData ) throws Exception {
        try {
            writeLock.lock();
            super.createTable(tableName, fieldData);
        } finally {
            writeLock.unlock();
        }
    }

    public void createIndex( String tableName, String column, boolean isUnique ) throws Exception {
        try {
            writeLock.lock();
            super.createIndex(tableName, column, isUnique);
        } finally {
            writeLock.unlock();
        }
    }

    public int executeInsertUpdateDeleteSql( String sql ) throws Exception {
        try {
            writeLock.lock();
            return super.executeInsertUpdateDeleteSql(sql);
        } finally {
            writeLock.unlock();
        }
    }
}
