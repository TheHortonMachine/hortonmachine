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
 * The status of the server of an {@link IAssistantBackend}.
 *
 * @param reachable <code>true</code> if the server answered.
 * @param local <code>true</code> if the server runs on this machine.
 * @param version the version of the server, or <code>null</code> if unknown.
 * @param message a message for the user: the reason why the server can't be reached, or
 *          what to do next (e.g. how to install it, or that no model is available yet).
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record BackendStatus( boolean reachable, boolean local, String version, String message ) {
}
