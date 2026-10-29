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
import java.util.function.Consumer;

import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

/**
 * An assistant that writes and explains the scripts of a scripting console.
 *
 * <p>The model gets the processing modules found on the classpath (the ones most relevant to
 * the request in detail, the others by name) and what the console gives through the
 * {@link ScriptContext}: default imports, helper reference, examples, variables.</p>
 *
 * <p>Scripts are only generated, never run: running them is up to the caller. They are only
 * compiled, through the {@link ScriptContext#checker()}, to send errors back to the model.</p>
 *
 * <p>The assistant keeps no state: to continue a discussion, pass the earlier exchanges as
 * <code>history</code>, oldest first (an empty list starts a new one). Only the most recent
 * turns may be sent to the model, to stay within its context.</p>
 *
 * <p>In all methods <code>onToken</code> receives the raw text of the answer while it
 * arrives (can be <code>null</code>), and <code>pm</code> is used to cancel the request. When a
 * script is rejected by the checks and sent back to the model, <code>pm.beginTask</code> is
 * called with what happens: the text <code>onToken</code> receives after it is a new answer,
 * which replaces the rejected one.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public interface IScriptAssistant {

    /**
     * Answer a request: write the script that does what is asked, or answer a question (which
     * module does something, what a parameter means, how the console works).
     *
     * <p>The answer is text; when it holds a script, that script is checked like the ones of
     * {@link #fix}.</p>
     *
     * @param context the console the request is made in.
     * @param request the request, in natural language.
     * @param history the earlier turns of the discussion, oldest first.
     * @param options the options of the request.
     * @param onToken receives the answer while it arrives, or <code>null</code>.
     * @param pm the monitor used to cancel the request.
     * @return the answer, with its script if it has one.
     * @throws Exception if the request fails or is canceled.
     */
    ScriptAnswer ask( ScriptContext context, String request, List<AssistantTurn> history, ScriptAssistOptions options,
            Consumer<String> onToken, IHMProgressMonitor pm ) throws Exception;

    /**
     * Fix a script that failed.
     *
     * @param context the console the script is for.
     * @param script the script that failed.
     * @param error the error of the run, with the relevant part of the stack trace.
     * @param history the earlier turns of the discussion, oldest first.
     * @param options the options of the request.
     * @param onToken receives the answer while it arrives, or <code>null</code>.
     * @param pm the monitor used to cancel the request.
     * @return the fixed script with an explanation of the fix.
     * @throws Exception if the request fails or is canceled.
     */
    ScriptAnswer fix( ScriptContext context, String script, String error, List<AssistantTurn> history,
            ScriptAssistOptions options, Consumer<String> onToken, IHMProgressMonitor pm ) throws Exception;

    /**
     * Explain what a script does.
     *
     * @param context the console the script is for.
     * @param script the script to explain.
     * @param history the earlier turns of the discussion, oldest first.
     * @param options the options of the request.
     * @param onToken receives the answer while it arrives, or <code>null</code>.
     * @param pm the monitor used to cancel the request.
     * @return the explanation.
     * @throws Exception if the request fails or is canceled.
     */
    String explain( ScriptContext context, String script, List<AssistantTurn> history, ScriptAssistOptions options,
            Consumer<String> onToken, IHMProgressMonitor pm ) throws Exception;

    /**
     * Get the server ready for the requests of a console, e.g. when the assistant opens: the
     * model is loaded and reads the part of the instructions that is the same for every request,
     * which servers that keep it then don't read again. Calling it is optional; the first request
     * is just slower without.
     *
     * <p>Nothing of the user is sent: no request, script or variables. The default does nothing.</p>
     *
     * @param context the console the requests will come from.
     * @param pm the monitor used to cancel it.
     * @throws Exception if the server can't be reached or has no such model.
     */
    default void prepare( ScriptContext context, IHMProgressMonitor pm ) throws Exception {
    }
}
