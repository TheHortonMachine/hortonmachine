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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.hortonmachine.cli.ModuleDescriptor;
import org.hortonmachine.cli.ModuleDescriptor.DataType;
import org.hortonmachine.cli.ModuleDescriptor.Parameter;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMParameterKind;
import org.hortonmachine.server.jobs.Job;
import org.hortonmachine.server.jobs.JobManager;
import org.hortonmachine.server.jobs.JobValidationException;
import org.hortonmachine.server.users.User;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;

/**
 * The pages of the modules: the list and the form to run one.
 *
 * <p>The form is built from the {@link ModuleDescriptor}; empty fields are not sent, so the modules use
 * their defaults, shown as placeholders.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class ModulePages {
    private static final int SHORT_DESCRIPTION_LENGTH = 160;

    private final JobManager jobManager;
    private final FileSuggestions suggestions;
    private final Map<String, ModuleDescriptor> modules = new LinkedHashMap<>();

    public ModulePages( JobManager jobManager, FileSuggestions suggestions ) {
        this.jobManager = jobManager;
        this.suggestions = suggestions;
        for( Map.Entry<String, Class< ? extends HMModel>> entry : ModuleDescriptor.getAvailableModules().entrySet() ) {
            modules.put(entry.getKey(), ModuleDescriptor.of(entry.getValue()));
        }
    }

    public void register( JavalinConfig config ) {
        config.routes.get("/modules", this::listPage);
        config.routes.get("/modules/{name}", this::formPage);
        config.routes.post("/modules/{name}", this::submit);
    }

    private ModuleDescriptor getModule( Context ctx ) {
        ModuleDescriptor module = modules.get(ctx.pathParam("name"));
        if (module == null) {
            throw new NotFoundResponse("Unknown module.");
        }
        return module;
    }

    private void listPage( Context ctx ) {
        String query = ctx.queryParam("q");
        String lowerQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        Map<String, List<ModuleDescriptor>> byFolder = new TreeMap<>();
        for( ModuleDescriptor module : modules.values() ) {
            if (lowerQuery.isEmpty() || matches(module, lowerQuery)) {
                byFolder.computeIfAbsent(module.getFolder().isEmpty() ? "Other" : module.getFolder(), f -> new ArrayList<>())
                        .add(module);
            }
        }

        StringBuilder body = new StringBuilder("<h1>Modules</h1>");
        body.append("<form method=\"get\" action=\"/modules\" class=\"actions\">");
        body.append("<input type=\"text\" name=\"q\" placeholder=\"Search name, folder or keyword\" value=\"")
                .append(Html.escape(query)).append("\" aria-label=\"Search\">");
        body.append("<button type=\"submit\">Search</button></form>");
        if (byFolder.isEmpty()) {
            body.append("<p class=\"muted\">No module matches.</p>");
        }
        for( Map.Entry<String, List<ModuleDescriptor>> entry : byFolder.entrySet() ) {
            body.append("<h2>").append(Html.escape(entry.getKey())).append("</h2><div class=\"scroll\"><table>");
            for( ModuleDescriptor module : entry.getValue() ) {
                body.append("<tr><td><a href=\"/modules/").append(Html.escape(module.getName())).append("\">")
                        .append(Html.escape(module.getName())).append("</a>");
                if (module.getStatus().equals("experimental")) {
                    body.append(" <span class=\"muted\">experimental</span>");
                }
                body.append("</td><td class=\"muted\">").append(Html.escape(shortDescription(module.getDescription())))
                        .append("</td></tr>");
            }
            body.append("</table></div>");
        }
        ctx.html(Html.page("Modules", Auth.user(ctx), Auth.csrfToken(ctx), body.toString(), null));
    }

    private static boolean matches( ModuleDescriptor module, String lowerQuery ) {
        if (module.getName().toLowerCase(Locale.ROOT).contains(lowerQuery)
                || module.getFolder().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
            return true;
        }
        for( String keyword : module.getKeywords() ) {
            if (keyword.toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                return true;
            }
        }
        return false;
    }

    private static String shortDescription( String description ) {
        int end = description.indexOf(". ");
        String first = end > 0 ? description.substring(0, end + 1) : description;
        return first.length() > SHORT_DESCRIPTION_LENGTH ? first.substring(0, SHORT_DESCRIPTION_LENGTH - 1) + "…" : first;
    }

    private void formPage( Context ctx ) throws Exception {
        ModuleDescriptor module = getModule(ctx);
        User user = Auth.user(ctx);
        Map<String, String> values = new LinkedHashMap<>();
        // run again with the values of a job
        String fromJobId = ctx.queryParam("from");
        if (fromJobId != null && jobManager.canRead(user.id, user.isAdmin(), fromJobId)) {
            Job job = jobManager.getJob(fromJobId);
            if (job.module.equals(module.getName())) {
                values.putAll(job.parameters);
            }
        } else {
            Parameter single = singleOutput(module);
            if (single != null) {
                values.put(single.name, defaultOutputName(single));
            }
        }
        ctx.html(formHtml(ctx, module, values, Collections.emptyMap(), null));
    }

    /**
     * @return the only output path of the module, or <code>null</code> if there are none or more.
     */
    private static Parameter singleOutput( ModuleDescriptor module ) {
        Parameter single = null;
        for( Parameter parameter : module.getParameters() ) {
            if (parameter.kind == HMParameterKind.OUTPUT && parameter.isPath() && !parameter.isComputed) {
                if (single != null) {
                    return null;
                }
                single = parameter;
            }
        }
        return single;
    }

    /**
     * @return a file name for an output, as pit.tif for outPit.
     */
    static String defaultOutputName( Parameter parameter ) {
        String name = parameter.name.replaceFirst("^out(?=[A-Z])", "");
        name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        String extension;
        switch( parameter.dataType ) {
        case RASTER:
            extension = "tif";
            break;
        case VECTOR:
            extension = "gpkg";
            break;
        case LAS:
            extension = "las";
            break;
        case CSV:
            extension = "csv";
            break;
        case FILE:
            extension = parameter.extension;
            break;
        default:
            extension = null;
        }
        return extension != null ? name + "." + extension : name;
    }

    private void submit( Context ctx ) throws Exception {
        ModuleDescriptor module = getModule(ctx);
        User user = Auth.user(ctx);
        Map<String, String> values = new LinkedHashMap<>();
        for( Parameter parameter : module.getParameters() ) {
            if (parameter.isComputed) {
                continue;
            }
            List<String> posted = ctx.formParams(parameter.name);
            if (posted.isEmpty()) {
                continue;
            }
            // a checkbox comes after its hidden false
            String value = posted.get(posted.size() - 1);
            if (!value.isBlank()) {
                values.put(parameter.name, value);
            }
        }
        try {
            Job job = jobManager.submit(user.id, user.isAdmin(), module.getName(), values);
            ctx.redirect("/jobs/" + job.id);
        } catch (JobValidationException e) {
            ctx.status(HttpStatus.BAD_REQUEST).html(formHtml(ctx, module, values, e.getErrors(), "Some values are not valid."));
        }
    }

    private String formHtml( Context ctx, ModuleDescriptor module, Map<String, String> values, Map<String, String> errors,
            String error ) throws Exception {
        User user = Auth.user(ctx);
        StringBuilder body = new StringBuilder();
        body.append("<p><a href=\"/modules\">Modules</a> / ").append(Html.escape(module.getFolder())).append("</p>");
        body.append("<h1>").append(Html.escape(module.getName())).append("</h1>");
        body.append("<p>").append(Html.escape(module.getDescription())).append("</p>");
        if (!module.getAuthors().isEmpty()) {
            body.append("<p class=\"muted\">Authors: ").append(Html.escape(String.join(", ", module.getAuthors()))).append("</p>");
        }
        body.append("<p class=\"muted\">Inputs are paths in the workspace: <code>data/…</code> for the shared data, ")
                .append("<code>uploads/…</code> for your uploads, <code>jobs/&lt;id&gt;/…</code> for the outputs of your jobs. ")
                .append("Outputs are file names, written in the folder of the job. Empty fields use the default.</p>");
        if (error != null) {
            body.append("<p class=\"error\">").append(Html.escape(error)).append("</p>");
        }
        for( String name : errors.keySet() ) {
            if (module.getParameter(name) == null || module.getParameter(name).isComputed) {
                body.append("<p class=\"error\">").append(Html.escape(errors.get(name))).append("</p>");
            }
        }

        body.append("<form method=\"post\" action=\"/modules/").append(Html.escape(module.getName())).append("\">");
        body.append(Html.csrfField(Auth.csrfToken(ctx)));
        for( HMParameterKind kind : HMParameterKind.values() ) {
            List<Parameter> parameters = new ArrayList<>();
            for( Parameter parameter : module.getParameters() ) {
                if (parameter.kind == kind && !parameter.isComputed) {
                    parameters.add(parameter);
                }
            }
            if (parameters.isEmpty()) {
                continue;
            }
            body.append("<fieldset><legend>").append(Html.escape(kind.getTitle())).append("</legend>");
            for( Parameter parameter : parameters ) {
                appendField(body, user, parameter, values.get(parameter.name), errors.get(parameter.name));
            }
            body.append("</fieldset>");
        }
        body.append("<p><button type=\"submit\">Run</button></p></form>");
        return Html.page(module.getName(), user, Auth.csrfToken(ctx), body.toString(), null);
    }

    private void appendField( StringBuilder body, User user, Parameter parameter, String value, String error ) throws Exception {
        String id = "p-" + parameter.name;
        String name = Html.escape(parameter.name);
        body.append("<div class=\"field\">");
        if (parameter.dataType == DataType.BOOLEAN) {
            boolean checked = value != null ? value.equalsIgnoreCase("true") : Boolean.TRUE.equals(parameter.defaultValue);
            body.append("<label><input type=\"hidden\" name=\"").append(name).append("\" value=\"false\">");
            body.append("<input type=\"checkbox\" id=\"").append(id).append("\" name=\"").append(name).append("\" value=\"true\"")
                    .append(checked ? " checked" : "").append("> ").append(name).append("</label>");
        } else {
            body.append("<label for=\"").append(id).append("\">").append(name).append("</label>");
            appendInput(body, user, parameter, id, value);
        }
        if (error != null) {
            body.append("<p class=\"error help\">").append(Html.escape(error)).append("</p>");
        }
        body.append("<p class=\"help muted\">").append(Html.escape(details(parameter))).append("</p></div>");
    }

    private void appendInput( StringBuilder body, User user, Parameter parameter, String id, String value ) throws Exception {
        String name = Html.escape(parameter.name);
        String placeholder = parameter.defaultValue != null && !parameter.defaultValue.getClass().isArray()
                ? Html.escape(parameter.defaultValue.toString())
                : "";
        String valueAttribute = value != null ? " value=\"" + Html.escape(value) + "\"" : "";
        switch( parameter.dataType ) {
        case CHOICE: {
            String selected = value != null ? value : parameter.defaultValue != null ? parameter.defaultValue.toString() : null;
            body.append("<select id=\"").append(id).append("\" name=\"").append(name).append("\">");
            if (selected == null) {
                body.append("<option value=\"\">(choose)</option>");
            }
            for( String choice : parameter.choices ) {
                body.append("<option").append(choice.equals(selected) ? " selected" : "").append(">").append(Html.escape(choice))
                        .append("</option>");
            }
            body.append("</select>");
            break;
        }
        case NUMBER:
        case INTEGER:
            body.append("<input type=\"number\" id=\"").append(id).append("\" name=\"").append(name).append("\" step=\"")
                    .append(parameter.dataType == DataType.INTEGER ? "1" : "any").append("\"");
            if (parameter.min != null) {
                body.append(" min=\"").append(parameter.min).append("\"");
            }
            if (parameter.max != null) {
                body.append(" max=\"").append(parameter.max).append("\"");
            }
            body.append(" placeholder=\"").append(placeholder).append("\"").append(valueAttribute).append(">");
            break;
        case TEXT:
            body.append("<textarea id=\"").append(id).append("\" name=\"").append(name).append("\" rows=\"4\" placeholder=\"")
                    .append(placeholder).append("\">").append(Html.escape(value)).append("</textarea>");
            break;
        case OTHER:
            body.append("<input type=\"text\" id=\"").append(id).append("\" disabled placeholder=\"not supported on the server\">");
            break;
        default:
            String listId = null;
            if (parameter.isPath() && parameter.kind != HMParameterKind.OUTPUT) {
                listId = "list-" + parameter.name;
                body.append("<datalist id=\"").append(Html.escape(listId)).append("\">");
                for( String suggestion : suggestions.suggest(user, parameter) ) {
                    body.append("<option value=\"").append(Html.escape(suggestion)).append("\">");
                }
                body.append("</datalist>");
            }
            if (parameter.dataType == DataType.CRS && placeholder.isEmpty()) {
                placeholder = "EPSG:32632";
            }
            body.append("<input type=\"text\" id=\"").append(id).append("\" name=\"").append(name).append("\"")
                    .append(listId != null ? " list=\"" + Html.escape(listId) + "\"" : "").append(" placeholder=\"")
                    .append(placeholder).append("\"").append(valueAttribute).append(">");
        }
    }

    private static String details( Parameter parameter ) {
        StringBuilder sb = new StringBuilder(parameter.description);
        sb.append(" [").append(parameter.dataType.getName()).append("]");
        if (parameter.unit != null) {
            sb.append(" Unit: ").append(parameter.unit).append(".");
        }
        if (parameter.dataType == DataType.NUMBERS) {
            sb.append(" Numbers separated by commas.");
        }
        return sb.toString().trim();
    }
}
