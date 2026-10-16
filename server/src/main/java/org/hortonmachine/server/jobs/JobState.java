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

/**
 * The state of a job.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public enum JobState {
    /** Waiting for a free slot. */
    QUEUED,
    RUNNING,
    /** Finished without errors. */
    DONE,
    /** Finished with an error, or interrupted by a restart of the server. */
    FAILED,
    /** Stopped by a user. */
    KILLED;

    /**
     * @return <code>true</code> if the job is finished, in any way.
     */
    public boolean isFinished() {
        return this == DONE || this == FAILED || this == KILLED;
    }
}
