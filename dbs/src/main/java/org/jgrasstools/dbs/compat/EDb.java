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
package org.jgrasstools.dbs.compat;

/**
 * The available databases.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum EDb {
    SQLITE(0, ".sqlite", "sqlite", "org.jgrasstools.dbs.spatialite.jgt.SqliteDb", false), //
    SPATIALITE(1, ".sqlite", "sqlite", "org.jgrasstools.dbs.spatialite.jgt.SpatialiteDb", true), //
    H2(2, "", "mv.db", "org.jgrasstools.dbs.h2gis.H2Db", false), //
    H2GIS(3, "", "mv.db", "org.jgrasstools.dbs.h2gis.H2GisDb", true);

    private int _code;
    private String _extensionOnCreation;
    private String _extension;
    private String _className;
    private boolean _isSpatial;

    private EDb( int code, String extensionOnCreation, String extension, String className, boolean isSpatial ) {
        this._code = code;
        this._extensionOnCreation = extensionOnCreation;
        this._extension = extension;
        this._className = className;
        this._isSpatial = isSpatial;
    }

    public int getCode() {
        return _code;
    }

    public static EDb forCode( int code ) {
        for( EDb dbType : values() ) {
            if (dbType.getCode() == code) {
                return dbType;
            }
        }
        throw new IllegalArgumentException("No db type for code: " + code);
    }

    public String getExtensionOnCreation() {
        return _extensionOnCreation;
    }

    public String getExtension() {
        return _extension;
    }

    public boolean isSpatial() {
        return _isSpatial;
    }

    /**
     * Get a new instance of the spatial database version.
     * 
     * @return the spatial instance if the db supports it.
     * @throws Exception
     */
    public ASpatialDb getSpatialDb() throws Exception {
        if (_isSpatial) {
            Class< ? > forName = Class.forName(_className);
            Object newInstance = forName.newInstance();
            if (newInstance instanceof ASpatialDb) {
                return (ASpatialDb) newInstance;
            }
        }
        throw new IllegalArgumentException("Database type is not spatial");
    }

    /**
     * Get a new instance of the database.
     * 
     * @return the database instance.
     * @throws Exception
     */
    public ADb getDb() throws Exception {
        Class< ? > forName = Class.forName(_className);
        Object newInstance = forName.newInstance();
        return (ADb) newInstance;
    }

}
