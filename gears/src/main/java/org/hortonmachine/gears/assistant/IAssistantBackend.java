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

import java.util.List;
import java.util.Optional;

import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

/**
 * A language model backend that provides the assistants (text to SQL, scripts, ...).
 *
 * <p>This is the extension point of the optional assistant support: an implementation is
 * found through the {@link java.util.ServiceLoader}, listing it in
 * <code>META-INF/services/org.hortonmachine.gears.assistant.IAssistantBackend</code>. When
 * no implementation is on the classpath, {@link Assistants#getBackend()} is empty and the
 * applications show no assistant at all, so they must only ever use these contracts and
 * never the classes of the implementing module.</p>
 *
 * <p>All the methods that talk to the server can be slow (the first call after the server
 * started also loads the model) and must not be called on the UI thread.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public interface IAssistantBackend {

    /**
     * @return a name to show to the user, e.g. "Ollama".
     */
    String getName();

    /**
     * @return the configuration in use.
     */
    AssistantConfig getConfig();

    /**
     * Use a new configuration and save it in the user preferences, so that it is used
     * also at the next start.
     *
     * @param config the configuration to use.
     */
    void setConfig( AssistantConfig config );

    /**
     * Check whether the server of the current configuration can be reached.
     *
     * <p>This never throws: an unreachable server is a {@link BackendStatus} with
     * <code>reachable</code> set to <code>false</code> and the reason in the message.</p>
     *
     * @return the status of the server.
     */
    BackendStatus checkStatus();

    /**
     * @return the models available on the server.
     * @throws Exception if the server can't be reached or answers with an error.
     */
    List<ModelInfo> listModels() throws Exception;

    /**
     * @return the name of the model suggested to users that have none yet.
     */
    String getRecommendedModel();

    /**
     * @return <code>true</code> if the server can download models through
     *          {@link #pullModel(String, IHMProgressMonitor)}.
     */
    boolean canPullModels();

    /**
     * Download a model on the server.
     *
     * <p>With a remote server the model is downloaded there, and the server can refuse it.</p>
     *
     * @param model the name of the model.
     * @param pm the monitor for the download progress, which is canceled through it.
     * @throws Exception if the download fails or is refused.
     */
    void pullModel( String model, IHMProgressMonitor pm ) throws Exception;

    /**
     * @return the text to SQL assistant, if this backend has one.
     */
    Optional<ISqlAssistant> getSqlAssistant();
}
