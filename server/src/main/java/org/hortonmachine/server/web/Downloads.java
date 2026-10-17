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
package org.hortonmachine.server.web;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import io.javalin.http.Context;

/**
 * The downloads of the files of the workspace.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class Downloads {

    private Downloads() {
    }

    /**
     * Send a file.
     *
     * @param ctx the request.
     * @param file the file, already checked to be one the user can read.
     * @param asText <code>true</code> to show it in the browser as text, else it is saved.
     */
    public static void send( Context ctx, Path file, boolean asText ) throws Exception {
        String name = file.getFileName().toString();
        ctx.contentType(asText ? "text/plain; charset=utf-8" : "application/octet-stream");
        ctx.header("Content-Disposition", contentDisposition(name, !asText));
        ctx.header("Content-Length", String.valueOf(Files.size(file)));
        try (InputStream input = Files.newInputStream(file); OutputStream output = ctx.outputStream()) {
            input.transferTo(output);
        }
    }

    /**
     * @return the Content-Disposition header of a file with the given name.
     */
    public static String contentDisposition( String name, boolean attachment ) {
        // the names are of the workspace, but keep the header safe anyway
        String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return (attachment ? "attachment" : "inline") + "; filename=\"" + safe + "\"";
    }
}
