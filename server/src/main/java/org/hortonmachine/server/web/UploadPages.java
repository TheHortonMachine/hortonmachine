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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hortonmachine.server.uploads.UploadManager;
import org.hortonmachine.server.uploads.UploadManager.Upload;
import org.hortonmachine.server.users.User;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UploadedFile;

/**
 * The pages of the uploads of a user: the list, the upload form, the downloads and the deletes.
 *
 * <p>The uploaded files are given to the modules as <code>uploads/...</code>; the zip files can be unpacked.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class UploadPages {
    /** The field of the files in the upload form. */
    public static final String FILES_FIELD = "files";

    private final UploadManager uploads;
    // changed by the settings page while running
    private volatile int maxUploadMb;

    /**
     * @param uploads the uploads.
     * @param maxUploadMb the maximum size of an upload, shown in the form.
     */
    public UploadPages( UploadManager uploads, int maxUploadMb ) {
        this.uploads = uploads;
        this.maxUploadMb = maxUploadMb;
    }

    /**
     * Change the maximum size of an upload, for the next uploads.
     */
    public void setMaxUploadMb( int maxUploadMb ) {
        this.maxUploadMb = maxUploadMb;
    }

    /**
     * Add the check of the size of the uploads. To register before {@link Auth}, which reads the form.
     */
    public void registerSizeCheck( JavalinConfig config ) {
        config.routes.before(this::checkSize);
    }

    public void register( JavalinConfig config ) {
        config.routes.get("/uploads", this::listPage);
        config.routes.post("/uploads", this::upload);
        config.routes.post("/uploads/delete", this::delete);
        config.routes.get("/uploads/files/<path>", this::download);
    }

    /**
     * Refuse the forms larger than an upload can be, before they are read, with a page that tells why.
     * The forms sent without length are stopped by the limits of the multipart configuration.
     */
    private void checkSize( Context ctx ) {
        long maxBytes = maxUploadMb * 1024L * 1024L;
        if (ctx.method() == HandlerType.POST && ctx.isMultipartFormData() && ctx.req().getContentLengthLong() > maxBytes) {
            String body = "<h1>Upload</h1><p class=\"error\">The upload is larger than " + maxUploadMb
                    + " MB: send fewer files at a time.</p><p><a href=\"/uploads\">Back to the uploads</a></p>";
            ctx.status(HttpStatus.CONTENT_TOO_LARGE).html(Html.page("Upload", null, null, body, null));
            ctx.skipRemainingHandlers();
        }
    }

    private void listPage( Context ctx ) throws Exception {
        ctx.html(listHtml(ctx, Auth.takeFlash(ctx), null, ""));
    }

    private String listHtml( Context ctx, String message, String error, String folder ) throws Exception {
        User user = Auth.user(ctx);
        String csrfToken = Auth.csrfToken(ctx);
        List<Upload> list = uploads.list(user.id);
        long used = uploads.getUsedBytes(user.id);

        StringBuilder body = new StringBuilder("<h1>Uploads</h1>");
        body.append("<p class=\"muted\">Your files, for the inputs of the modules as <code>uploads/...</code>. ");
        body.append("In use ").append(Html.formatSize(used));
        if (uploads.getQuotaBytes() > 0) {
            body.append(" of ").append(Html.formatSize(uploads.getQuotaBytes()));
        }
        body.append(".</p>");
        if (message != null) {
            body.append("<p class=\"ok\">").append(Html.escape(message)).append("</p>");
        }
        if (error != null) {
            body.append("<p class=\"error\">").append(Html.escape(error)).append("</p>");
        }

        body.append("<fieldset><legend>Upload</legend>");
        body.append("<form method=\"post\" action=\"/uploads\" enctype=\"multipart/form-data\">").append(Html.csrfField(csrfToken));
        body.append("<label for=\"files\">Files</label>");
        body.append("<input type=\"file\" id=\"files\" name=\"").append(FILES_FIELD).append("\" multiple required>");
        body.append("<p class=\"help muted\">At most ").append(maxUploadMb)
                .append(" MB at a time. A shapefile needs all its files, or a zip file with them.</p>");
        body.append("<label for=\"folder\">Folder</label>");
        body.append("<input type=\"text\" id=\"folder\" name=\"folder\" placeholder=\"optional, as dtm/2024\" value=\"")
                .append(Html.escape(folder)).append("\">");
        body.append("<p><label class=\"inline\"><input type=\"checkbox\" name=\"unzip\" checked> Unpack the zip files</label></p>");
        body.append("<p><button type=\"submit\">Upload</button></p></form></fieldset>");

        if (list.isEmpty()) {
            body.append("<p class=\"muted\">No uploads yet.</p>");
        } else {
            body.append("<div class=\"scroll\"><table><tr><th>Path</th><th class=\"num\">Size</th><th></th></tr>");
            for( Upload upload : list ) {
                body.append("<tr><td>");
                if (upload.isFolder) {
                    body.append(Html.escape(upload.getWorkspacePath())).append("/");
                } else {
                    body.append("<a href=\"/uploads/files/").append(Html.escape(Html.urlPath(upload.path))).append("\">")
                            .append(Html.escape(upload.getWorkspacePath())).append("</a>");
                }
                body.append("</td><td class=\"num\">").append(upload.isFolder ? "" : Html.formatSize(upload.size)).append("</td><td>");
                body.append("<form class=\"inline\" method=\"post\" action=\"/uploads/delete\">").append(Html.csrfField(csrfToken));
                body.append("<input type=\"hidden\" name=\"path\" value=\"").append(Html.escape(upload.path)).append("\">");
                body.append("<button class=\"link\" type=\"submit\">Delete</button></form></td></tr>");
            }
            body.append("</table></div>");
        }
        return Html.page("Uploads", user, csrfToken, body.toString(), null);
    }

    private void upload( Context ctx ) throws Exception {
        User user = Auth.user(ctx);
        String folder = ctx.formParam("folder");
        folder = folder != null ? folder : "";
        boolean unzip = ctx.formParam("unzip") != null;
        List<UploadedFile> files = new ArrayList<>();
        for( UploadedFile file : ctx.uploadedFiles(FILES_FIELD) ) {
            // an empty part when no file is chosen
            if (file.filename() != null && !file.filename().isEmpty()) {
                files.add(file);
            }
        }
        if (files.isEmpty()) {
            ctx.status(HttpStatus.BAD_REQUEST).html(listHtml(ctx, null, "Choose the files to upload.", folder));
            return;
        }

        int count = 0;
        try {
            for( UploadedFile file : files ) {
                try (InputStream content = file.content()) {
                    if (unzip && file.filename().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                        count += uploads.unzip(user.id, folder, file.filename(), content).size();
                    } else {
                        uploads.save(user.id, folder, file.filename(), file.size(), content);
                        count++;
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            String done = count > 0 ? count + " file(s) uploaded before the error." : null;
            ctx.status(HttpStatus.BAD_REQUEST).html(listHtml(ctx, done, e.getMessage(), folder));
            return;
        }
        Auth.flash(ctx, count + " file(s) uploaded.");
        ctx.redirect("/uploads");
    }

    private void delete( Context ctx ) throws Exception {
        User user = Auth.user(ctx);
        String path = ctx.formParam("path");
        try {
            if (path == null || !uploads.delete(user.id, path)) {
                throw new NotFoundResponse("The file doesn't exist.");
            }
        } catch (IllegalArgumentException e) {
            throw new NotFoundResponse("The file doesn't exist.");
        }
        ctx.redirect("/uploads");
    }

    private void download( Context ctx ) throws Exception {
        User user = Auth.user(ctx);
        File file;
        try {
            file = uploads.getFile(user.id, ctx.pathParam("path"));
        } catch (IllegalArgumentException e) {
            throw new NotFoundResponse("The file doesn't exist.");
        }
        if (!file.isFile()) {
            throw new NotFoundResponse("The file doesn't exist.");
        }
        Downloads.send(ctx, file.toPath(), false);
    }
}
