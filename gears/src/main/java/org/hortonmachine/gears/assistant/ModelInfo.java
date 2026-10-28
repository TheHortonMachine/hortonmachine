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
 * A model available on the server of an {@link IAssistantBackend}.
 *
 * @param name the name used to ask for the model.
 * @param sizeBytes the size of the model, or -1 if the server doesn't tell.
 * @param details a short description (e.g. parameters and quantization), or <code>null</code>.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record ModelInfo( String name, long sizeBytes, String details ) {
}
