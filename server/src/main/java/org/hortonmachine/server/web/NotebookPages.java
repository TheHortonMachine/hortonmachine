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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.hortonmachine.server.notebooks.NotebookDocument;
import org.hortonmachine.server.notebooks.NotebookDocument.Cell;
import org.hortonmachine.server.notebooks.NotebookManager;
import org.hortonmachine.server.notebooks.NotebookManager.NotebookInfo;
import org.hortonmachine.server.notebooks.NotebookSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.config.JavalinConfig;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.sse.SseClient;

/**
 * The pages of the notebooks, which run Groovy code in kernels on this computer.
 *
 * <p>They are the only pages of the server started with <code>--notebooks=true</code>, which listens only
 * on the local address; {@link NotebookAuth} also refuses every request not addressed to the local host
 * ({@link #checkLocal(Context)}), so that no other site can reach the server through the browser, as
 * with the DNS rebinding.</p>
 *
 * <p>The editor page is the only one of the server with scripts, all served by the server itself. The
 * runs of the cells are posts with the token of the session, as the forms; the outputs arrive as
 * server sent events.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class NotebookPages {

    /** The scripts, styles and images of the editor only from the server, the images also as data urls. */
    private static final String EDITOR_CSP = "default-src 'none'; script-src 'self'; style-src 'self'; "
            + "img-src 'self' data:; connect-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'";

    /** The largest form, as the save of a notebook. */
    private static final int MAX_FORM_BYTES = 20 * 1024 * 1024;

    private static final Pattern CELL_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private static final String STATIC_FOLDER = "org/hortonmachine/server/static/";
    /** The files of the editor, served under /static/notebook/, by name: path in the jar and type. */
    private static final Map<String, String[]> ASSETS = Map.of(//
            "notebook.js", new String[]{"notebook/notebook.js", "text/javascript"}, //
            "notebook.css", new String[]{"notebook/notebook.css", "text/css"}, //
            "codemirror.js", new String[]{"codemirror/codemirror.js", "text/javascript"}, //
            "codemirror.css", new String[]{"codemirror/codemirror.css", "text/css"}, //
            "groovy.js", new String[]{"codemirror/groovy.js", "text/javascript"}, //
            "markdown.js", new String[]{"codemirror/markdown.js", "text/javascript"}, //
            "matchbrackets.js", new String[]{"codemirror/matchbrackets.js", "text/javascript"}, //
            "closebrackets.js", new String[]{"codemirror/closebrackets.js", "text/javascript"} //
    );

    private final NotebookManager notebooks;
    private final Templates templates;
    private final ObjectMapper mapper = new ObjectMapper();

    public NotebookPages( NotebookManager notebooks, Templates templates ) {
        this.notebooks = notebooks;
        this.templates = templates;
    }

    public void register( JavalinConfig config ) {
        config.jetty.modifyServletContextHandler(handler -> handler.setMaxFormContentSize(MAX_FORM_BYTES));
        config.routes.get("/static/notebook/{name}", this::sendAsset);
        config.routes.get("/notebooks", ctx -> ctx.html(listHtml(ctx, Auth.takeFlash(ctx), null)));
        config.routes.post("/notebooks", this::create);
        config.routes.get("/notebooks/{name}", this::editor);
        config.routes.post("/notebooks/{name}/delete", this::delete);
        config.routes.get("/notebooks/{name}/content", this::content);
        config.routes.get("/notebooks/{name}/download", this::download);
        config.routes.post("/notebooks/{name}/save", this::save);
        config.routes.post("/notebooks/{name}/run", this::run);
        config.routes.post("/notebooks/{name}/interrupt", ctx -> {
            session(ctx).interrupt();
            ctx.status(HttpStatus.NO_CONTENT);
        });
        config.routes.post("/notebooks/{name}/restart", ctx -> {
            session(ctx).restart();
            ctx.status(HttpStatus.NO_CONTENT);
        });
        config.routes.sse("/notebooks/{name}/events", this::events);
    }

    /**
     * Refuse the requests whose Host or Origin is not the local host.
     */
    static void checkLocal( Context ctx ) {
        if (!isLocal(hostName(ctx.header("Host")))) {
            throw new ForbiddenResponse("With the notebooks on, the server answers only to the browsers of this computer.");
        }
        String origin = ctx.header("Origin");
        if (origin != null && !origin.equals("null")) {
            String originHost;
            try {
                originHost = URI.create(origin).getHost();
            } catch (IllegalArgumentException e) {
                originHost = null;
            }
            if (!isLocal(originHost)) {
                throw new ForbiddenResponse("Requests from other sites are refused.");
            }
        }
    }

    /**
     * @return the host of a Host header, without the port.
     */
    static String hostName( String hostHeader ) {
        if (hostHeader == null) {
            return null;
        }
        String host = hostHeader.trim();
        if (host.startsWith("[")) {
            int end = host.indexOf(']');
            return end > 0 ? host.substring(0, end + 1) : host;
        }
        int colon = host.lastIndexOf(':');
        return colon >= 0 ? host.substring(0, colon) : host;
    }

    static boolean isLocal( String host ) {
        return host != null && (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("[::1]")
                || host.equals("::1"));
    }

    private String listHtml( Context ctx, String message, String error ) {
        List<Model> items = new ArrayList<>();
        for( NotebookInfo info : notebooks.list() ) {
            items.add(Model.of("name", info.name(), "href", href(info.name()), "modified", Html.formatTime(info.modified())));
        }
        Model model = Model.of("notebookList", items, "hasNotebooks", !items.isEmpty(), "message", message, "error", error, //
                "folder", notebooks.getFolder().getAbsolutePath());
        return templates.page(ctx, "notebooks.html", "Notebooks", model);
    }

    private static String href( String name ) {
        return "/notebooks/" + URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private void create( Context ctx ) throws Exception {
        String name = ctx.formParam("name");
        name = name == null ? "" : name.trim();
        if (name.endsWith(NotebookManager.EXTENSION)) {
            name = name.substring(0, name.length() - NotebookManager.EXTENSION.length());
        }
        try {
            notebooks.create(name);
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST).html(listHtml(ctx, null, e.getMessage()));
            return;
        }
        ctx.redirect(href(name));
    }

    private void delete( Context ctx ) throws Exception {
        String name = ctx.pathParam("name");
        try {
            notebooks.delete(name);
        } catch (IllegalArgumentException e) {
            throw new NotFoundResponse(e.getMessage());
        }
        Auth.flash(ctx, "The notebook " + name + " was deleted.");
        ctx.redirect("/notebooks");
    }

    /**
     * @return the session of the notebook of the path.
     */
    private NotebookSession session( Context ctx ) throws Exception {
        try {
            return notebooks.open(ctx.pathParam("name"));
        } catch (IllegalArgumentException e) {
            throw new NotFoundResponse(e.getMessage());
        }
    }

    private void editor( Context ctx ) throws Exception {
        NotebookSession session = session(ctx);
        ctx.header("Content-Security-Policy", EDITOR_CSP);
        String base = href(session.getName());
        ctx.html(templates.page(ctx, "notebook.html", session.getName(), Model.of("name", session.getName(), "base", base)));
    }

    private void content( Context ctx ) throws Exception {
        ctx.contentType("application/json").result(session(ctx).snapshot().toString());
    }

    /**
     * Send the saved notebook file, as a download.
     */
    private void download( Context ctx ) throws Exception {
        NotebookSession session = session(ctx);
        String fileName = session.getName() + NotebookManager.EXTENSION;
        ctx.contentType("application/x-ipynb+json");
        ctx.header("Content-Disposition", "attachment; filename*=UTF-8''"
                + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20"));
        ctx.result(session.savedBytes());
    }

    private void save( Context ctx ) throws Exception {
        NotebookSession session = session(ctx);
        String json = ctx.formParam("cells");
        List<Cell> cells = new ArrayList<>();
        try {
            JsonNode nodes = mapper.readTree(json == null ? "" : json);
            if (!nodes.isArray()) {
                throw new BadRequestResponse("The cells are missing.");
            }
            for( JsonNode node : nodes ) {
                String id = node.path("id").asText();
                String type = node.path("type").asText();
                if (!CELL_ID.matcher(id).matches()
                        || !(NotebookDocument.CODE.equals(type) || NotebookDocument.MARKDOWN.equals(type))) {
                    throw new BadRequestResponse("Not a valid cell.");
                }
                cells.add(new Cell(id, type, node.path("source").asText("")));
            }
        } catch (IOException e) {
            throw new BadRequestResponse("The cells are not valid JSON.");
        }
        session.save(cells);
        ctx.status(HttpStatus.NO_CONTENT);
    }

    private void run( Context ctx ) throws Exception {
        NotebookSession session = session(ctx);
        String cellId = ctx.formParam("cell");
        String code = ctx.formParam("code");
        if (cellId == null || !CELL_ID.matcher(cellId).matches() || code == null) {
            throw new BadRequestResponse("The cell and its code are needed.");
        }
        try {
            session.run(cellId, code);
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse(e.getMessage());
        }
        ctx.status(HttpStatus.ACCEPTED);
    }

    private void events( SseClient client ) {
        NotebookSession session;
        try {
            session = session(client.ctx());
        } catch (Exception e) {
            client.close();
            return;
        }
        client.keepAlive();
        Consumer<JsonNode> listener = new Consumer<JsonNode>(){
            @Override
            public void accept( JsonNode event ) {
                if (client.terminated()) {
                    throw new IllegalStateException("The page is closed.");
                }
                client.sendEvent("message", event.toString());
            }
        };
        client.onClose(() -> session.removeListener(listener));
        session.addListener(listener);
    }

    private void sendAsset( Context ctx ) throws IOException {
        String[] asset = ASSETS.get(ctx.pathParam("name"));
        if (asset == null) {
            throw new NotFoundResponse("The file doesn't exist.");
        }
        byte[] bytes;
        try (InputStream input = NotebookPages.class.getClassLoader().getResourceAsStream(STATIC_FOLDER + asset[0])) {
            if (input == null) {
                throw new NotFoundResponse("The file doesn't exist.");
            }
            bytes = input.readAllBytes();
        }
        ctx.contentType(asset[1] + "; charset=utf-8");
        ctx.header("Cache-Control", "no-cache");
        ctx.result(bytes);
    }
}
