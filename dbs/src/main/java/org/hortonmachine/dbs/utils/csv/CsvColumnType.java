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

import org.hortonmachine.dbs.compat.ADatabaseSyntaxHelper;

/**
 * The types a CSV column can be interpreted as.
 *
 * <p>
 * {@link #DATE} isn't a distinct SQL column type: a DATE-typed CSV column is
 * parsed with its {@link CsvColumnSchema#datePattern} and stored as epoch
 * milliseconds in a {@link #LONG} column, the same convention used elsewhere
 * for timeseries tables (see {@code DbTimeseriesIterator}) - this sidesteps
 * native DATE/TIMESTAMP type differences across database engines.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public enum CsvColumnType {
    TEXT, INTEGER, LONG, DOUBLE, DATE;

    /**
     * @return the engine-specific SQL type to use for a column of this type, via
     *          the given database's {@link ADatabaseSyntaxHelper}.
     */
    public String toSqlType( ADatabaseSyntaxHelper syntaxHelper ) {
        switch( this ) {
        case TEXT:
            return syntaxHelper.TEXT();
        case INTEGER:
            return syntaxHelper.INTEGER();
        case LONG:
        case DATE: // stored as epoch millis
            return syntaxHelper.LONG();
        case DOUBLE:
            return syntaxHelper.REAL();
        default:
            throw new IllegalStateException("Unhandled type: " + this);
        }
    }
}
