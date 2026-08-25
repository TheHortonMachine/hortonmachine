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
 * One column of a CSV file's guessed (and possibly user-edited) schema: its
 * header name, its {@link CsvColumnType}, and, only meaningful for
 * {@link CsvColumnType#DATE}, the pattern used to parse it (a
 * {@link java.text.SimpleDateFormat} pattern, e.g. {@code "yyyy-MM-dd
 * HH:mm:ss"}).
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class CsvColumnSchema {
    public final String name;
    public CsvColumnType type;
    public String datePattern;

    public CsvColumnSchema( String name, CsvColumnType type ) {
        this(name, type, null);
    }

    public CsvColumnSchema( String name, CsvColumnType type, String datePattern ) {
        this.name = name;
        this.type = type;
        this.datePattern = datePattern;
    }
}
