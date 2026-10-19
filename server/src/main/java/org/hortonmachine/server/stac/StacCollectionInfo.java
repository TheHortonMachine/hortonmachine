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
package org.hortonmachine.server.stac;

import java.util.List;

/**
 * The metadata of a collection of the STAC catalog given by the administrators; without it the collection is described
 * by the name of its folder.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class StacCollectionInfo {
    /** The id of the collection: the name of its folder. */
    public final String id;
    /** The fields, <code>null</code> or empty when not given. */
    public final String title;
    public final String description;
    public final List<String> keywords;
    /** The license, as an SPDX identifier, or "other". */
    public final String license;

    public StacCollectionInfo( String id, String title, String description, List<String> keywords, String license ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.keywords = keywords != null ? List.copyOf(keywords) : List.of();
        this.license = license;
    }
}
