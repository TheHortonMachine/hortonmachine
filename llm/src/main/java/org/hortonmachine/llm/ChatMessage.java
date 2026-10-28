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

/**
 * A message of a chat with a model.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @param role the role: <code>system</code>, <code>user</code> or <code>assistant</code>.
 * @param content the text of the message.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public record ChatMessage( String role, String content ) {

    /**
     * @param content the instructions for the model.
     * @return a system message.
     */
    public static ChatMessage system( String content ) {
        return new ChatMessage("system", content);
    }

    /**
     * @param content the request of the user.
     * @return a user message.
     */
    public static ChatMessage user( String content ) {
        return new ChatMessage("user", content);
    }

    /**
     * @param content an earlier answer of the model.
     * @return an assistant message.
     */
    public static ChatMessage assistant( String content ) {
        return new ChatMessage("assistant", content);
    }
}
