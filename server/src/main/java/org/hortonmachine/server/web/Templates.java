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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;

import org.hortonmachine.Version;
import org.hortonmachine.server.users.User;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheResolver;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

/**
 * The templates of the pages, in the Mustache format: plain HTML with <code>{{name}}</code> for the values,
 * always escaped, and <code>{{#name}}...{{/name}}</code> for the parts shown if a value is true or repeated
 * for each element of a list. See https://mustache.github.io/mustache.5.html.
 *
 * <p>The templates are in the <code>org/hortonmachine/server/templates</code> folder of the jar. A file
 * with the same name in the {@value #OVERRIDE_FOLDER} folder of the workspace replaces it, as for an own
 * <code>hm.css</code>; while that folder exists, the changes to its files show at the next page, without
 * a restart.
 *
 * <p>Every page is a template rendered inside <code>layout.html</code>, which has the header, the menu
 * and the footer. All the templates have the values:
 * <ul>
 * <li><code>title</code>: the title of the page;</li>
 * <li><code>csrf</code>: the token to put in every form, as <code>&lt;input type="hidden" name="csrf" value="{{csrf}}"&gt;</code>;</li>
 * <li><code>user</code>: the logged in user, with <code>username</code> and <code>isAdmin</code>, missing in the login page;</li>
 * <li><code>version</code>: the version of HortonMachine.</li>
 * </ul>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class Templates {
    /** The folder of the workspace with the templates that replace the built-in ones. */
    public static final String OVERRIDE_FOLDER = "templates";
    /** The files served to the browsers under /static/, without login. */
    public static final Set<String> STATIC_FILES = Set.of("hm.css");

    private static final String CLASSPATH_FOLDER = "org/hortonmachine/server/templates/";
    private static final String LAYOUT = "layout.html";

    private final File overrideFolder;
    private final DefaultMustacheFactory cachedFactory;

    /**
     * @param workspace the workspace, whose {@value #OVERRIDE_FOLDER} folder can replace the templates.
     */
    public Templates( File workspace ) {
        overrideFolder = new File(workspace, OVERRIDE_FOLDER);
        cachedFactory = new DefaultMustacheFactory(this::getReader);
    }

    /**
     * Add the route of the static files, as the style sheet.
     */
    public void register( JavalinConfig config ) {
        config.routes.get("/static/{name}", this::sendStatic);
    }

    /**
     * @return the reader of a template or static file, from the override folder or the jar, or
     *         <code>null</code> if missing.
     */
    private Reader getReader( String name ) {
        // the names are of the code, not of the users, but stay in the folders anyway
        if (name.contains("..") || name.contains("/") || name.contains("\\")) {
            return null;
        }
        File override = new File(overrideFolder, name);
        try {
            if (override.isFile()) {
                return Files.newBufferedReader(override.toPath(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        InputStream input = Templates.class.getClassLoader().getResourceAsStream(CLASSPATH_FOLDER + name);
        return input != null ? new InputStreamReader(input, StandardCharsets.UTF_8) : null;
    }

    private MustacheResolver resolver() {
        return this::getReader;
    }

    /**
     * @return the factory: a new one while there are overrides, so that their changes show at once.
     */
    private DefaultMustacheFactory factory() {
        return overrideFolder.isDirectory() ? new DefaultMustacheFactory(resolver()) : cachedFactory;
    }

    /**
     * Render a template.
     *
     * @param name the name of the template, as <code>jobs.html</code>.
     * @param scopes the models, the later ones win for the same name.
     * @return the HTML.
     */
    public String render( String name, Object... scopes ) {
        Mustache mustache = factory().compile(name);
        StringWriter writer = new StringWriter();
        mustache.execute(writer, scopes);
        return writer.toString();
    }

    /**
     * Render a whole page: the template inside the layout.
     *
     * @param ctx the request, for the user and the token of the forms.
     * @param template the name of the template of the content.
     * @param title the title of the page.
     * @param model the values of the template.
     * @return the HTML.
     */
    public String page( Context ctx, String template, String title, Model model ) {
        return page(ctx, template, title, model, null);
    }

    /**
     * Render a whole page that reloads itself.
     *
     * @param refreshSeconds the seconds after which the page reloads, or <code>null</code> for never.
     */
    public String page( Context ctx, String template, String title, Model model, Integer refreshSeconds ) {
        User user = Auth.user(ctx);
        Model common = Model.of("title", title, "csrf", Auth.csrfToken(ctx), "version", Version.getVersion(), //
                "user", user == null ? null : Model.of("username", user.username, "isAdmin", user.isAdmin()), //
                "refresh", refreshSeconds);
        String content = render(template, common, model);
        return render(LAYOUT, common, Model.of("content", content));
    }

    /**
     * Show a page with a message and a link back, as for the errors of an action.
     *
     * @param error the message, shown as an error.
     * @param backHref where the link goes.
     * @param backLabel the text of the link.
     */
    public String messagePage( Context ctx, String title, String error, String backHref, String backLabel ) {
        return page(ctx, "message.html", title, Model.of("error", error, "backHref", backHref, "backLabel", backLabel));
    }

    private void sendStatic( Context ctx ) throws IOException {
        String name = ctx.pathParam("name");
        Reader reader = STATIC_FILES.contains(name) ? getReader(name) : null;
        if (reader == null) {
            throw new NotFoundResponse("The file doesn't exist.");
        }
        StringWriter writer = new StringWriter();
        try (Reader input = reader) {
            input.transferTo(writer);
        }
        ctx.contentType("text/css; charset=utf-8");
        ctx.header("Cache-Control", "no-cache");
        ctx.result(writer.toString());
    }
}
