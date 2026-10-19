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

import java.io.File;
import java.util.function.Consumer;

/**
 * Told of the successful ends of the jobs, to prepare their outputs, as the conversion to COG.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public interface JobListener {

    /**
     * Called in the thread of a job, after the module ended successfully and before the job is done, so that the
     * job keeps its slot meanwhile.
     *
     * @param job the job, still running.
     * @param jobFolder its folder.
     * @param endTime the end of the run of the module.
     * @param task to show what is being done, as the task of the job.
     * @throws Exception if something went wrong: the message goes to the log of the job, which is still successful.
     */
    void afterRun( Job job, File jobFolder, long endTime, Consumer<String> task ) throws Exception;
}
