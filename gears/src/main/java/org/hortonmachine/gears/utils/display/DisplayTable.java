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
package org.hortonmachine.gears.utils.display;

import java.util.List;

/**
 * A table output.
 *
 * <p>The rows can be only the first part of the data, in which case
 * <code>totalRows</code> is bigger than the number of rows.</p>
 *
 * @param header the column names.
 * @param rows the rows, each one with as many values as the header.
 * @param totalRows the total number of rows of the data.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record DisplayTable( List<String> header, List<List<String>> rows, long totalRows ) implements DisplayPart {

    /**
     * @return <code>true</code> if only the first part of the data is in the rows.
     */
    public boolean isTruncated() {
        return totalRows > rows.size();
    }
}
