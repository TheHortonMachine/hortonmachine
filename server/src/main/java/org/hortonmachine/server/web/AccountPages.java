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

import org.hortonmachine.server.users.LoginLockedException;
import org.hortonmachine.server.users.User;
import org.hortonmachine.server.users.UserManager;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

/**
 * The page of the account of the logged in user, to change the password.
 *
 * <p>The current password is asked, and its failures count as failed logins. After the change the other
 * sessions of the user end, this one continues.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class AccountPages {
    private final UserManager users;

    public AccountPages( UserManager users ) {
        this.users = users;
    }

    public void register( JavalinConfig config ) {
        config.routes.get("/account", ctx -> ctx.html(accountHtml(ctx, Auth.takeFlash(ctx), null)));
        config.routes.post("/account/password", this::changePassword);
    }

    private static String accountHtml( Context ctx, String message, String error ) {
        User user = Auth.user(ctx);
        String csrfToken = Auth.csrfToken(ctx);
        StringBuilder body = new StringBuilder("<h1>Account</h1><table>");
        body.append("<tr><th>User name</th><td>").append(Html.escape(user.username)).append("</td></tr>");
        body.append("<tr><th>Role</th><td>").append(user.role.name().toLowerCase()).append("</td></tr>");
        body.append("<tr><th>Created</th><td>").append(Html.formatTime(user.created)).append("</td></tr></table>");
        if (message != null) {
            body.append("<p class=\"ok\">").append(Html.escape(message)).append("</p>");
        }
        if (error != null) {
            body.append("<p class=\"error\">").append(Html.escape(error)).append("</p>");
        }
        body.append("<fieldset><legend>Change the password</legend>");
        body.append("<form method=\"post\" action=\"/account/password\">").append(Html.csrfField(csrfToken));
        body.append("<label for=\"current\">Current password</label>");
        body.append("<input type=\"password\" id=\"current\" name=\"current\" autocomplete=\"current-password\" required>");
        body.append("<label for=\"password\">New password</label>");
        body.append("<input type=\"password\" id=\"password\" name=\"password\" autocomplete=\"new-password\" required minlength=\"")
                .append(UserManager.MIN_PASSWORD_LENGTH).append("\">");
        body.append("<label for=\"again\">New password again</label>");
        body.append("<input type=\"password\" id=\"again\" name=\"again\" autocomplete=\"new-password\" required>");
        body.append("<p class=\"help muted\">At least ").append(UserManager.MIN_PASSWORD_LENGTH)
                .append(" characters. Your other sessions end.</p>");
        body.append("<p><button type=\"submit\">Change</button></p></form></fieldset>");
        return Html.page("Account", user, csrfToken, body.toString(), null);
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
