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

import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.files.FileOrigin;
import org.hortonmachine.server.files.FileOrigins;
import org.hortonmachine.server.uploads.UploadManager;
import org.hortonmachine.server.uploads.UploadManager.Upload;
import org.hortonmachine.server.users.User;
import org.hortonmachine.server.users.UserManager;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UploadedFile;

/**
 * The pages of the files of an area of the data: the list with the origin of each file, the upload form, the downloads
 * and the deletes.
 *
 * <p>A user sees the own area and, read only, the shared one; an administrator chooses any area with the
 * <code>area</code> parameter. The files are given to the modules as <code>data/&lt;area&gt;/...</code>; the zip files
 * can be unpacked.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class UploadPages {
    /** The field of the files in the upload form. */
    public static final String FILES_FIELD = "files";

    private final UploadManager uploads;
    private final FileOrigins origins;
    private final UserManager users;
    private final Templates templates;
    // changed by the settings page while running
    private volatile int maxUploadMb;

    /**
     * @param uploads the files of the areas.
     * @param maxUploadMb the maximum size of an upload, shown in the form.
     */
    public UploadPages( UploadManager uploads, FileOrigins origins, UserManager users, int maxUploadMb, Templates templates ) {
        this.uploads = uploads;
        this.origins = origins;
        this.users = users;
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
                    "The upload is larger than " + maxUploadMb + " MB: send fewer files at a time.", "/uploads", "Back to the files"));
            ctx.skipRemainingHandlers();
        }
    }

    /**
     * @param area the area asked, <code>null</code> for the own one.
     * @param write <code>true</code> if the area is written.
     * @return the area, if the user can read it, or write it.
     * @throws NotFoundResponse if not.
     */
    private static String checkArea( User user, String area, boolean write ) {
        String checked = area == null || area.isBlank() ? user.username : area;
        if (!WorkspacePaths.isArea(checked) || (write ? !WorkspacePaths.canWrite(checked, user.username, user.isAdmin())
                : !WorkspacePaths.canRead(checked, user.username, user.isAdmin()))) {
            throw new NotFoundResponse("The area doesn't exist.");
        }
        return checked;
    }

    private static String listHref( String area, User user ) {
        return "/uploads" + (area.equals(user.username) ? "" : "?area=" + area);
    }

    private void listPage( Context ctx ) throws Exception {
        String area = checkArea(Auth.user(ctx), ctx.queryParam("area"), false);
        ctx.html(listHtml(ctx, area, Auth.takeFlash(ctx), null, ""));
    }

    private String listHtml( Context ctx, String area, String message, String error, String folder ) throws Exception {
        User user = Auth.user(ctx);
        List<Model> list = new ArrayList<>();
        for( Upload upload : uploads.list(area) ) {
            list.add(Model.of("path", upload.path, "workspacePath", upload.getWorkspacePath(), "isFolder", upload.isFolder, //
                    "href", "/uploads/files/" + Html.urlPath(area + "/" + upload.path), //
                    "size", upload.isFolder ? "" : Html.formatSize(upload.size), //
                    "origin", upload.isFolder ? "" : originLabel(upload.getWorkspacePath())));
        }
        // the areas to choose: the own and the shared one, all for the administrators
        List<Model> areas = new ArrayList<>();
        areas.add(Model.of("label", "Your files", "href", "/uploads", "isCurrent", area.equals(user.username)));
        areas.add(Model.of("label", "Shared", "href", "/uploads?area=" + WorkspacePaths.SHARED, "isCurrent",
                area.equals(WorkspacePaths.SHARED)));
        if (user.isAdmin()) {
            for( User other : users.listUsers() ) {
                if (!other.id.equals(user.id)) {
                    areas.add(Model.of("label", other.username, "href", "/uploads?area=" + other.username, "isCurrent",
                            area.equals(other.username)));
                }
            }
        }
        boolean canWrite = WorkspacePaths.canWrite(area, user.id, user.isAdmin());
        boolean isUserArea = !area.equals(WorkspacePaths.SHARED);
        Model model = Model.of("area", area, "areaPath", UploadManager.workspacePath(area, ""), "areas", areas, //
                "canWrite", canWrite, "used", Html.formatSize(uploads.getUsedBytes(area)), //
                "hasQuota", isUserArea && uploads.getQuotaBytes() > 0, "quota", Html.formatSize(uploads.getQuotaBytes()), //
                "message", message, "error", error, "filesField", FILES_FIELD, "maxUploadMb", maxUploadMb, "folder", folder, //
                "uploads", list, "hasUploads", !list.isEmpty());
        return templates.page(ctx, "uploads.html", "Files", model);
    }

    /**
     * @return where a file comes from, as shown in the list.
     */
    private String originLabel( String workspacePath ) throws Exception {
        FileOrigin origin = origins.get(workspacePath);
        if (origin == null) {
            return "";
        }
        String who = origin.userId != null ? userName(origin.userId) : null;
        String label = origin.origin.equals(FileOrigin.PROCESSED) ? origin.module : "uploaded";
        label += (who != null ? " by " + who : "") + ", " + Html.formatTime(origin.created);
        if (!origin.history.isEmpty()) {
            label += " (replaced " + origin.history.size() + ")";
        }
        return label;
    }

    private String userName( String userId ) throws Exception {
        User user = users.getUser(userId);
        return user != null ? user.username : null;
    }

    private void upload( Context ctx ) throws Exception {
        User user = Auth.user(ctx);
        String area = checkArea(user, ctx.formParam("area"), true);
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
            ctx.status(HttpStatus.BAD_REQUEST).html(listHtml(ctx, area, null, "Choose the files to upload.", folder));
            return;
        }

        int count = 0;
        try {
            for( UploadedFile file : files ) {
                try (InputStream content = file.content()) {
                    if (unzip && file.filename().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                        count += uploads.unzip(area, user.id, folder, file.filename(), content).size();
                    } else {
                        uploads.save(area, user.id, folder, file.filename(), file.size(), content);
                        count++;
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            String done = count > 0 ? count + " file(s) uploaded before the error." : null;
            ctx.status(HttpStatus.BAD_REQUEST).html(listHtml(ctx, area, done, e.getMessage(), folder));
            return;
        }
        Auth.flash(ctx, count + " file(s) uploaded.");
        ctx.redirect(listHref(area, user));
    }

    private void delete( Context ctx ) throws Exception {
        User user = Auth.user(ctx);
        String area = checkArea(user, ctx.formParam("area"), true);
        String path = ctx.formParam("path");
        try {
            if (path == null || !uploads.delete(area, path)) {
                throw new NotFoundResponse("The file doesn't exist.");
            }
        } catch (IllegalArgumentException e) {
            throw new NotFoundResponse("The file doesn't exist.");
        }
        ctx.redirect(listHref(area, user));
    }

    /**
     * A file, as area/path.
     */
    private void download( Context ctx ) throws Exception {
        User user = Auth.user(ctx);
        String path = ctx.pathParam("path");
        int slash = path.indexOf('/');
        File file;
        try {
            String area = checkArea(user, slash > 0 ? path.substring(0, slash) : "-", false);
            file = uploads.getFile(area, path.substring(slash + 1));
        } catch (IllegalArgumentException e) {
            throw new NotFoundResponse("The file doesn't exist.");
        }
        if (!file.isFile()) {
            throw new NotFoundResponse("The file doesn't exist.");
        }
        Downloads.send(ctx, file.toPath(), false);
    }
}
