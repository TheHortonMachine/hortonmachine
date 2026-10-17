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
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.http.HttpCookie;
import org.hortonmachine.server.users.LoginLockedException;
import org.hortonmachine.server.users.User;
import org.hortonmachine.server.users.UserManager;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;
import jakarta.servlet.http.HttpSession;

/**
 * The login of the users and the protection of the pages.
 *
 * <p>Before every request:
 * <ul>
 * <li>security headers forbid scripts, frames and foreign form targets;</li>
 * <li>every POST must carry the token of the session ({@link #CSRF_FIELD}), so that other sites can't send forms
 *     in the name of a user; the session cookie is also SameSite=Strict and HttpOnly;</li>
 * <li>the pages need a logged in, enabled user, and those under <code>/admin</code> an administrator.</li>
 * </ul>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class Auth {
    public static final String CSRF_FIELD = "csrf";
    public static final String SESSION_COOKIE = "HMSESSION";
    /** The hours a session lasts without requests. */
    public static final int SESSION_HOURS = 8;

    private static final String SESSION_USER_ID = "hm.userId";
    private static final String SESSION_LOGIN_TIME = "hm.loginTime";
    private static final String SESSION_CSRF = "hm.csrf";
    private static final String SESSION_FLASH = "hm.flash";
    private static final String ATTRIBUTE_USER = "hm.user";

    private static final Set<String> PUBLIC_PATHS = Set.of("/login");

    private static final String CONTENT_SECURITY_POLICY = "default-src 'none'; style-src 'unsafe-inline'; img-src 'self'; "
            + "form-action 'self'; frame-ancestors 'none'; base-uri 'none'";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserManager users;

    public Auth( UserManager users ) {
        this.users = users;
    }

    /**
     * Add the session configuration, the checks before the requests and the login and logout pages.
     */
    public void register( JavalinConfig config ) {
        config.jetty.modifyServletContextHandler(handler -> {
            SessionHandler sessions = new SessionHandler();
            sessions.setSessionCookie(SESSION_COOKIE);
            sessions.setHttpOnly(true);
            sessions.setSameSite(HttpCookie.SameSite.STRICT);
            sessions.setMaxInactiveInterval(SESSION_HOURS * 3600);
            handler.setSessionHandler(sessions);
        });
        config.routes.before(this::beforeRequest);
        config.routes.get("/login", this::loginPage);
        config.routes.post("/login", this::login);
        config.routes.post("/logout", this::logout);
    }

    /**
     * @return the logged in user of the request, or <code>null</code> on the public pages.
     */
    public static User user( Context ctx ) {
        return ctx.attribute(ATTRIBUTE_USER);
    }

    /**
     * @return the token of the forms of the session, created if missing.
     */
    public static String csrfToken( Context ctx ) {
        String token = ctx.sessionAttribute(SESSION_CSRF);
        if (token == null) {
            token = newToken();
            ctx.sessionAttribute(SESSION_CSRF, token);
        }
        return token;
    }

    /**
     * Keep a message for the next page of the session, as after the redirect of a form.
     */
    public static void flash( Context ctx, String message ) {
        ctx.sessionAttribute(SESSION_FLASH, message);
    }

    /**
     * @return the message kept for this page, removed from the session, or <code>null</code>.
     */
    public static String takeFlash( Context ctx ) {
        HttpSession session = ctx.req().getSession(false);
        if (session == null) {
            return null;
        }
        String message = (String) session.getAttribute(SESSION_FLASH);
        session.removeAttribute(SESSION_FLASH);
        return message;
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void beforeRequest( Context ctx ) throws Exception {
        ctx.header("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        ctx.header("X-Content-Type-Options", "nosniff");
        ctx.header("X-Frame-Options", "DENY");
        ctx.header("Referrer-Policy", "no-referrer");

        if (ctx.method() == HandlerType.POST) {
            HttpSession session = ctx.req().getSession(false);
            String expected = session != null ? (String) session.getAttribute(SESSION_CSRF) : null;
            String token = ctx.formParam(CSRF_FIELD);
            if (expected == null || token == null
                    || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
                throw new ForbiddenResponse("The form has expired: reload the page and try again.");
            }
        }

        HttpSession session = ctx.req().getSession(false);
        String userId = session != null ? (String) session.getAttribute(SESSION_USER_ID) : null;
        User user = userId != null ? users.getUser(userId) : null;
        if (userId != null && (user == null || user.disabled || loginTime(session) < users.getSessionsValidFrom(userId))) {
            // deleted, disabled or the password changed since the login
            session.invalidate();
            user = null;
        }
        if (user != null) {
            ctx.attribute(ATTRIBUTE_USER, user);
        }

        if (PUBLIC_PATHS.contains(ctx.path())) {
            return;
        }
        if (user == null) {
            if (ctx.method() == HandlerType.GET) {
                ctx.redirect("/login");
                ctx.skipRemainingHandlers();
                return;
            }
            throw new UnauthorizedResponse("Log in first.");
        }
        if (ctx.path().startsWith("/admin") && !user.isAdmin()) {
            throw new ForbiddenResponse("Only for administrators.");
        }
    }

    private void loginPage( Context ctx ) {
        if (user(ctx) != null) {
            ctx.redirect("/");
            return;
        }
        ctx.html(loginHtml(ctx, null, ""));
    }

    private static String loginHtml( Context ctx, String error, String username ) {
        StringBuilder body = new StringBuilder("<div class=\"box\"><h1>Log in</h1>");
        if (error != null) {
            body.append("<p class=\"error\">").append(Html.escape(error)).append("</p>");
        }
        body.append("<form method=\"post\" action=\"/login\">").append(Html.csrfField(csrfToken(ctx)));
        body.append("<label for=\"username\">User name</label>");
        body.append("<input type=\"text\" id=\"username\" name=\"username\" autocomplete=\"username\" required value=\"")
                .append(Html.escape(username)).append("\">");
        body.append("<label for=\"password\">Password</label>");
        body.append("<input type=\"password\" id=\"password\" name=\"password\" autocomplete=\"current-password\" required>");
        body.append("<p><button type=\"submit\">Log in</button></p></form></div>");
        return Html.page("Log in", null, null, body.toString(), null);
    }

    private void login( Context ctx ) throws Exception {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");
        User user;
        try {
            user = users.login(username, password != null ? password.toCharArray() : new char[0]);
        } catch (LoginLockedException e) {
            ctx.status(HttpStatus.TOO_MANY_REQUESTS).html(loginHtml(ctx, e.getMessage(), username));
            return;
        }
        if (user == null) {
            ctx.status(HttpStatus.UNAUTHORIZED).html(loginHtml(ctx, "Wrong user name or password.", username));
            return;
        }
        startSession(ctx, user, System.currentTimeMillis());
        ctx.redirect("/");
    }

    /**
     * Start the session of a user, with a new id, so that an id known before can't be used.
     *
     * @param loginTime the time of the login: the session ends if the password changes after it.
     */
    static void startSession( Context ctx, User user, long loginTime ) {
        ctx.req().changeSessionId();
        ctx.sessionAttribute(SESSION_USER_ID, user.id);
        ctx.sessionAttribute(SESSION_LOGIN_TIME, loginTime);
        ctx.sessionAttribute(SESSION_CSRF, newToken());
    }

    private static long loginTime( HttpSession session ) {
        Object time = session.getAttribute(SESSION_LOGIN_TIME);
        return time instanceof Long ? (Long) time : 0L;
    }

    private void logout( Context ctx ) {
        HttpSession session = ctx.req().getSession(false);
        if (session != null) {
            session.invalidate();
        }
        ctx.redirect("/login");
    }
}
