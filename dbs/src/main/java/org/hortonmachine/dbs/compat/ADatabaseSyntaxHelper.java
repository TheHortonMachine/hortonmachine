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

/**
 * Database Syntax Compatibility Helper class.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class ADatabaseSyntaxHelper {
    public static final String COMPAT_TEXT = "TEXT";
    public static final String COMPAT_INT = "INT";
    public static final String COMPAT_LONG = "LONG";
    public static final String COMPAT_REAL = "REAL";
    public static final String COMPAT_BLOB = "BLOB";
    public static final String COMPAT_CLOB = "CLOB";

    /**
     * @return the db specific type for a text type.
     */
    public abstract String TEXT();

    /**
     * @return the db specific type for an integer type.
     */
    public abstract String INTEGER();

    /**
     * @return the db specific type for a long type.
     */
    public abstract String LONG();

    /**
     * @return the db specific type for a double type.
     */
    public abstract String REAL();

    /**
     * @return the db specific type for a blob type.
     */
    public abstract String BLOB();

    /**
     * @return the db specific type for a clob type.
     */
    public abstract String CLOB();

    /**
     * Get the type by its compatibility name.
     * 
     * @param compatibilityDataTypeName the compat name, as for example {@link #COMPAT_TEXT};
     * @return the data type;
     */
    public String ofCompat( String compatibilityDataTypeName ) {
        switch( compatibilityDataTypeName ) {
        case COMPAT_TEXT:
            return TEXT();
        case COMPAT_INT:
            return INTEGER();
        case COMPAT_LONG:
            return LONG();
        case COMPAT_REAL:
            return REAL();
        case COMPAT_BLOB:
            return BLOB();
        case COMPAT_CLOB:
            return CLOB();
        }
        throw new RuntimeException("No type for name: " + compatibilityDataTypeName);
    }

    /**
     * Get the compatibility name by the db specific type.
     * 
     * @param dbSpecificType the db specific name, as for example DOUBLE;
     * @return the compat data type;
     */
    public String ofDbType( String dbSpecificType ) {
        if (dbSpecificType.equals(TEXT())) {
            return COMPAT_TEXT;
        } else if (dbSpecificType.equals(INTEGER())) {
            return COMPAT_INT;
        } else if (dbSpecificType.equals(LONG())) {
            return COMPAT_LONG;
        } else if (dbSpecificType.equals(REAL())) {
            return COMPAT_REAL;
        } else if (dbSpecificType.equals(BLOB())) {
            return COMPAT_BLOB;
        } else if (dbSpecificType.equals(CLOB())) {
            return COMPAT_CLOB;
        }
        throw new RuntimeException("No type for name: " + dbSpecificType);
    }

    public abstract String PRIMARYKEY();

    /**
     * @return the string used to define the primary key as long and with autoincrement.
     */
    public abstract String LONG_PRIMARYKEY_AUTOINCREMENT();

    public abstract String AUTOINCREMENT();

    public abstract String MAKEPOINT2D();
    
    /**
     * Check for compatibility issues with different databases.
     * 
     * @param sql the original sql.
     * @return the fixed sql.
     */
    public abstract String checkSqlCompatibilityIssues( String sql );
}