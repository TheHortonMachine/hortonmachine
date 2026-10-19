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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.hortonmachine.server.WorkspacePaths;
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
    private final Templates templates;
    private final Map<String, ModuleDescriptor> modules = new LinkedHashMap<>();

    public ModulePages( JobManager jobManager, FileSuggestions suggestions, Templates templates ) {
        this.jobManager = jobManager;
        this.suggestions = suggestions;
        this.templates = templates;
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

        // grouped by folder, or one alphabetic list
        boolean isList = "list".equals(ctx.queryParam("view"));
        List<Model> folders = new ArrayList<>();
        List<Model> all = new ArrayList<>();
        for( Map.Entry<String, List<ModuleDescriptor>> entry : byFolder.entrySet() ) {
            List<Model> folderModules = new ArrayList<>();
            for( ModuleDescriptor module : entry.getValue() ) {
                Model moduleModel = Model.of("name", module.getName(), "folder", entry.getKey(), //
                        "experimental", module.getStatus().equals("experimental"), //
                        "description", shortDescription(module.getDescription()));
                folderModules.add(moduleModel);
                all.add(moduleModel);
            }
            folders.add(Model.of("name", entry.getKey(), "modules", folderModules));
        }
        all.sort(Comparator.comparing(module -> ((String) module.get("name")).toLowerCase(Locale.ROOT)));

        String q = lowerQuery.isEmpty() ? "" : "q=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        Model model = Model.of("query", query, "isList", isList, "hasModules", !all.isEmpty(), "folders", folders, //
                "modules", all, "groupsHref", "/modules" + (q.isEmpty() ? "" : "?" + q), //
                "listHref", "/modules?view=list" + (q.isEmpty() ? "" : "&" + q));
        ctx.html(templates.page(ctx, "modules.html", "Modules", model));
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

    static String shortDescription( String description ) {
        int end = description.indexOf(". ");
        String first = end > 0 ? description.substring(0, end + 1) : description;
        return first.length() > SHORT_DESCRIPTION_LENGTH ? first.substring(0, SHORT_DESCRIPTION_LENGTH - 1) + "…" : first;
    }

    private void formPage( Context ctx ) throws Exception {
        ModuleDescriptor module = getModule(ctx);
        User user = Auth.user(ctx);
        Map<String, String> values = new LinkedHashMap<>();
        String outputFolder = "";
        // run again with the values of a job
        String fromJobId = ctx.queryParam("from");
        if (fromJobId != null && jobManager.canRead(user.id, user.isAdmin(), fromJobId)) {
            Job job = jobManager.getJob(fromJobId);
            if (job.module.equals(module.getName())) {
                values.putAll(job.parameters);
                outputFolder = job.outputFolder != null ? job.outputFolder : "";
            }
        } else {
            Parameter single = singleOutput(module);
            if (single != null) {
                values.put(single.name, defaultOutputName(single));
            }
        }
        ctx.html(formHtml(ctx, module, values, Collections.emptyMap(), null, outputFolder, false));
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
        if (parameter.dataType == DataType.VECTOR) {
            // the modules write a geopackage in a table
            return name + "." + extension + "#" + name;
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
        String outputFolder = ctx.formParam(JobManager.OUTPUT_FOLDER);
        outputFolder = outputFolder != null ? outputFolder.trim() : "";
        boolean overwrite = ctx.formParam("overwrite") != null;
        try {
            Job job = jobManager.submit(user.id, user.isAdmin(), module.getName(), values, outputFolder, overwrite);
            ctx.redirect("/jobs/" + job.id);
        } catch (JobValidationException e) {
            String error = e.isConflict() ? "Some outputs would replace existing files: tick overwrite to replace them."
                    : "Some values are not valid.";
            ctx.status(e.isConflict() ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST)
                    .html(formHtml(ctx, module, values, e.getErrors(), error, outputFolder, overwrite));
        }
    }

    private String formHtml( Context ctx, ModuleDescriptor module, Map<String, String> values, Map<String, String> errors,
            String error, String outputFolder, boolean overwrite ) throws Exception {
        User user = Auth.user(ctx);
        List<String> otherErrors = new ArrayList<>();
        for( String name : errors.keySet() ) {
            if (!name.equals(JobManager.OUTPUT_FOLDER) && (module.getParameter(name) == null || module.getParameter(name).isComputed)) {
                otherErrors.add(errors.get(name));
            }
        }
        List<Model> groups = new ArrayList<>();
        for( HMParameterKind kind : HMParameterKind.values() ) {
            List<Model> fields = new ArrayList<>();
            for( Parameter parameter : module.getParameters() ) {
                if (parameter.kind == kind && !parameter.isComputed) {
                    fields.add(fieldModel(user, parameter, values.get(parameter.name), errors.get(parameter.name)));
                }
            }
            if (!fields.isEmpty()) {
                groups.add(Model.of("title", kind.getTitle(), "fields", fields));
            }
        }
        List<Model> folderSuggestions = new ArrayList<>();
        for( String folder : suggestions.suggestOutputFolders(user) ) {
            folderSuggestions.add(Model.of("value", folder));
        }
        Model model = Model.of("name", module.getName(), "folder", module.getFolder(), "description", module.getDescription(),
                "authors", String.join(", ", module.getAuthors()), "error", error, "otherErrors", otherErrors, "groups", groups,
                "hasOutputs", hasOutputFiles(module), //
                "outputFolderName", JobManager.OUTPUT_FOLDER, "outputFolder", outputFolder, //
                "outputFolderError", errors.get(JobManager.OUTPUT_FOLDER), "outputFolders", folderSuggestions, //
                "defaultOutputFolder", WorkspacePaths.DATA + "/" + user.username + "/…", "overwrite", overwrite);
        return templates.page(ctx, "module.html", module.getName(), model);
    }

    /**
     * @return <code>true</code> if the module writes files.
     */
    private static boolean hasOutputFiles( ModuleDescriptor module ) {
        for( Parameter parameter : module.getParameters() ) {
            if (parameter.kind == HMParameterKind.OUTPUT && parameter.isPath() && !parameter.isComputed) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the model of the field of a parameter: the kind of input is told by one of isBoolean, isChoice,
     *         isNumber, isText, isUnsupported, isPlain.
     */
    private Model fieldModel( User user, Parameter parameter, String value, String error ) throws Exception {
        String placeholder = parameter.defaultValue != null && !parameter.defaultValue.getClass().isArray()
                ? parameter.defaultValue.toString()
                : "";
        DataType type = parameter.dataType;
        boolean isBoolean = type == DataType.BOOLEAN;
        boolean isChoice = type == DataType.CHOICE;
        boolean isNumber = type == DataType.NUMBER || type == DataType.INTEGER;
        boolean isText = type == DataType.TEXT;
        boolean isUnsupported = type == DataType.OTHER;
        // every value is set, not to take the one of the module with the same name
        Model field = Model.of("id", "p-" + parameter.name, "name", parameter.name, "value", value != null ? value : "", //
                "hasValue", value != null, "fieldError", error, "help", details(parameter), //
                "isBoolean", isBoolean, "isChoice", isChoice, "isNumber", isNumber, "isText", isText, "isUnsupported", isUnsupported,
                "isPlain", !isBoolean && !isChoice && !isNumber && !isText && !isUnsupported, "hasSuggestions", false);

        switch( type ) {
        case BOOLEAN:
            field.with("checked", value != null ? value.equalsIgnoreCase("true") : Boolean.TRUE.equals(parameter.defaultValue));
            break;
        case CHOICE: {
            String selected = value != null ? value : parameter.defaultValue != null ? parameter.defaultValue.toString() : null;
            List<Model> choices = new ArrayList<>();
            for( String choice : parameter.choices ) {
                choices.add(Model.of("label", choice, "selected", choice.equals(selected)));
            }
            field.with("noSelection", selected == null).with("choices", choices);
            break;
        }
        case NUMBER:
        case INTEGER:
            field.with("step", type == DataType.INTEGER ? "1" : "any").with("hasMin", parameter.min != null)
                    .with("min", String.valueOf(parameter.min)).with("hasMax", parameter.max != null)
                    .with("max", String.valueOf(parameter.max));
            break;
        default:
            if (parameter.isPath() && parameter.kind != HMParameterKind.OUTPUT) {
                List<Model> suggestionModels = new ArrayList<>();
                for( FileSuggestions.Suggestion suggestion : suggestions.suggest(user, parameter) ) {
                    suggestionModels.add(Model.of("value", suggestion.value, "label", suggestion.label));
                }
                field.with("hasSuggestions", true).with("listId", "list-" + parameter.name).with("suggestions", suggestionModels);
            }
            if (type == DataType.CRS && placeholder.isEmpty()) {
                placeholder = "EPSG:32632";
            }
        }
        return field.with("placeholder", placeholder);
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
