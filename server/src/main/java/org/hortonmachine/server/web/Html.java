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

import org.hortonmachine.Version;
import org.hortonmachine.server.users.User;

/**
 * The HTML of the pages: plain HTML and CSS, without scripts.
 *
 * <p>Every text that comes from the users, the modules or the files must go through {@link #escape(String)}.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class Html {

    private static final String CSS = """
            :root { --bg: #ffffff; --fg: #1d1d1f; --muted: #6e6e73; --line: #d2d2d7; --accent: #0b5cad;
              --error: #b3261e; --ok: #1e7a34; --panel: #f5f5f7; }
            @media (prefers-color-scheme: dark) {
              :root { --bg: #161618; --fg: #ececf0; --muted: #a1a1a6; --line: #3a3a3c; --accent: #6cb1ff;
                --error: #ff8a80; --ok: #7ed492; --panel: #222225; }
            }
            * { box-sizing: border-box; }
            body { margin: 0; background: var(--bg); color: var(--fg);
              font: 15px/1.5 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
            header { display: flex; flex-wrap: wrap; gap: 4px 20px; align-items: center; padding: 10px 16px;
              border-bottom: 1px solid var(--line); background: var(--panel); }
            header .brand { font-weight: 600; margin-right: auto; }
            header nav { display: flex; flex-wrap: wrap; gap: 16px; align-items: center; }
            main { max-width: 1000px; margin: 0 auto; padding: 16px; }
            a { color: var(--accent); }
            h1 { font-size: 1.4em; margin: 0.4em 0 0.8em; }
            table { border-collapse: collapse; width: 100%; }
            th, td { text-align: left; padding: 6px 8px; border-bottom: 1px solid var(--line); vertical-align: top; }
            .scroll { overflow-x: auto; }
            label { display: block; font-weight: 600; margin-top: 12px; }
            input[type=text], input[type=password], input[type=number], select, textarea {
              width: 100%; max-width: 480px; padding: 6px 8px; font: inherit; color: var(--fg);
              background: var(--bg); border: 1px solid var(--line); border-radius: 4px; }
            button { font: inherit; padding: 6px 14px; border: 1px solid var(--accent); border-radius: 4px;
              background: var(--accent); color: var(--bg); cursor: pointer; }
            button.link { background: none; border: none; color: var(--accent); padding: 0; text-decoration: underline; }
            form.inline { display: inline; }
            .muted { color: var(--muted); }
            .error { color: var(--error); }
            .ok { color: var(--ok); }
            .field { margin-bottom: 6px; }
            .field .help { font-size: 0.9em; margin: 2px 0 0; max-width: 640px; }
            fieldset { border: 1px solid var(--line); border-radius: 6px; margin: 16px 0; padding: 4px 16px 14px; }
            legend { font-weight: 600; padding: 0 6px; }
            pre { background: var(--panel); border: 1px solid var(--line); border-radius: 4px; padding: 8px;
              overflow-x: auto; font-size: 0.85em; max-height: 480px; }
            .state-DONE { color: var(--ok); } .state-FAILED, .state-KILLED { color: var(--error); }
            .actions { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; margin: 12px 0; }
            .box { max-width: 380px; margin: 40px auto; padding: 20px; border: 1px solid var(--line);
              border-radius: 8px; background: var(--panel); }
            """;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
            .withZone(ZoneId.systemDefault());

    private Html() {
    }

    /**
     * Escape a text for HTML content and attribute values.
     *
     * @param text the text, may be <code>null</code>.
     * @return the escaped text, empty for <code>null</code>.
     */
    public static String escape( String text ) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length() + 16);
        for( int i = 0; i < text.length(); i++ ) {
            char c = text.charAt(i);
            switch( c ) {
            case '&':
                sb.append("&amp;");
                break;
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case '"':
                sb.append("&quot;");
                break;
            case '\'':
                sb.append("&#39;");
                break;
            default:
                sb.append(c);
            }
        }
        return sb.toString();
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
            sb.append(URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"));
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

    /**
     * @return the hidden field with the token that protects a form from other sites.
     */
    public static String csrfField( String csrfToken ) {
        return "<input type=\"hidden\" name=\"" + Auth.CSRF_FIELD + "\" value=\"" + escape(csrfToken) + "\">";
    }

    /**
     * Build a whole page.
     *
     * @param title the title, as text.
     * @param user the logged in user, or <code>null</code>.
     * @param csrfToken the token of the forms.
     * @param body the HTML of the content.
     * @param extraHead more HTML for the head, as a refresh, or <code>null</code>.
     * @return the HTML of the page.
     */
    public static String page( String title, User user, String csrfToken, String body, String extraHead ) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        sb.append("<title>").append(escape(title)).append(" - HortonMachine</title>");
        if (extraHead != null) {
            sb.append(extraHead);
        }
        sb.append("<style>").append(CSS).append("</style></head><body>");
        sb.append("<header><span class=\"brand\">HortonMachine</span>");
        if (user != null) {
            sb.append("<nav><a href=\"/modules\">Modules</a><a href=\"/jobs\">Jobs</a>");
            if (user.isAdmin()) {
                sb.append("<a href=\"/admin/users\">Users</a>");
            }
            sb.append("<span class=\"muted\">").append(escape(user.username)).append("</span>");
            sb.append("<form class=\"inline\" method=\"post\" action=\"/logout\">").append(csrfField(csrfToken));
            sb.append("<button class=\"link\" type=\"submit\">Log out</button></form></nav>");
        }
        sb.append("</header><main>").append(body).append("</main>");
        sb.append("<footer><main class=\"muted\">HortonMachine ").append(escape(Version.getVersion())).append("</main></footer>");
        sb.append("</body></html>");
        return sb.toString();
    }
}
