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
    private final Templates templates;
    // changed by the settings page while running
    private volatile int maxUploadMb;

    /**
     * @param uploads the uploads.
     * @param maxUploadMb the maximum size of an upload, shown in the form.
     */
    public UploadPages( UploadManager uploads, int maxUploadMb, Templates templates ) {
        this.uploads = uploads;
        this.maxUploadMb = maxUploadMb;
        this.templates = templates;
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
        config.routes.head("/uploads/files/<path>", this::download);
    }

    /**
     * Refuse the forms larger than an upload can be, before they are read, with a page that tells why.
     * The forms sent without length are stopped by the limits of the multipart configuration.
     */
    private void checkSize( Context ctx ) {
        long maxBytes = maxUploadMb * 1024L * 1024L;
        if (ctx.method() == HandlerType.POST && ctx.isMultipartFormData() && ctx.req().getContentLengthLong() > maxBytes) {
            ctx.status(HttpStatus.CONTENT_TOO_LARGE).html(templates.messagePage(ctx, "Upload",
                    "The upload is larger than " + maxUploadMb + " MB: send fewer files at a time.", "/uploads", "Back to the uploads"));
            ctx.skipRemainingHandlers();
        }
    }

    private void listPage( Context ctx ) throws Exception {
        ctx.html(listHtml(ctx, Auth.takeFlash(ctx), null, ""));
    }

    private String listHtml( Context ctx, String message, String error, String folder ) throws Exception {
        User user = Auth.user(ctx);
        List<Model> list = new ArrayList<>();
        for( Upload upload : uploads.list(user.id) ) {
            list.add(Model.of("path", upload.path, "workspacePath", upload.getWorkspacePath(), "isFolder", upload.isFolder, //
                    "href", "/uploads/files/" + Html.urlPath(upload.path), "size", upload.isFolder ? "" : Html.formatSize(upload.size)));
        }
        Model model = Model.of("used", Html.formatSize(uploads.getUsedBytes(user.id)), "hasQuota", uploads.getQuotaBytes() > 0, //
                "quota", Html.formatSize(uploads.getQuotaBytes()), "message", message, "error", error, //
                "filesField", FILES_FIELD, "maxUploadMb", maxUploadMb, "folder", folder, //
                "uploads", list, "hasUploads", !list.isEmpty());
        return templates.page(ctx, "uploads.html", "Uploads", model);
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
