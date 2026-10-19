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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hortonmachine.Version;
import org.hortonmachine.cli.ModuleDescriptor;
import org.hortonmachine.cli.ModuleDescriptor.DataType;
import org.hortonmachine.cli.ModuleDescriptor.Parameter;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMParameterKind;
import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.jobs.Job;
import org.hortonmachine.server.jobs.JobManager;
import org.hortonmachine.server.jobs.JobState;
import org.hortonmachine.server.jobs.JobValidationException;
import org.hortonmachine.server.users.User;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.Handler;

/**
 * The modules and the jobs as OGC API - Processes - Part 1: Core 1.0 (https://docs.ogc.org/is/18-062r2/18-062r2.html),
 * under {@value Auth#API_PREFIX}.
 *
 * <p>Each module is a process, executed always asynchronously as a job of the user of the token, with the same
 * rules as the pages:
 * <ul>
 * <li>the inputs that are files are paths in the data (<code>data/shared/...</code>, <code>data/&lt;username&gt;/...</code>)
 *     or the hrefs of the files of this server, as the results of the jobs,
 *     <code>{"href": ".../api/data/files/&lt;username&gt;/dem/pit.tif"}</code>, to chain the jobs;</li>
 * <li>the files of the outputs get the default names, as <code>pit.tif</code> for <code>outPit</code>; the
 *     <code>outputs</code> of the request choose which ones are written, all if missing; they go in the folder of the
 *     first input in the area of the user, or in <code>outputFolder</code> of the request; files are replaced only
 *     with <code>"overwrite": true</code>, else the request is answered with 409;</li>
 * <li>the results are given by reference, as hrefs to the files, and the computed values inline; the hrefs are
 *     links signed by the {@link UrlSigner} if they are on, so that GDAL or QGIS can open them without the token;</li>
 * <li>DELETE of a job kills it if it is queued or running, else deletes it with its files.</li>
 * </ul>
 * A job of another user answers as not existing, as in the pages.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class OgcApi {
    private static final Logger LOGGER = Logger.getLogger(OgcApi.class.getName());

    private static final String API = Auth.API_PREFIX;
    private static final String JSON = "application/json";
    private static final String CONFORMANCE_PREFIX = "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/";
    private static final List<String> CONFORMANCE = List.of("core", "ogc-process-description", "json", "job-list", "dismiss");
    private static final String REL_PREFIX = "http://www.opengis.net/def/rel/ogc/1.0/";
    /** The jobs given at most by the list of the jobs. */
    public static final int JOBS_LIMIT = 1000;

    private final JobManager jobManager;
    private final UrlSigner signer;
    private final Templates templates;
    private final Map<String, ModuleDescriptor> modules = new LinkedHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public OgcApi( JobManager jobManager, UrlSigner signer, Templates templates ) {
        this.jobManager = jobManager;
        this.signer = signer;
        this.templates = templates;
        for( Map.Entry<String, Class< ? extends HMModel>> entry : ModuleDescriptor.getAvailableModules().entrySet() ) {
            modules.put(entry.getKey(), ModuleDescriptor.of(entry.getValue()));
        }
    }

    public void register( JavalinConfig config ) {
        config.routes.get(API, api(this::landingPage));
        config.routes.get(API + "/conformance", api(this::conformance));
        config.routes.get(API + "/docs", ctx -> ctx.html(templates.page(ctx, "api-docs.html", "API", Model.of("base", baseUrl(ctx)))));
        config.routes.get(API + "/processes", api(this::processList));
        config.routes.get(API + "/processes/{id}", api(this::processDescription));
        config.routes.post(API + "/processes/{id}/execution", api(this::execute));
        config.routes.get(API + "/jobs", api(this::jobList));
        config.routes.get(API + "/jobs/{id}", api(ctx -> sendJson(ctx, 200, statusInfo(ctx, getJob(ctx)))));
        config.routes.delete(API + "/jobs/{id}", api(this::dismiss));
        config.routes.get(API + "/jobs/{id}/results", api(this::results));
        config.routes.get(API + "/jobs/{id}" + Auth.FILES_PATH + "<path>", api(this::download));
        config.routes.head(API + "/jobs/{id}" + Auth.FILES_PATH + "<path>", api(this::download));
    }

    /**
     * @return the handler, answering its problems and errors as problem details.
     */
    private static Handler api( Handler handler ) {
        return ctx -> {
            try {
                handler.handle(ctx);
            } catch (ApiProblem problem) {
                problem.send(ctx);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in " + ctx.method() + " " + ctx.path(), e);
                new ApiProblem(500, "about:blank", "Internal server error", e.getMessage()).send(ctx);
            }
        };
    }

    /**
     * @return the url of the server as seen by the client, as http://host:8080.
     */
    static String baseUrl( Context ctx ) {
        String host = ctx.host();
        if (host == null || host.isEmpty()) {
            host = ctx.req().getServerName() + ":" + ctx.req().getServerPort();
        }
        return ctx.req().getScheme() + "://" + host;
    }

    private void sendJson( Context ctx, int status, JsonNode json ) throws Exception {
        ctx.status(status).contentType(JSON).result(mapper.writeValueAsString(json));
    }

    private ObjectNode link( String href, String rel, String type, String title ) {
        ObjectNode link = mapper.createObjectNode().put("href", href).put("rel", rel);
        if (type != null) {
            link.put("type", type);
        }
        if (title != null) {
            link.put("title", title);
        }
        return link;
    }

    private static String time( Long millis ) {
        return millis != null ? Instant.ofEpochMilli(millis).toString() : null;
    }

    private void landingPage( Context ctx ) throws Exception {
        String base = baseUrl(ctx) + API;
        ObjectNode landing = mapper.createObjectNode().put("title", "HortonMachine processes").put("description",
                "The modules of HortonMachine " + Version.getVersion() + " as OGC API - Processes.");
        ArrayNode links = landing.putArray("links");
        links.add(link(base, "self", JSON, "This document"));
        links.add(link(base + "/docs", "service-doc", "text/html", "The documentation of the api"));
        links.add(link(base + "/conformance", REL_PREFIX + "conformance", JSON, "The conformance classes"));
        links.add(link(base + "/processes", REL_PREFIX + "processes", JSON, "The processes"));
        links.add(link(base + "/jobs", REL_PREFIX + "job-list", JSON, "The jobs"));
        links.add(link(baseUrl(ctx) + StacApi.STAC, "related", JSON, "The STAC catalog of the data and uploads"));
        sendJson(ctx, 200, landing);
    }

    private void conformance( Context ctx ) throws Exception {
        ObjectNode conformance = mapper.createObjectNode();
        ArrayNode classes = conformance.putArray("conformsTo");
        for( String name : CONFORMANCE ) {
            classes.add(CONFORMANCE_PREFIX + name);
        }
        sendJson(ctx, 200, conformance);
    }

    private ModuleDescriptor getModule( Context ctx ) {
        ModuleDescriptor module = modules.get(ctx.pathParam("id"));
        if (module == null) {
            throw new ApiProblem(404, ApiProblem.NO_SUCH_PROCESS, "No such process", "Unknown process: " + ctx.pathParam("id"));
        }
        return module;
    }

    private ObjectNode processSummary( String base, ModuleDescriptor module ) {
        String href = base + "/processes/" + module.getName();
        ObjectNode summary = mapper.createObjectNode().put("id", module.getName()).put("title", module.getName())
                .put("description", ModulePages.shortDescription(module.getDescription())).put("version", Version.getVersion());
        ArrayNode keywords = summary.putArray("keywords");
        if (!module.getFolder().isEmpty()) {
            keywords.add(module.getFolder());
        }
        module.getKeywords().forEach(keywords::add);
        summary.putArray("jobControlOptions").add("async-execute").add("dismiss");
        summary.putArray("outputTransmission").add("value").add("reference");
        ArrayNode links = summary.putArray("links");
        links.add(link(href, "self", JSON, "The description of the process"));
        links.add(link(href + "/execution", REL_PREFIX + "execute", JSON, "Execute the process"));
        return summary;
    }

    private void processList( Context ctx ) throws Exception {
        String base = baseUrl(ctx) + API;
        ObjectNode list = mapper.createObjectNode();
        ArrayNode processes = list.putArray("processes");
        for( ModuleDescriptor module : modules.values() ) {
            processes.add(processSummary(base, module));
        }
        list.putArray("links").add(link(base + "/processes", "self", JSON, null));
        sendJson(ctx, 200, list);
    }

    /**
     * @return <code>true</code> if the parameter is an input of the process: set by the user and supported.
     */
    private static boolean isInput( Parameter parameter ) {
        return parameter.kind != HMParameterKind.OUTPUT && !parameter.isComputed && parameter.dataType != DataType.OTHER;
    }

    /**
     * @return <code>true</code> if the parameter is a file written by the module.
     */
    private static boolean isOutputFile( Parameter parameter ) {
        return parameter.kind == HMParameterKind.OUTPUT && parameter.isPath() && !parameter.isComputed;
    }

    private ObjectNode schema( Parameter parameter ) {
        ObjectNode schema = mapper.createObjectNode();
        switch( parameter.dataType ) {
        case BOOLEAN:
            schema.put("type", "boolean");
            break;
        case INTEGER:
        case NUMBER:
            schema.put("type", parameter.dataType == DataType.INTEGER ? "integer" : "number");
            if (parameter.min != null) {
                schema.put("minimum", parameter.min);
            }
            if (parameter.max != null) {
                schema.put("maximum", parameter.max);
            }
            break;
        case NUMBERS:
            schema.put("type", "array");
            schema.putObject("items").put("type", "number");
            break;
        case CHOICE:
            schema.put("type", "string");
            parameter.choices.forEach(schema.putArray("enum")::add);
            break;
        default:
            schema.put("type", "string");
            String mediaType = parameter.isPath() ? MediaTypes.forParameter(parameter) : null;
            if (mediaType != null && parameter.kind == HMParameterKind.OUTPUT) {
                schema.put("contentMediaType", mediaType);
            }
        }
        Object defaultValue = parameter.defaultValue;
        if (defaultValue != null && !defaultValue.getClass().isArray()) {
            schema.set("default", mapper.valueToTree(defaultValue));
        }
        return schema;
    }

    private void processDescription( Context ctx ) throws Exception {
        ModuleDescriptor module = getModule(ctx);
        ObjectNode description = processSummary(baseUrl(ctx) + API, module);
        description.put("description", module.getDescription());
        ObjectNode inputs = description.putObject("inputs");
        ObjectNode outputs = description.putObject("outputs");
        for( Parameter parameter : module.getParameters() ) {
            if (isInput(parameter)) {
                ObjectNode input = inputs.putObject(parameter.name).put("title", parameter.name).put("description",
                        parameter.description + (parameter.isPath()
                                ? " A path in the data (data/shared/..., data/<your username>/...) or {\"href\": \"<a file of this server>\"}."
                                : "") + (parameter.unit != null ? " Unit: " + parameter.unit + "." : ""));
                input.set("schema", schema(parameter));
                input.put("minOccurs", parameter.defaultValue != null || parameter.dataType == DataType.BOOLEAN ? 0 : 1);
                input.put("maxOccurs", 1);
            } else if (parameter.kind == HMParameterKind.OUTPUT) {
                ObjectNode output = outputs.putObject(parameter.name).put("title", parameter.name).put("description",
                        parameter.description);
                output.set("schema", schema(parameter));
            }
        }
        sendJson(ctx, 200, description);
    }

    /**
     * Convert the value of an input of the request to the value of the module.
     *
     * @throws IllegalArgumentException if it is not valid.
     */
    private String toValue( Context ctx, Parameter parameter, JsonNode node ) {
        if (node.isObject() && node.has("value")) {
            return toValue(ctx, parameter, node.get("value"));
        }
        if (node.isObject() && node.has("href")) {
            if (!parameter.isPath()) {
                throw new IllegalArgumentException("Only the files can be given as href.");
            }
            return hrefToPath(ctx, node.get("href").asText());
        }
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            for( JsonNode item : node ) {
                if (!item.isValueNode()) {
                    throw new IllegalArgumentException("The array can contain only values.");
                }
                values.add(item.asText());
            }
            return String.join(",", values);
        }
        if (node.isValueNode() && !node.isNull()) {
            return node.asText();
        }
        throw new IllegalArgumentException("Not a valid value.");
    }

    /**
     * @return the path in the workspace of a file of the data of this server, given by its href.
     */
    private static String hrefToPath( Context ctx, String href ) {
        String prefix = StacApi.DATA_FILES;
        String path = href;
        String base = baseUrl(ctx);
        if (path.startsWith(base)) {
            path = path.substring(base.length());
        }
        // the table of a geopackage, given as fragment
        String table = "";
        int fragmentIndex = path.indexOf('#');
        if (fragmentIndex >= 0) {
            table = path.substring(fragmentIndex);
            path = path.substring(0, fragmentIndex);
        }
        // a signed link: the job reads the file as its user, the signature is not needed
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        if (!path.startsWith(prefix)) {
            throw new IllegalArgumentException("Only the files of the data of this server can be given as href: " + href);
        }
        String file = URLDecoder.decode(path.substring(prefix.length()), StandardCharsets.UTF_8);
        return WorkspacePaths.DATA + "/" + file + table;
    }

    /**
     * @return the href of a file of the api, signed if the request has a token and signed links are on, so that it
     *         can be opened without the token.
     */
    private String fileHref( Context ctx, String path ) throws Exception {
        return fileHref(signer, ctx, path);
    }

    /**
     * @param path the path of a file of the api, url encoded.
     * @return the href of the file, signed if the request has a token and signed links are on.
     */
    static String fileHref( UrlSigner signer, Context ctx, String path ) throws Exception {
        String query = signer.sign(path, Auth.tokenId(ctx));
        return baseUrl(ctx) + path + (query != null ? "?" + query : "");
    }

    private void execute( Context ctx ) throws Exception {
        ModuleDescriptor module = getModule(ctx);
        User user = Auth.user(ctx);
        JsonNode request;
        try {
            request = ctx.body().isBlank() ? mapper.createObjectNode() : mapper.readTree(ctx.body());
        } catch (Exception e) {
            throw new ApiProblem(400, ApiProblem.INVALID_PARAMETER, "Invalid request", "The body is not valid JSON: " + e.getMessage());
        }

        Map<String, String> values = new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();
        for( Map.Entry<String, JsonNode> entry : request.path("inputs").properties() ) {
            Parameter parameter = module.getParameter(entry.getKey());
            if (parameter == null || !isInput(parameter)) {
                errors.put(entry.getKey(), "Not an input of the process.");
                continue;
            }
            try {
                values.put(parameter.name, toValue(ctx, parameter, entry.getValue()));
            } catch (IllegalArgumentException e) {
                errors.put(entry.getKey(), e.getMessage());
            }
        }

        // the files of the outputs asked, all if none
        Set<String> asked = new HashSet<>();
        for( Map.Entry<String, JsonNode> entry : request.path("outputs").properties() ) {
            asked.add(entry.getKey());
        }
        for( String name : asked ) {
            Parameter parameter = module.getParameter(name);
            if (parameter == null || parameter.kind != HMParameterKind.OUTPUT) {
                errors.put(name, "Not an output of the process.");
            }
        }
        for( Parameter parameter : module.getParameters() ) {
            if (isOutputFile(parameter) && (asked.isEmpty() || asked.contains(parameter.name))) {
                values.put(parameter.name, ModulePages.defaultOutputName(parameter));
            }
        }
        if (!errors.isEmpty()) {
            throw invalidParameters(errors);
        }

        JsonNode outputFolder = request.get(JobManager.OUTPUT_FOLDER);
        JsonNode overwrite = request.get("overwrite");
        if (overwrite != null && !overwrite.isBoolean()) {
            throw invalidParameters(Map.of("overwrite", "Must be true or false."));
        }
        Job job;
        try {
            job = jobManager.submit(user.id, user.isAdmin(), module.getName(), values,
                    outputFolder != null && !outputFolder.isNull() ? outputFolder.asText() : null,
                    overwrite != null && overwrite.asBoolean());
        } catch (JobValidationException e) {
            if (e.isConflict()) {
                Map<String, Object> extra = new LinkedHashMap<>();
                extra.put("errors", e.getErrors());
                throw new ApiProblem(409, "about:blank", "Conflict",
                        "Some outputs would replace existing files: run with \"overwrite\": true to replace them.", extra);
            }
            throw invalidParameters(e.getErrors());
        }
        ctx.header("Location", baseUrl(ctx) + API + "/jobs/" + job.id);
        sendJson(ctx, 201, statusInfo(ctx, job));
    }

    private static ApiProblem invalidParameters( Map<String, String> errors ) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("errors", errors);
        return new ApiProblem(400, ApiProblem.INVALID_PARAMETER, "Invalid parameter value",
                "Some inputs or outputs are not valid: " + String.join(", ", errors.keySet()) + ".", extra);
    }

    private Job getJob( Context ctx ) throws Exception {
        User user = Auth.user(ctx);
        String jobId = ctx.pathParam("id");
        if (!jobId.matches("[A-Za-z0-9_-]+") || !jobManager.canRead(user.id, user.isAdmin(), jobId)) {
            throw new ApiProblem(404, ApiProblem.NO_SUCH_JOB, "No such job", "Unknown job: " + jobId);
        }
        return jobManager.getJob(jobId);
    }

    static String ogcStatus( JobState state ) {
        switch( state ) {
        case QUEUED:
            return "accepted";
        case RUNNING:
            return "running";
        case DONE:
            return "successful";
        case FAILED:
            return "failed";
        default:
            return "dismissed";
        }
    }

    private ObjectNode statusInfo( Context ctx, Job job ) {
        String base = baseUrl(ctx);
        ObjectNode info = mapper.createObjectNode().put("jobID", job.id).put("type", "process").put("processID", job.module)
                .put("status", ogcStatus(job.getState()));
        String message = job.getMessage() != null ? job.getMessage() : job.getTask();
        if (message != null) {
            info.put("message", message);
        }
        info.put("created", time(job.created));
        if (job.getStarted() != null) {
            info.put("started", time(job.getStarted()));
        }
        if (job.getFinished() != null) {
            info.put("finished", time(job.getFinished()));
        }
        Long updated = job.getFinished() != null ? job.getFinished() : job.getStarted() != null ? job.getStarted() : job.created;
        info.put("updated", time(updated));
        info.put("progress", job.getState() == JobState.DONE ? 100 : job.getProgress());
        ArrayNode links = info.putArray("links");
        links.add(link(base + API + "/jobs/" + job.id, "self", JSON, "The status of the job"));
        links.add(link(base + "/jobs/" + job.id, "alternate", "text/html", "The page of the job"));
        if (job.getState() == JobState.DONE) {
            links.add(link(base + API + "/jobs/" + job.id + "/results", REL_PREFIX + "results", JSON, "The results of the job"));
        }
        return info;
    }

    private void jobList( Context ctx ) throws Exception {
        User user = Auth.user(ctx);
        Set<String> processIds = splitParam(ctx.queryParam("processID"));
        Set<String> statuses = splitParam(ctx.queryParam("status"));
        int limit = JOBS_LIMIT;
        if (ctx.queryParam("limit") != null) {
            try {
                limit = Math.max(1, Math.min(JOBS_LIMIT, Integer.parseInt(ctx.queryParam("limit"))));
            } catch (NumberFormatException e) {
                throw new ApiProblem(400, ApiProblem.INVALID_PARAMETER, "Invalid parameter value", "The limit must be a number.");
            }
        }
        ObjectNode list = mapper.createObjectNode();
        ArrayNode jobs = list.putArray("jobs");
        for( Job job : jobManager.listJobs(user.isAdmin() ? null : user.id, JOBS_LIMIT) ) {
            if ((processIds.isEmpty() || processIds.contains(job.module))
                    && (statuses.isEmpty() || statuses.contains(ogcStatus(job.getState()))) && jobs.size() < limit) {
                jobs.add(statusInfo(ctx, job));
            }
        }
        list.putArray("links").add(link(baseUrl(ctx) + API + "/jobs", "self", JSON, null));
        sendJson(ctx, 200, list);
    }

    private static Set<String> splitParam( String value ) {
        Set<String> values = new HashSet<>();
        if (value != null && !value.isBlank()) {
            Arrays.stream(value.split(",")).map(String::trim).filter(v -> !v.isEmpty()).forEach(values::add);
        }
        return values;
    }

    private void dismiss( Context ctx ) throws Exception {
        Job job = getJob(ctx);
        if (!job.getState().isFinished()) {
            // killed, it stays as dismissed: a second DELETE removes it
            jobManager.kill(job.id);
        } else {
            try {
                jobManager.delete(job.id);
            } catch (IllegalArgumentException e) {
                throw new ApiProblem(409, "about:blank", "Conflict", e.getMessage());
            }
        }
        ObjectNode info = statusInfo(ctx, job).put("status", "dismissed");
        sendJson(ctx, 200, info);
    }

    private void results( Context ctx ) throws Exception {
        Job job = getJob(ctx);
        switch( job.getState() ) {
        case QUEUED:
        case RUNNING:
            throw new ApiProblem(404, ApiProblem.RESULT_NOT_READY, "Result not ready", "The job is " + ogcStatus(job.getState()) + ".");
        case FAILED:
            throw new ApiProblem(500, "about:blank", "The job failed", job.getMessage());
        case KILLED:
            throw new ApiProblem(410, "about:blank", "The job was dismissed", null);
        default:
        }
        // the outputs are in the data, those of the jobs of the older versions in the folder of their job
        String jobFolder = WorkspacePaths.JOBS + "/" + job.id + "/";
        String dataFolder = WorkspacePaths.DATA + "/";
        ObjectNode results = mapper.createObjectNode();
        for( Map.Entry<String, String> entry : job.getOutputs().entrySet() ) {
            String value = entry.getValue();
            String filesPath = null;
            String file = null;
            if (value.startsWith(dataFolder)) {
                filesPath = StacApi.DATA_FILES;
                file = WorkspacePaths.filePath(value.substring(dataFolder.length()));
            } else if (value.startsWith(jobFolder)) {
                filesPath = API + "/jobs/" + job.id + Auth.FILES_PATH;
                file = WorkspacePaths.filePath(value.substring(jobFolder.length()));
            }
            if (file != null) {
                // the table of a geopackage as fragment, which the next job reads
                String table = WorkspacePaths.tableOf(value);
                results.putObject(entry.getKey())
                        .put("href", fileHref(ctx, filesPath + Html.urlPath(file)) + (table != null ? "#" + table : ""))
                        .put("type", MediaTypes.forFile(file)).put("hm:path", value);
            } else {
                results.set(entry.getKey(), inlineValue(value));
            }
        }
        sendJson(ctx, 200, results);
    }

    /**
     * @return a computed value as a JSON number if it is one, else as text.
     */
    private JsonNode inlineValue( String value ) {
        try {
            if (value.matches("-?\\d{1,18}")) {
                return mapper.getNodeFactory().numberNode(Long.parseLong(value));
            }
            if (value.matches("-?\\d*\\.\\d+([eE][-+]?\\d+)?")) {
                return mapper.getNodeFactory().numberNode(Double.parseDouble(value));
            }
        } catch (NumberFormatException e) {
            // as text
        }
        return mapper.getNodeFactory().textNode(value);
    }

    private void download( Context ctx ) throws Exception {
        Job job = getJob(ctx);
        Path file = JobPages.resolveJobFile(jobManager, job.id, ctx.pathParam("path"));
        if (file == null) {
            throw new ApiProblem(404, "about:blank", "Not found", "The file doesn't exist.");
        }
        Downloads.send(ctx, file, false);
    }
}
