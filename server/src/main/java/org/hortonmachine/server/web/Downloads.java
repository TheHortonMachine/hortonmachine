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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;

/**
 * The downloads of the files of the workspace.
 *
 * <p>A single byte range can be asked (RFC 9110, as <code>Range: bytes=0-1023</code>, <code>bytes=1024-</code>
 * or <code>bytes=-512</code>), so that GDAL and QGIS can read only the parts of a cloud optimized GeoTIFF they
 * need; several ranges in one request get the whole file. HEAD requests get the headers only.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class Downloads {
    private static final Pattern RANGE = Pattern.compile("bytes=(\\d{0,18})-(\\d{0,18})", Pattern.CASE_INSENSITIVE);
    private static final int BUFFER_SIZE = 64 * 1024;

    private Downloads() {
    }

    /**
     * Send a file, or the byte range of it asked by the request.
     *
     * @param ctx the request, a GET or a HEAD.
     * @param file the file, already checked to be one the user can read.
     * @param asText <code>true</code> to show it in the browser as text, else it is saved.
     */
    public static void send( Context ctx, Path file, boolean asText ) throws Exception {
        String name = file.getFileName().toString();
        long size = Files.size(file);
        Instant modified = Files.getLastModifiedTime(file).toInstant().truncatedTo(ChronoUnit.SECONDS);
        String lastModified = DateTimeFormatter.RFC_1123_DATE_TIME.format(modified.atOffset(ZoneOffset.UTC));
        String etag = "\"" + Long.toHexString(size) + "-" + Long.toHexString(modified.getEpochSecond()) + "\"";

        ctx.contentType(asText ? "text/plain; charset=utf-8" : "application/octet-stream");
        ctx.header("Content-Disposition", contentDisposition(name, !asText));
        ctx.header("Accept-Ranges", "bytes");
        ctx.header("ETag", etag);
        ctx.header("Last-Modified", lastModified);
        if (etag.equals(ctx.header("If-None-Match"))) {
            ctx.status(304);
            return;
        }

        long from = 0;
        long to = size - 1;
        String ifRange = ctx.header("If-Range");
        long[] range = ifRange == null || ifRange.equals(etag) || ifRange.equals(lastModified) ? range(ctx.header("Range"), size) : null;
        if (range != null && range.length == 0) {
            ctx.status(416);
            ctx.header("Content-Range", "bytes */" + size);
            return;
        }
        if (range != null) {
            from = range[0];
            to = range[1];
            ctx.status(206);
            ctx.header("Content-Range", "bytes " + from + "-" + to + "/" + size);
        }
        long length = to - from + 1;
        // the response is written as is, not compressed, so that its length and ranges are those of the file
        ctx.res().setContentLengthLong(length);
        if (ctx.method() == HandlerType.HEAD) {
            return;
        }
        try (InputStream input = Files.newInputStream(file); OutputStream output = ctx.res().getOutputStream()) {
            input.skipNBytes(from);
            byte[] buffer = new byte[BUFFER_SIZE];
            long left = length;
            while( left > 0 ) {
                int read = input.read(buffer, 0, (int) Math.min(buffer.length, left));
                if (read < 0) {
                    // the file got shorter while sending it
                    break;
                }
                output.write(buffer, 0, read);
                left -= read;
            }
        }
    }

    /**
     * Read the Range header of a request.
     *
     * @param header the header, or <code>null</code>.
     * @param size the size of the file.
     * @return the first and last byte asked, <code>null</code> to send the whole file (no header, several ranges or
     *         not understood, which the client must accept), an empty array if the range is outside the file.
     */
    static long[] range( String header, long size ) {
        if (header == null) {
            return null;
        }
        Matcher matcher = RANGE.matcher(header.trim());
        if (!matcher.matches()) {
            return null;
        }
        String first = matcher.group(1);
        String last = matcher.group(2);
        if (first.isEmpty()) {
            // the last bytes
            if (last.isEmpty()) {
                return null;
            }
            long suffix = Long.parseLong(last);
            if (suffix == 0 || size == 0) {
                return new long[0];
            }
            return new long[]{Math.max(0, size - suffix), size - 1};
        }
        long from = Long.parseLong(first);
        Long to = last.isEmpty() ? null : Long.parseLong(last);
        if (to != null && to < from) {
            return null;
        }
        if (from >= size) {
            return new long[0];
        }
        return new long[]{from, to == null ? size - 1 : Math.min(to, size - 1)};
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
