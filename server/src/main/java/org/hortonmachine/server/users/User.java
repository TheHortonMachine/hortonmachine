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
 * A user of the server.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class User {
    public final String id;
    /** The name to log in with, in lower case. */
    public final String username;
    public final Role role;
    /** The creation time, in milliseconds since the epoch. */
    public final long created;
    /** If the user can't log in. */
    public final boolean disabled;

    public User( String id, String username, Role role, long created, boolean disabled ) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.created = created;
        this.disabled = disabled;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
