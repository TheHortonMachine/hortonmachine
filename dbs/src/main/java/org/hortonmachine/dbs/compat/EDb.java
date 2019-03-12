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
package org.hortonmachine.dbs.compat;

import java.io.File;

import org.hortonmachine.dbs.h2gis.H2GisGeometryParser;
import org.hortonmachine.dbs.h2gis.H2SyntaxHelper;
import org.hortonmachine.dbs.postgis.PGSyntaxHelper;
import org.hortonmachine.dbs.postgis.PostgisGeometryParser;
import org.hortonmachine.dbs.spatialite.SpatialiteCommonMethods;
import org.hortonmachine.dbs.spatialite.SpatialiteGeometryParser;
import org.hortonmachine.dbs.spatialite.SpatialiteTableNames;
import org.hortonmachine.dbs.spatialite.SqliteNonSpatialDataType;

/**
 * The available databases.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum EDb {
    SQLITE(0, ".sqlite", "sqlite", "org.hortonmachine.dbs.spatialite.hm.SqliteDb", false,
            "org.hortonmachine.dbs.spatialite.SpatialiteSqlTemplates", "jdbc:sqlite:", false, false, false, true), //
    SPATIALITE(1, ".sqlite", "sqlite", "org.hortonmachine.dbs.spatialite.hm.SpatialiteThreadsafeDb", true,
            "org.hortonmachine.dbs.spatialite.SpatialiteSqlTemplates", "jdbc:sqlite:", false, false, false, true), //
    H2(2, "", "mv.db", "org.hortonmachine.dbs.h2gis.H2Db", false, "org.hortonmachine.dbs.h2gis.H2GisSqlTemplates", "jdbc:h2:",
            true, true, false, true), //
    H2GIS(3, "", "mv.db", "org.hortonmachine.dbs.h2gis.H2GisDb", true, "org.hortonmachine.dbs.h2gis.H2GisSqlTemplates",
            "jdbc:h2:", true, true, false, true), //
    SPATIALITE4ANDROID(4, ".sqlite", "sqlite", "org.hortonmachine.dbs.spatialite.android.GPSpatialiteDb", true,
            "org.hortonmachine.dbs.spatialite.SpatialiteSqlTemplates", "", false, false, true, false), //
    POSTGRES(5, "", "", "org.hortonmachine.dbs.postgis.PGDb", false, "org.hortonmachine.dbs.postgis.PGSqlTemplates",
            "jdbc:postgresql://", true, true, false, false), //
    POSTGIS(6, "", "", "org.hortonmachine.dbs.postgis.PostgisDb", true, "org.hortonmachine.dbs.postgis.PostgisSqlTemplates",
            "jdbc:postgresql://", true, true, false, false)

    ; //

    private int _code;
    private String _extensionOnCreation;
    private String _extension;
    private String _dbClassName;
    private boolean _isSpatial;
    private String _sqlTemplatesClassName;
    private String _jdbcPrefix;
    private boolean _supportsPwd;
    private boolean _supportsServerMode;
    private boolean _supportsMobile;
    private boolean _supportsDesktop;
    private ASqlTemplates sqlTemplates;

    /**
     * @param code db code.
     * @param extensionOnCreation extension to use when creating a new db.
     * @param extension extension to use when opening the db.
     * @param dbClassName class of the db.
     * @param isSpatial if <code>true</code>, the database supports spatial tables.
     * @param sqlTemplatesClassName class of db templates.
     * @param jdbcPrefix the jdbc prefic, if supported.
     * @param supportsPwd if the db allows for password.
     * @param supportsServerMode if the db supports server mode.
     * @param supportsMobile if the db can be run on mobile devices.
     * @param supportsDesktop if the db supports file based desktop mode (i.e. without server).
     */
    private EDb( int code, String extensionOnCreation, String extension, String dbClassName, boolean isSpatial,
            String sqlTemplatesClassName, String jdbcPrefix, boolean supportsPwd, boolean supportsServerMode,
            boolean supportsMobile, boolean supportsDesktop ) {
        this._code = code;
        this._extensionOnCreation = extensionOnCreation;
        this._extension = extension;
        this._dbClassName = dbClassName;
        this._isSpatial = isSpatial;
        this._sqlTemplatesClassName = sqlTemplatesClassName;
        this._jdbcPrefix = jdbcPrefix;
        this._supportsPwd = supportsPwd;
        this._supportsServerMode = supportsServerMode;
        this._supportsMobile = supportsMobile;
        this._supportsDesktop = supportsDesktop;
    }

    public static EDb[] getSpatialTypesDesktop() {
        return new EDb[]{SPATIALITE, H2GIS, POSTGIS};
    }

    public static EDb[] getSpatialTypesMobile() {
        return new EDb[]{SPATIALITE4ANDROID};
    }

    public int getCode() {
        return _code;
    }

    public String getJdbcPrefix() {
        return _jdbcPrefix;
    }

    public boolean supportsPwd() {
        return _supportsPwd;
    }

    public boolean supportsServerMode() {
        return _supportsServerMode;
    }

    public boolean supportsMobile() {
        return _supportsMobile;
    }

    public boolean supportsDesktop() {
        return _supportsDesktop;
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
            Class< ? > forName = Class.forName(_dbClassName);
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
        Class< ? > forName = Class.forName(_dbClassName);
        Object newInstance = forName.newInstance();
        return (ADb) newInstance;
    }

    /**
     * Get a new instance of the sql templates.
     * 
     * @return the sql templates instance.
     * @throws Exception
     */
    public ASqlTemplates getSqlTemplates() throws Exception {
        if (sqlTemplates == null) {
            Class< ? > forName = Class.forName(_sqlTemplatesClassName);
            Object newInstance = forName.newInstance();
            sqlTemplates = (ASqlTemplates) newInstance;
        }
        return sqlTemplates;
    }

    public IGeometryParser getGeometryParser() {
        switch( this ) {
        case H2GIS:
            return new H2GisGeometryParser();
        case POSTGIS:
            return new PostgisGeometryParser();
        case SPATIALITE:
        case SPATIALITE4ANDROID:
            return new SpatialiteGeometryParser();
        default:
            return null;
        }
    }

    /**
     * Guesses the database type from the extension.
     * 
     * @param name the db name to check.
     * @return the db type.
     */
    public static EDb fromFileNameDesktop( String name ) {
        EDb tmpNonspatial = null;
        for( EDb edb : values() ) {
            if (edb.supportsDesktop() && name.toLowerCase().endsWith(edb.getExtension())) {
                if (edb.isSpatial()) {
                    return edb;
                } else {
                    tmpNonspatial = edb;
                }
            }
        }
        return tmpNonspatial;
    }

    /**
     * Checks the db by connecting.
     * 
     * <p>Spatialite and sqlite are checked properly by magic number 
     * and tables presence. H2GIS by its extension.
     * 
     * @param file the db file to check.
     * @return the db type.
     * @throws Exception 
     */
    public static EDb fromFileDesktop( File file ) throws Exception {
        if (!file.exists()) {
            return fromFileNameDesktop(file.getName());
        }
        if (SpatialiteCommonMethods.isSqliteFile(file)) {
            // sqlite
            try (ASpatialDb db = SPATIALITE.getSpatialDb()) {
                db.open(file.getAbsolutePath());
                if (db.hasTable(SpatialiteTableNames.CHECK_SPATIALITE_TABLE)) {
                    return SPATIALITE;
                } else {
                    return SQLITE;
                }
            }
        }
        String name = file.getName();
        return fromFileNameDesktop(name);
    }

    public ADatabaseSyntaxHelper getDatabaseSyntaxHelper() {
        switch( this ) {
        case H2:
        case H2GIS:
            return new H2SyntaxHelper();
        case POSTGRES:
        case POSTGIS:
            return new PGSyntaxHelper();
        case SQLITE:
        case SPATIALITE:
        case SPATIALITE4ANDROID:
            return new SqliteNonSpatialDataType();
        default:
            return null;
        }
    }
}
