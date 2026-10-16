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
package org.hortonmachine.server.jobs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A run of a module on the server.
 *
 * <p>The fields that change while the job runs are accessed by the thread running it and by the threads
 * of the web pages, so they are volatile and the maps are replaced, not changed.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class Job {
    public final String id;
    /** The user who launched the job, or <code>null</code>. */
    public final String userId;
    public final String module;
    /** The values of the parameters by name, as passed to the module. */
    public final Map<String, String> parameters;
    /** The creation time, in milliseconds since the epoch. */
    public final long created;

    private volatile JobState state = JobState.QUEUED;
    private volatile int progress = 0;
    private volatile String task;
    private volatile String message;
    private volatile Map<String, String> outputs = Collections.emptyMap();
    private volatile Long started;
    private volatile Long finished;
    private volatile Integer exitCode;

    public Job( String id, String userId, String module, Map<String, String> parameters, long created ) {
        this.id = id;
        this.userId = userId;
        this.module = module;
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        this.created = created;
    }

    public JobState getState() {
        return state;
    }

    public void setState( JobState state ) {
        this.state = state;
    }

    /**
     * @return the percentage of the current task, 0 to 100.
     */
    public int getProgress() {
        return progress;
    }

    public void setProgress( int progress ) {
        this.progress = progress;
    }

    /**
     * @return the name of the current task of the module, or <code>null</code>.
     */
    public String getTask() {
        return task;
    }

    public void setTask( String task ) {
        this.task = task;
    }

    /**
     * @return the error message, or <code>null</code>.
     */
    public String getMessage() {
        return message;
    }

    public void setMessage( String message ) {
        this.message = message;
    }

    /**
     * @return the outputs of the module by parameter name: the paths written and the values computed.
     */
    public Map<String, String> getOutputs() {
        return outputs;
    }

    public void setOutputs( Map<String, String> outputs ) {
        this.outputs = Collections.unmodifiableMap(new LinkedHashMap<>(outputs));
    }

    /**
     * @return the start time in milliseconds since the epoch, or <code>null</code> if not started.
     */
    public Long getStarted() {
        return started;
    }

    public void setStarted( Long started ) {
        this.started = started;
    }

    /**
     * @return the end time in milliseconds since the epoch, or <code>null</code> if not finished.
     */
    public Long getFinished() {
        return finished;
    }

    public void setFinished( Long finished ) {
        this.finished = finished;
    }

    /**
     * @return the exit code of the process of the job, or <code>null</code> if it didn't end by itself.
     */
    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode( Integer exitCode ) {
        this.exitCode = exitCode;
    }
}
