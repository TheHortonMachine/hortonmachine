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
import org.hortonmachine.server.users.ApiToken;
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
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
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
    private static final String ATTRIBUTE_TOKEN_ID = "hm.tokenId";

    private static final Set<String> PUBLIC_PATHS = Set.of("/login");

    /** The start of the paths of the api. */
    public static final String API_PREFIX = "/api";
    /** The paths of the api open without token: the landing page, the conformance and the documentation. */
    /** The page of the documentation of the api. */
    public static final String API_DOCS = API_PREFIX + "/docs";
    private static final Set<String> API_PUBLIC_PATHS = Set.of(API_PREFIX, API_PREFIX + "/conformance", API_DOCS);
    /** The part of the paths of the api of the files, which can be downloaded with signed links. */
    public static final String FILES_PATH = "/files/";

    /** The paths, as /static/hm.css, open also without login. */
    private static final String PUBLIC_PREFIX = "/static/";

    private static final String CONTENT_SECURITY_POLICY = "default-src 'none'; style-src 'self'; img-src 'self'; "
            + "form-action 'self'; frame-ancestors 'none'; base-uri 'none'";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserManager users;
    private final UrlSigner signer;
    private final Templates templates;

    public Auth( UserManager users, UrlSigner signer, Templates templates ) {
        this.users = users;
        this.signer = signer;
        this.templates = templates;
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

    /**
     * @return <code>true</code> if the path is of the api, which uses tokens instead of the session.
     */
    public static boolean isApi( String path ) {
        return path.equals(API_PREFIX) || path.startsWith(API_PREFIX + "/");
    }

    /**
     * @return the id of the token of a request of the api, given in the Authorization header, or <code>null</code>.
     */
    public static String tokenId( Context ctx ) {
        return ctx.attribute(ATTRIBUTE_TOKEN_ID);
    }

    /**
     * @return <code>true</code> if the request can be a signed link instead of having a token: the download of a file.
     */
    private static boolean isSignable( Context ctx ) {
        return (ctx.method() == HandlerType.GET || ctx.method() == HandlerType.HEAD) && ctx.path().startsWith(API_PREFIX + "/")
                && ctx.path().contains(FILES_PATH);
    }

    /**
     * The requests of the api: the user is given by the token in the Authorization header, the session cookie
     * is not used, so no other site can send requests in the name of a user and no form token is needed.
     * The files can also be downloaded with a link signed by the {@link UrlSigner}, without the header.
     */
    private void beforeApiRequest( Context ctx ) throws Exception {
        String header = ctx.header("Authorization");
        ApiToken token = null;
        String detail = "A valid token is needed, as the header Authorization: Bearer <token>. Tokens are created in the account page.";
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = users.authenticateToken(header.substring(7).trim());
            if (token != null) {
                ctx.attribute(ATTRIBUTE_TOKEN_ID, token.id);
            }
        } else if (header == null && ctx.queryParam(UrlSigner.SIGNATURE) != null && isSignable(ctx)) {
            String tokenId = ctx.queryParam(UrlSigner.TOKEN);
            if (signer.verify(ctx.path(), ctx.queryParam(UrlSigner.EXPIRES), tokenId, ctx.queryParam(UrlSigner.SIGNATURE))) {
                token = users.authenticateTokenId(tokenId);
            }
            detail = "The link has expired or is not valid anymore: get a new one, or use the header Authorization: Bearer <token>.";
        }
        User user = token != null ? users.getUser(token.userId) : null;
        if (user != null) {
            ctx.attribute(ATTRIBUTE_USER, user);
        }
        if (API_PUBLIC_PATHS.contains(ctx.path()) || user != null) {
            return;
        }
        ctx.header("WWW-Authenticate", "Bearer");
        new ApiProblem(401, ApiProblem.UNAUTHORIZED, "Unauthorized", detail).send(ctx);
        ctx.skipRemainingHandlers();
    }

    private void beforeRequest( Context ctx ) throws Exception {
        ctx.header("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        ctx.header("X-Content-Type-Options", "nosniff");
        ctx.header("X-Frame-Options", "DENY");
        ctx.header("Referrer-Policy", "no-referrer");
        if (isApi(ctx.path())) {
            if (ctx.path().equals(API_DOCS) && ctx.method() == HandlerType.GET) {
                // a page without data or effects, shown with the menu of the logged in user
                User user = sessionUser(ctx);
                if (user != null) {
                    ctx.attribute(ATTRIBUTE_USER, user);
                }
                return;
            }
            beforeApiRequest(ctx);
            return;
        }

        if (ctx.method() == HandlerType.POST) {
            HttpSession session = ctx.req().getSession(false);
            String expected = session != null ? (String) session.getAttribute(SESSION_CSRF) : null;
            String token = ctx.formParam(CSRF_FIELD);
            if (expected == null || token == null
                    || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
                throw new ForbiddenResponse("The form has expired: reload the page and try again.");
            }
        }

        User user = sessionUser(ctx);
        if (user != null) {
            ctx.attribute(ATTRIBUTE_USER, user);
        }

        if (PUBLIC_PATHS.contains(ctx.path()) || ctx.path().startsWith(PUBLIC_PREFIX)) {
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

    /**
     * @return the user logged in the session of the request, or <code>null</code>; the session ends if its user was
     *         deleted, disabled or changed the password since the login.
     */
    private User sessionUser( Context ctx ) throws Exception {
        HttpSession session = ctx.req().getSession(false);
        String userId = session != null ? (String) session.getAttribute(SESSION_USER_ID) : null;
        if (userId == null) {
            return null;
        }
        User user = users.getUser(userId);
        if (user == null || user.disabled || loginTime(session) < users.getSessionsValidFrom(userId)) {
            session.invalidate();
            return null;
        }
        return user;
    }

    private void loginPage( Context ctx ) {
        if (user(ctx) != null) {
            ctx.redirect("/");
            return;
        }
        ctx.html(loginHtml(ctx, null, ""));
    }

    private String loginHtml( Context ctx, String error, String username ) {
        return templates.page(ctx, "login.html", "Log in", Model.of("error", error, "username", username));
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
