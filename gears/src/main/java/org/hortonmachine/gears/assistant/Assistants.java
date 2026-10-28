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

import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;

import org.hortonmachine.dbs.log.Logger;

/**
 * The access point to the optional assistant support.
 *
 * <pre>
 * Assistants.getBackend().flatMap(IAssistantBackend::getSqlAssistant).ifPresent(sqlAssistant -&gt; {
 *     // add the assistant to the UI
 * });
 * </pre>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class Assistants {

    private static Optional<IAssistantBackend> backend;

    private Assistants() {
    }

    /**
     * Get the assistant backend, looked up once through the {@link ServiceLoader}.
     *
     * <p>If more than one is on the classpath, the first one found is used.</p>
     *
     * @return the backend, or empty if no module providing one is installed.
     */
    public static synchronized Optional<IAssistantBackend> getBackend() {
        if (backend == null) {
            backend = Optional.empty();
            Iterator<IAssistantBackend> iterator = ServiceLoader.load(IAssistantBackend.class).iterator();
            while( iterator.hasNext() ) {
                try {
                    backend = Optional.of(iterator.next());
                    break;
                } catch (Throwable e) {
                    // a broken provider must not break the applications, which then run without assistant
                    Logger.INSTANCE.insertError("Assistants", "Error loading an assistant backend.", e);
                }
            }
        }
        return backend;
    }
}
