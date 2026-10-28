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
package org.hortonmachine.llm;

import java.util.List;
import java.util.function.Consumer;

import org.hortonmachine.gears.assistant.ModelInfo;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.json.JSONObject;

/**
 * A language model that answers chats.
 *
 * <p>The assistants only use this interface, so that no vendor library ends up in
 * HortonMachine and the server can be changed by configuration.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public interface LlmProvider {

    /**
     * Send a chat and get the answer.
     *
     * @param messages the messages of the chat, the instructions first.
     * @param jsonSchema the JSON schema the answer must follow, or <code>null</code> for free text.
     * @param onToken receives the answer while it arrives, or <code>null</code>.
     * @param pm the monitor used to cancel the request, or <code>null</code>.
     * @return the whole answer.
     * @throws Exception if the request fails, or a
     *          {@link org.hortonmachine.gears.libs.exceptions.ModelsUserCancelException} if it is canceled.
     */
    String chat( List<ChatMessage> messages, JSONObject jsonSchema, Consumer<String> onToken, IHMProgressMonitor pm )
            throws Exception;

    /**
     * @return the models the server offers.
     * @throws Exception if the server can't be reached.
     */
    List<ModelInfo> listModels() throws Exception;
}
