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
package org.hortonmachine.server.users;

/**
 * A token of the api of a user. Its secret is shown once, when created, and only its hash is kept.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class ApiToken {
    public final String id;
    public final String userId;
    /** The name given by the user, as "laptop" or "qgis". */
    public final String name;
    public final long created;
    /** The time of the last request with the token, or <code>null</code> if never used. */
    public final Long lastUsed;

    public ApiToken( String id, String userId, String name, long created, Long lastUsed ) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.created = created;
        this.lastUsed = lastUsed;
    }
}
