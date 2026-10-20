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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import jakarta.servlet.http.HttpSession;

/**
 * The access to the notebook server, as in Jupyter: no accounts, but a random token printed at the start,
 * in a link that starts the session of the browser that opens it.
 *
 * <p>Before every request:
 * <ul>
 * <li>the requests not addressed to the local host are refused, see {@link NotebookPages#checkLocal(Context)};</li>
 * <li>the security headers are set as for the server;</li>
 * <li>a request with the right <code>?{@value #TOKEN_PARAM}=</code> starts a session and goes to the same
 *     page without the token, so that it doesn't stay in the history;</li>
 * <li>the pages need a session started so, the posts also the token of the forms of the session.</li>
 * </ul>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class NotebookAuth {
    public static final String TOKEN_PARAM = "token";

    private static final String SESSION_OWNER = "hm.notebookOwner";
    private static final String PUBLIC_PREFIX = "/static/";

    private final byte[] token;
    private final Templates templates;

    /**
     * @param token the token of the link printed at the start.
     */
    public NotebookAuth( String token, Templates templates ) {
        this.token = token.getBytes(StandardCharsets.UTF_8);
        this.templates = templates;
    }

    public void register( JavalinConfig config ) {
        Auth.configureSessions(config);
        config.routes.before(this::beforeRequest);
    }

    private void beforeRequest( Context ctx ) {
        NotebookPages.checkLocal(ctx);
        Auth.securityHeaders(ctx);

        String given = ctx.queryParam(TOKEN_PARAM);
        if (given != null && ctx.method() == HandlerType.GET) {
            if (!MessageDigest.isEqual(token, given.getBytes(StandardCharsets.UTF_8))) {
                deny(ctx, "The token of the link is wrong: open the link printed by the server when it started.");
                return;
            }
            // a new session id, so that one known before can't be used
            ctx.req().getSession(true);
            ctx.req().changeSessionId();
            ctx.sessionAttribute(SESSION_OWNER, Boolean.TRUE);
            Auth.csrfToken(ctx);
            ctx.redirect(ctx.path());
            ctx.skipRemainingHandlers();
            return;
        }

        if (ctx.path().startsWith(PUBLIC_PREFIX)) {
            return;
        }
        HttpSession session = ctx.req().getSession(false);
        if (session == null || !Boolean.TRUE.equals(session.getAttribute(SESSION_OWNER))) {
            deny(ctx, "Open the link with the token printed by the server when it started.");
            return;
        }
        if (ctx.method() == HandlerType.POST) {
            Auth.checkCsrf(ctx);
        }
    }

    private void deny( Context ctx, String message ) {
        ctx.status(HttpStatus.UNAUTHORIZED).html(templates.page(ctx, "message.html", "Notebooks", Model.of("error", message)));
        ctx.skipRemainingHandlers();
    }
}
