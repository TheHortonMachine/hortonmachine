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
package org.hortonmachine.gears.assistant;

/**
 * The options of a request to an {@link ISqlAssistant}.
 *
 * @param includeSampleRows <code>true</code> to also give the model a few rows of each
 *          table. This sends data to the server, so it is off unless the user asks for it.
 * @param autoLimit the limit added to queries without one, or 0 for none.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record SqlAssistOptions( boolean includeSampleRows, int autoLimit ) {

    /**
     * The limit added to queries without one by {@link #defaults()}.
     */
    public static final int DEFAULT_AUTO_LIMIT = 100;

    /**
     * @return no sample rows and the {@link #DEFAULT_AUTO_LIMIT}.
     */
    public static SqlAssistOptions defaults() {
        return new SqlAssistOptions(false, DEFAULT_AUTO_LIMIT);
    }
}
