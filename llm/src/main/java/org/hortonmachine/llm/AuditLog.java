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

import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;

import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.assistant.AssistantConfig;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The log of every request to a model, one JSON object per line.
 *
 * <p>Each line has the time, the task, the server and model, the whole prompt, the answer, the
 * code taken from it and the error, if any: what is needed to understand afterwards where some
 * SQL or script came from. The file is {@value #DEFAULT_FILE_NAME} in the
 * <code>.hortonmachine</code> folder of the user home, or the one given by the system property
 * {@value #KEY_AUDIT_FILE}.</p>
 *
 * <p>Writing the log never breaks a request: failures are only logged.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class AuditLog {

    public static final String KEY_AUDIT_FILE = "hm.llm.audit";
    public static final String DEFAULT_FILE_NAME = "llm-audit.jsonl";

    private final File file;

    /**
     * @param file the file to append to.
     */
    public AuditLog( File file ) {
        this.file = file;
    }

    /**
     * @return the log in the default place, or the one of the system property {@value #KEY_AUDIT_FILE}.
     */
    public static AuditLog defaultLog() {
        String path = System.getProperty(KEY_AUDIT_FILE);
        if (path != null && !path.isBlank()) {
            return new AuditLog(new File(path));
        }
        File folder = new File(System.getProperty("user.home"), ".hortonmachine");
        return new AuditLog(new File(folder, DEFAULT_FILE_NAME));
    }

    /**
     * @return the file of the log.
     */
    public File getFile() {
        return file;
    }

    /**
     * Add a request to the log.
     *
     * @param task what was asked, e.g. <code>sql-generate</code>.
     * @param config the server and model used.
     * @param messages the whole prompt.
     * @param response the answer of the model, or <code>null</code> if there is none.
     * @param code the code taken from the answer, or <code>null</code>.
     * @param error the error, or <code>null</code> if the request succeeded.
     */
    public synchronized void append( String task, AssistantConfig config, List<ChatMessage> messages, String response,
            String code, String error ) {
        try {
            JSONObject entry = new JSONObject();
            entry.put("timestamp", OffsetDateTime.now().toString());
            entry.put("task", task);
            entry.put("serverKind", config.serverKind().name());
            entry.put("server", config.baseUrl());
            entry.put("model", config.model());
            JSONArray messagesArray = new JSONArray();
            for( ChatMessage message : messages ) {
                messagesArray.put(new JSONObject().put("role", message.role()).put("content", message.content()));
            }
            entry.put("messages", messagesArray);
            entry.put("response", response == null ? JSONObject.NULL : response);
            entry.put("code", code == null ? JSONObject.NULL : code);
            entry.put("error", error == null ? JSONObject.NULL : error);

            File folder = file.getParentFile();
            if (folder != null && !folder.exists()) {
                folder.mkdirs();
            }
            FileUtilities.appendToFile(entry.toString() + "\n", file);
        } catch (Exception e) {
            Logger.INSTANCE.insertError("AuditLog", "Could not write the assistant audit log: " + file, e);
        }
    }
}
