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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The texts of the values shown in the pages: times, durations, sizes and paths for the links.
 * The pages themselves are the {@link Templates}.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class Html {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
            .withZone(ZoneId.systemDefault());

    private Html() {
    }

    /**
     * Encode a relative path for a URL, segment by segment, keeping the slashes.
     */
    public static String urlPath( String path ) {
        StringBuilder sb = new StringBuilder();
        for( String segment : path.split("/", -1) ) {
            if (sb.length() > 0) {
                sb.append('/');
            }
            // ~ is unreserved: clients may decode it, which would change the paths of the signed links
            sb.append(URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20").replace("%7E", "~"));
        }
        return sb.toString();
    }

    /**
     * @return the time as yyyy-MM-dd HH:mm:ss in the time zone of the server, empty for <code>null</code>.
     */
    public static String formatTime( Long millis ) {
        if (millis == null) {
            return "";
        }
        return TIME_FORMAT.format(Instant.ofEpochMilli(millis));
    }

    /**
     * @return the duration as 1h 02m, 3m 05s or 12s.
     */
    public static String formatDuration( long millis ) {
        long seconds = Math.max(0, millis / 1000);
        if (seconds >= 3600) {
            return String.format(Locale.ROOT, "%dh %02dm", seconds / 3600, (seconds % 3600) / 60);
        } else if (seconds >= 60) {
            return String.format(Locale.ROOT, "%dm %02ds", seconds / 60, seconds % 60);
        }
        return seconds + "s";
    }

    /**
     * @return the size as 512 B, 1.5 KB, 20.3 MB or 1.2 GB.
     */
    public static String formatSize( long bytes ) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        String[] units = {"KB", "MB", "GB", "TB"};
        double size = bytes;
        int unit = -1;
        while( size >= 1024 && unit < units.length - 1 ) {
            size /= 1024;
            unit++;
        }
        return String.format(Locale.ROOT, "%.1f %s", size, units[unit]);
    }
}
