package org.hortonmachine.gears.io.ogcprocesses;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A generic client for OGC API - Processes servers.
 *
 * <p>
 * This is the entrypoint to submit, poll and download the results of jobs run by a
 * server implementing the <a href="https://ogcapi.ogc.org/processes/">OGC API - Processes</a>
 * standard (async execution), such as the Copernicus CDS/ADS "retrieve" endpoints
 * linked to from their STAC catalogue collections (which do not support STAC item-search,
 * since a CDS/ADS "collection" is a whole dataset, not a set of discrete STAC items).
 * </p>
 * 
 * <p><b>Note:</b> this client is not a full OGC API - Processes implementation, but rather
 * a minimal one that is sufficient to submit jobs to the CDS/ADS retrieve endpoints and
 * download their results. It is not guaranteed to work with other OGC API - Processes servers.
 * </p>
 *
 * @author Andrea Antonello
 */
public class OGCProcessesManager implements AutoCloseable {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final IHMProgressMonitor pm;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private HttpClient httpClient;

    /**
     * @param baseUrl the base URL of the OGC API - Processes server, e.g.
     *                {@code https://cds.climate.copernicus.eu/api/retrieve/v1}.
     * @param pm      progress monitor, or <code>null</code> to use a dummy one.
     */
    public OGCProcessesManager( String baseUrl, IHMProgressMonitor pm ) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.pm = pm != null ? pm : new DummyProgressMonitor();
    }

    /**
     * Open the connection to the server.
     */
    public void open() {
        httpClient = HttpClient.newHttpClient();
    }

    /**
     * Convenience method to set the CDS/ADS personal API token, sent as a
     * <code>PRIVATE-TOKEN</code> header on every request.
     *
     * @param token the personal API token.
     */
    public void setApiToken( String token ) {
        addHeader("PRIVATE-TOKEN", token);
    }

    /**
     * Add a custom header sent on every request. Useful if a server authenticates
     * differently than with a <code>PRIVATE-TOKEN</code> header.
     */
    public void addHeader( String name, String value ) {
        headers.put(name, value);
    }

    /**
     * @param processId the process id. This, in case of CDS/ADS, is the 
     *         actual collection id, e.g. <code>sis-agrometeorological-indicators</code>.
     * @return the raw process description HashMap (id, title, inputs schema, links, ...).
     */
    public Map<String, Object> getProcessDescription( String processId ) throws IOException, InterruptedException {
        HttpResponse<String> response = doGet(baseUrl + "/processes/" + processId);
        checkStatus(response, "Unable to fetch process description for '" + processId + "'");
        return parseMap(response.body());
    }

    /**
     * Given a (possibly partial or empty) set of already-chosen inputs, ask the server
     * which values are still valid for every input field. This is how e.g. the CDS/ADS
     * web form narrows down choices interactively, and can be used the same way here:
     * start with an empty map to get all currently-valid values, then re-call as fields
     * are filled in to narrow down the remaining ones.
     *
     *
     * @param processId the process id.
     * @param partialInputs the inputs already chosen, or <code>null</code>/empty for none.
     * @return a map from input field name to the list of values still valid given
     *         {@code partialInputs}.
     */
    public Map<String, Object> getConstraints( String processId, Map<String, Object> partialInputs )
            throws IOException, InterruptedException {
        checkOpen();
        String body = OBJECT_MAPPER
                .writeValueAsString(Map.of("inputs", partialInputs == null ? Map.of() : partialInputs));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + "/processes/" + processId + "/constraints"))
                .header("Content-Type", "application/json").header("Accept", "application/json")
                .POST(BodyPublishers.ofString(body));
        headers.forEach(builder::header);
        HttpResponse<String> response = httpClient.send(builder.build(), BodyHandlers.ofString());
        checkStatus(response, "Unable to fetch constraints for '" + processId + "'");
        return parseMap(response.body());
    }

    /**
     * Submit a job for the given process.
     *
     * @param processId the process (collection) id.
     * @param inputs the job inputs, matching the schema returned by
     *               {@link #getProcessDescription(String)}.
     * @return a handle to the submitted job.
     */
    public OGCJob submitJob( String processId, Map<String, Object> inputs ) throws IOException, InterruptedException {
        checkOpen();
        String body = OBJECT_MAPPER.writeValueAsString(Map.of("inputs", inputs));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + "/processes/" + processId + "/execution"))
                .header("Content-Type", "application/json").header("Accept", "application/json")
                .header("Prefer", "respond-async").POST(BodyPublishers.ofString(body));
        headers.forEach(builder::header);
        HttpResponse<String> response = httpClient.send(builder.build(), BodyHandlers.ofString());

        checkStatus(response, "Unable to submit job for process '" + processId + "'");

        Map<String, Object> statusDoc = response.body() == null || response.body().isBlank() ? null
                : parseMap(response.body());
        String jobId = statusDoc != null ? asString(statusDoc.get("jobID")) : null;
        if (jobId != null) {
            return toJob(statusDoc);
        }

        String location = response.headers().firstValue("Location").orElse(null);
        if (location == null) {
            throw new IOException(
                    "Job submission response for '" + processId + "' contained neither a jobID nor a Location header");
        }
        String derivedJobId = location.substring(location.lastIndexOf('/') + 1);
        return getJobStatus(derivedJobId);
    }

    /**
     * @param jobId the job id.
     * @return the current status of the job.
     */
    public OGCJob getJobStatus( String jobId ) throws IOException, InterruptedException {
        HttpResponse<String> response = doGet(baseUrl + "/jobs/" + jobId);
        checkStatus(response, "Unable to fetch status for job '" + jobId + "'");
        return toJob(parseMap(response.body()));
    }

    /**
     * Poll a job until it reaches a terminal status.
     *
     * @param job the job to wait for.
     * @param pollIntervalMillis how long to wait between polls.
     * @param timeoutMillis how long to wait in total before giving up.
     * @return the job once it has reached the "successful" status.
     * @throws IOException
     */
    public OGCJob waitForCompletion( OGCJob job, long pollIntervalMillis, long timeoutMillis )
            throws IOException, InterruptedException {
        long start = System.currentTimeMillis();
        OGCJob current = job;
        pm.beginTask("Waiting for job " + job.getJobId() + " to complete...", IHMProgressMonitor.UNKNOWN);
        try {
            while( true ) {
                String status = current.getStatus();
                if ("successful".equalsIgnoreCase(status)) {
                    return current;
                }
                if ("failed".equalsIgnoreCase(status) || "dismissed".equalsIgnoreCase(status)) {
                    throw new IOException(
                            "Job " + current.getJobId() + " ended with status '" + status + "': " + current.getMessage());
                }
                if (System.currentTimeMillis() - start > timeoutMillis) {
                    throw new IOException("Timed out after " + timeoutMillis + "ms waiting for job " + current.getJobId()
                            + " (last status: " + status + ")");
                }
                pm.message("Job " + current.getJobId() + ": " + status
                        + (current.getProgress() != null ? " (" + current.getProgress() + "%)" : ""));
                if (pm.isCanceled()) {
                    throw new IOException("Wait for job " + current.getJobId() + " canceled by user");
                }
                Thread.sleep(pollIntervalMillis);
                current = getJobStatus(current.getJobId());
            }
        } finally {
            pm.done();
        }
    }

    /**
     * @param jobId the job id.
     * @return the raw results document, mapping output keys to their descriptions
     *         (typically containing an <code>href</code> to download).
     */
    public Map<String, Object> getJobResults( String jobId ) throws IOException, InterruptedException {
        HttpResponse<String> response = doGet(baseUrl + "/jobs/" + jobId + "/results");
        checkStatus(response, "Unable to fetch results for job '" + jobId + "'");
        return parseMap(response.body());
    }

    /**
     * Download a job's result to a local file.
     *
     * @param jobId the job id.
     * @param outputKey the output key to download, or <code>null</code> if the job
     *                  has a single output.
     * @param destinationPath the local path to write the result to.
     */
    @SuppressWarnings("unchecked")
    public void downloadResult( String jobId, String outputKey, String destinationPath )
            throws IOException, InterruptedException {
        Map<String, Object> results = getJobResults(jobId);
        Object outputObj;
        if (outputKey != null) {
            outputObj = results.get(outputKey);
            if (!(outputObj instanceof Map)) {
                throw new IOException(
                        "No output named '" + outputKey + "' in results of job '" + jobId + "', available: " + results.keySet());
            }
        } else {
            if (results.size() != 1) {
                throw new IOException(
                        "Job '" + jobId + "' has " + results.size() + " outputs, an outputKey must be specified: "
                                + results.keySet());
            }
            outputObj = results.values().iterator().next();
            if (!(outputObj instanceof Map)) {
                throw new IOException("Unexpected results format for job '" + jobId + "': " + results);
            }
        }
        Map<String, Object> output = (Map<String, Object>) outputObj;
        String href = resolveHref(output);
        if (href == null) {
            throw new IOException("Output '" + outputKey + "' of job '" + jobId + "' has no href to download: " + output);
        }
        downloadToFile(href, destinationPath);
    }

    /**
     * Resolve the download href out of an output description, which per the OGC API -
     * Processes spec may either be a plain link ({@code {href, type, ...}}) or a
     * "qualified value" wrapping the link one level deeper ({@code {value: {href, type, ...}}},
     * as returned e.g. by the CDS/ADS retrieve API).
     */
    @SuppressWarnings("unchecked")
    private String resolveHref( Map<String, Object> output ) {
        String href = asString(output.get("href"));
        if (href != null) {
            return href;
        }
        Object value = output.get("value");
        if (value instanceof Map) {
            return asString(((Map<String, Object>) value).get("href"));
        }
        return null;
    }

    private void downloadToFile( String href, String destinationPath ) throws IOException {
        Path targetFile = Paths.get(destinationPath);
        URL url = new URL(href);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(60_000);
        long contentLength = connection.getContentLengthLong();

        pm.beginTask("Downloading result: " + href, IHMProgressMonitor.UNKNOWN);
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }
        try (InputStream is = new BufferedInputStream(connection.getInputStream());
                OutputStream os = new BufferedOutputStream(Files.newOutputStream(targetFile))) {
            byte[] buffer = new byte[128 * 1024];
            long totalRead = 0;
            int read;
            int lastPercent = -1;
            while( (read = is.read(buffer)) != -1 ) {
                os.write(buffer, 0, read);
                totalRead += read;
                if (contentLength > 0) {
                    int percent = (int) (100L * totalRead / contentLength);
                    if (percent != lastPercent && percent % 10 == 0) {
                        pm.message("Downloaded: " + percent + "%...");
                        lastPercent = percent;
                    }
                }
                if (pm.isCanceled()) {
                    throw new IOException("Download canceled by user");
                }
            }
        } finally {
            pm.done();
        }
    }

    private HttpResponse<String> doGet( String url ) throws IOException, InterruptedException {
        checkOpen();
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).header("Accept", "application/json")
                .timeout(Duration.ofSeconds(60)).GET();
        headers.forEach(builder::header);
        return httpClient.send(builder.build(), BodyHandlers.ofString());
    }

    private void checkOpen() throws IOException {
        if (httpClient == null) {
            throw new IOException("Client not available, did you call open()?");
        }
    }

    private void checkStatus( HttpResponse<String> response, String errorPrefix ) throws IOException {
        if (response.statusCode() >= 400) {
            throw new IOException(errorPrefix + " (HTTP " + response.statusCode() + "): " + response.body());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap( String body ) throws IOException {
        return OBJECT_MAPPER.readValue(body, Map.class);
    }

    private static String asString( Object o ) {
        return o == null ? null : o.toString();
    }

    private OGCJob toJob( Map<String, Object> statusDoc ) {
        OGCJob job = new OGCJob();
        job.setJobId(asString(statusDoc.get("jobID")));
        job.setStatus(asString(statusDoc.get("status")));
        job.setMessage(asString(statusDoc.get("message")));
        Object progress = statusDoc.get("progress");
        if (progress instanceof Number) {
            job.setProgress(((Number) progress).intValue());
        }
        job.setRawFields(statusDoc);
        return job;
    }

    @Override
    public void close() {
        // java.net.http.HttpClient has no resources to explicitly release
    }

}
