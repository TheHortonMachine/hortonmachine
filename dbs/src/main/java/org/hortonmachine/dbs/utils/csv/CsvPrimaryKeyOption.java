/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.dbs.utils.csv;

/**
 * How {@link CsvTableImporter#createTableFromCsv(org.hortonmachine.dbs.compat.ADb,
 * org.hortonmachine.dbs.utils.SqlName, java.io.File, char, java.util.List,
 * CsvPrimaryKeyOption)} should set up the new table's primary key.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class CsvPrimaryKeyOption {
    public enum Mode {
        /** No primary key. */
        NONE,
        /** One of the CSV's own columns is the primary key (not auto-generated). */
        EXISTING_COLUMN,
        /** A new, auto-incrementing column is added and used as the primary key. */
        GENERATED
    }

    public final Mode mode;
    /** Set only for {@link Mode#EXISTING_COLUMN}: the CSV column name to use as primary key. */
    public final String existingColumnName;
    /** Set only for {@link Mode#GENERATED}: the name of the new auto-increment column. */
    public final String generatedColumnName;

    private CsvPrimaryKeyOption( Mode mode, String existingColumnName, String generatedColumnName ) {
        this.mode = mode;
        this.existingColumnName = existingColumnName;
        this.generatedColumnName = generatedColumnName;
    }

    public static CsvPrimaryKeyOption none() {
        return new CsvPrimaryKeyOption(Mode.NONE, null, null);
    }

    public static CsvPrimaryKeyOption existingColumn( String columnName ) {
        return new CsvPrimaryKeyOption(Mode.EXISTING_COLUMN, columnName, null);
    }

    public static CsvPrimaryKeyOption generated( String columnName ) {
        return new CsvPrimaryKeyOption(Mode.GENERATED, null, columnName);
    }
}
