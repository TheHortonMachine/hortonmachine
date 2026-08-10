package org.hortonmachine.gears.io.copernicus;

import java.util.Map;

/**
 * A handle to an OGC API - Processes job (as created by
 * {@link OGCProcessesManager#submitJob(String, Map)}).
 *
 * @author Andrea Antonello
 */
public class OGCJob {
    private String jobId;
    private String status;
    private String message;
    private Integer progress;
    private Map<String, Object> rawFields;

    public String getJobId() {
        return jobId;
    }

    void setJobId( String jobId ) {
        this.jobId = jobId;
    }

    /**
     * @return the job status, as reported by the server (e.g. "accepted", "running",
     *         "successful", "failed", "dismissed").
     */
    public String getStatus() {
        return status;
    }

    void setStatus( String status ) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    void setMessage( String message ) {
        this.message = message;
    }

    /**
     * @return the completion percentage (0-100), or <code>null</code> if not reported.
     */
    public Integer getProgress() {
        return progress;
    }

    void setProgress( Integer progress ) {
        this.progress = progress;
    }

    /**
     * @return the full job status document as parsed from the server, for access to
     *         fields not otherwise exposed by this class.
     */
    public Map<String, Object> getRawFields() {
        return rawFields;
    }

    void setRawFields( Map<String, Object> rawFields ) {
        this.rawFields = rawFields;
    }

    @Override
    public String toString() {
        return "OGCJob{jobId=" + jobId + ", status=" + status + ", progress=" + progress + "}";
    }

}
