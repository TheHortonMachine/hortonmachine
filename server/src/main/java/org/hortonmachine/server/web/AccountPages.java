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

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.server.users.ApiToken;
import org.hortonmachine.server.users.LoginLockedException;
import org.hortonmachine.server.users.User;
import org.hortonmachine.server.users.UserManager;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;

/**
 * The page of the account of the logged in user, to change the password.
 *
 * <p>The current password is asked, and its failures count as failed logins. After the change the other
 * sessions of the user end, this one continues.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class AccountPages {
    private static final String SESSION_NEW_TOKEN = "hm.newToken";

    private final UserManager users;
    private final Templates templates;

    public AccountPages( UserManager users, Templates templates ) {
        this.users = users;
        this.templates = templates;
    }

    public void register( JavalinConfig config ) {
        config.routes.get("/account", ctx -> ctx.html(accountHtml(ctx, Auth.takeFlash(ctx), null)));
        config.routes.post("/account/password", this::changePassword);
        config.routes.post("/account/tokens", this::createToken);
        config.routes.post("/account/tokens/{id}/delete", this::revokeToken);
    }

    private String accountHtml( Context ctx, String message, String error ) throws Exception {
        User user = Auth.user(ctx);
        List<Model> tokens = new ArrayList<>();
        for( ApiToken token : users.listTokens(user.id) ) {
            tokens.add(Model.of("id", token.id, "name", token.name, "created", Html.formatTime(token.created), //
                    "lastUsed", token.lastUsed != null ? Html.formatTime(token.lastUsed) : "never"));
        }
        // the secret of a token just created, shown once
        String newToken = ctx.sessionAttribute(SESSION_NEW_TOKEN);
        ctx.sessionAttribute(SESSION_NEW_TOKEN, null);
        Model model = Model.of("username", user.username, "role", user.role.name().toLowerCase(), "created",
                Html.formatTime(user.created), "message", message, "error", error, "minLength", UserManager.MIN_PASSWORD_LENGTH, //
                "tokens", tokens, "hasTokens", !tokens.isEmpty(), "newToken", newToken, //
                "maxTokenName", UserManager.MAX_TOKEN_NAME_LENGTH);
        return templates.page(ctx, "account.html", "Account", model);
    }

    private void createToken( Context ctx ) throws Exception {
        User user = Auth.user(ctx);
        String secret;
        try {
            secret = users.createToken(user.id, ctx.formParam("name"));
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST).html(accountHtml(ctx, null, e.getMessage()));
            return;
        }
        ctx.sessionAttribute(SESSION_NEW_TOKEN, secret);
        ctx.redirect("/account");
    }

    private void revokeToken( Context ctx ) throws Exception {
        User user = Auth.user(ctx);
        if (!users.revokeToken(user.id, ctx.pathParam("id"))) {
            throw new NotFoundResponse("The token doesn't exist.");
        }
        Auth.flash(ctx, "The token was revoked.");
        ctx.redirect("/account");
    }

    private void changePassword( Context ctx ) throws Exception {
        User user = Auth.user(ctx);
        String current = ctx.formParam("current");
        String password = ctx.formParam("password");
        String again = ctx.formParam("again");
        if (password == null || !password.equals(again)) {
            ctx.status(HttpStatus.BAD_REQUEST).html(accountHtml(ctx, null, "The new passwords differ."));
            return;
        }
        try {
            if (users.login(user.username, current != null ? current.toCharArray() : new char[0]) == null) {
                ctx.status(HttpStatus.BAD_REQUEST).html(accountHtml(ctx, null, "The current password is wrong."));
                return;
            }
            long changed = users.changePassword(user.id, password.toCharArray());
            Auth.startSession(ctx, user, changed);
        } catch (LoginLockedException e) {
            ctx.status(HttpStatus.TOO_MANY_REQUESTS).html(accountHtml(ctx, null, e.getMessage()));
            return;
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST).html(accountHtml(ctx, null, e.getMessage()));
            return;
        }
        Auth.flash(ctx, "The password was changed.");
        ctx.redirect("/account");
    }
}
