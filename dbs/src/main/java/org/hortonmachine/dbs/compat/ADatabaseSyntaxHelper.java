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
    public String of( String compatibilityDataTypeName ) {
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

    public abstract String PRIMARYKEY();

    /**
     * @return the string used to define the primary key as long and with autoincrement.
     */
    public String LONG_PRIMARYKEY_AUTOINCREMENT() {
        return LONG() + " " + PRIMARYKEY() + " " + AUTOINCREMENT();
    }

    public abstract String AUTOINCREMENT();

    public abstract String MAKEPOINT2D();
}