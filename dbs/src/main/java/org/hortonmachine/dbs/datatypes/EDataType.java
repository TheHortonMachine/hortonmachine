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
package org.hortonmachine.dbs.datatypes;

import org.locationtech.jts.geom.Geometry;

/**
 * Common Data Types.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public enum EDataType {
    TEXT(0, String.class, "''", new String[]{"TEXT", "VARCHAR", "CLOB"}), //
    DOUBLE(1, Double.class, "-1.0", new String[]{"DOUBLE", "REAL", "NUMERIC"}), //
    PHONE(2, String.class, "''", new String[]{"PHONE"}), //
    DATE(3, String.class, "''", new String[]{"DATE"}), //
    INTEGER(4, Integer.class, "0", new String[]{"INTEGER", "INT", "TINYINT", "MEDIUMINT", "SMALLINT", "BOOLEAN"}), //
    LONG(5, Long.class, "0", new String[]{"LONG", "BIGINT"}), //
    FLOAT(6, Float.class, "0.0", new String[]{"FLOAT"}), //
    BLOB(7, Object.class, "''", new String[]{"BLOB"}), //
    BOOLEAN(8, Boolean.class, "''", new String[]{"BOOLEAN"}), //
    DATETIME(9, String.class, "''", new String[]{"DATETIME"}), //
    GEOMETRY(999, Geometry.class, "''", new String[]{"GEOMETRY"});

    private int code;
    private Class< ? > clazz;
    private String defaultValueForSql;
    private String[] possibleTypeNames;

    private EDataType( int code, Class< ? > clazz, String defaultValueForSql, String[] possibleTypeNames ) {
        this.code = code;
        this.clazz = clazz;
        this.defaultValueForSql = defaultValueForSql;
        this.possibleTypeNames = possibleTypeNames;
    }

    /**
     * @return the code of this type.
     */
    public int getCode() {
        return code;
    }

    /**
     * @return the class for the type.
     */
    public Class< ? > getClazz() {
        return clazz;
    }

    /**
     * @return the default value for an sql query.
     */
    public String getDefaultValueForSql() {
        return defaultValueForSql;
    }

//    /**
//     * Get the type from the code.
//     *
//     * @param code the code.
//     * @return the {@link EDataType}.
//     */
//    public static EDataType getType4Code(int code) {
//        EDataType[] values = values();
//        for (EDataType dataType : values) {
//            if (dataType.getCode() == code) {
//                return dataType;
//            }
//        }
//        throw new IllegalArgumentException("Unknown datatype for code: " + code); //$NON-NLS-1$
//    }

    /**
     * Get the type from the name.
     *
     * @param name the name.
     * @return the {@link EDataType}.
     */
    public static EDataType getType4Name( String name ) {
        name = name.toUpperCase();
        EDataType[] values = values();
        for( EDataType dataType : values ) {
            for( String possibleName : dataType.possibleTypeNames ) {
                if (name.startsWith(possibleName)) {
                    return dataType;
                }
            }
        }

        EGeometryType type = EGeometryType.forTypeName(name);
        if (type != EGeometryType.UNKNOWN)
            return GEOMETRY;
        throw new IllegalArgumentException("Unknown datatype for name: " + name); //$NON-NLS-1$
    }

    /**
     * Get the datatype from a given sqlite code.
     * <p/>
     * <p>The codes are the ones defined in jsqlite.Constants.<br>
     * Currently supported are:<br>
     * <pre>
     * SQLITE_INTEGER = 1;
     * SQLITE_FLOAT = 2;
     * SQLITE_BLOB = 4;
     * SQLITE3_TEXT = 3;
     * SQLITE_NUMERIC = -1;
     * SQLITE_TEXT = 3;
     * SQLITE2_TEXT = -2;
     * </pre>
     *
     * @param sqliteCode the code.
     * @return the {@link EDataType}.
     */
    public static EDataType getType4SqliteCode( int sqliteCode ) {

        switch( sqliteCode ) {
        case 1:
            return INTEGER;
        case 2:
            return FLOAT;
        case 4:
            return BLOB;
        case 3:
        case -2:
            return TEXT;
        case -1:
            return DOUBLE;
        }
        return null;
    }

}
