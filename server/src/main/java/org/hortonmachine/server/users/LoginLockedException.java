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
 * Too many logins of a user name failed recently.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class LoginLockedException extends Exception {
    private static final long serialVersionUID = 1L;

    private final long minutes;

    /**
     * @param minutes the minutes until the next login can be tried.
     */
    public LoginLockedException( long minutes ) {
        super("Too many failed logins: try again in " + minutes + (minutes == 1 ? " minute." : " minutes."));
        this.minutes = minutes;
    }

    public long getMinutes() {
        return minutes;
    }
}
