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
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.SampleData;
import org.hortonmachine.server.ServerConfig;
import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.db.ServerDb;

import junit.framework.TestCase;

/**
 * Tests of the jobs, run in their own processes.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestJobManager extends TestCase {

    private static final String ELEVATION = SampleData.ELEVATION;

    private File tmpFolder;
    private WorkspacePaths paths;
    private ServerDb db;
    private JobManager jobManager;

    @Override
    protected void setUp() throws Exception {
        tmpFolder = Files.createTempDirectory("hm-jobs-test").toFile();
        paths = new WorkspacePaths(tmpFolder);
        paths.createFolders();
        ServerConfig config = new ServerConfig();
        config.workspace = tmpFolder;
        db = ServerDb.open(config);
    }

    @Override
    protected void tearDown() throws Exception {
        if (jobManager != null) {
            jobManager.close();
        }
        db.close();
        FileUtilities.deleteFileOrDir(tmpFolder);
    }

    private JobManager startJobManager( int slots, int ttlDays ) throws Exception {
        jobManager = new JobManager(db, paths, slots, "512m", ttlDays);
        jobManager.start();
        return jobManager;
    }

    private void writeElevation() throws Exception {
        SampleData.writeElevation(paths);
    }

    private static Map<String, String> pitValues( String inElev, String outPit ) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("inElev", inElev);
        values.put("outPit", outPit);
        return values;
    }

    private static Job waitFinished( JobManager jobManager, String jobId ) throws Exception {
        long end = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(120);
        while( System.currentTimeMillis() < end ) {
            Job job = jobManager.getJob(jobId);
            if (job.getState().isFinished()) {
                return job;
            }
            Thread.sleep(50);
        }
        fail("The job didn't finish: " + jobId);
        return null;
    }

    private void assertDone( Job job ) throws Exception {
        File log = new File(jobManager.getJobFolder(job.id), JobManager.LOG_FILE);
        assertEquals(FileUtilities.readFile(log), JobState.DONE, job.getState());
    }

    public void testRunJob() throws Exception {
        startJobManager(1, 0);
        writeElevation();
        Map<String, String> values = pitValues(ELEVATION, "pit.tif");

        Job job = jobManager.submit("user1", false, "Pitfiller", values);
        job = waitFinished(jobManager, job.id);
        assertDone(job);
        assertEquals(Integer.valueOf(0), job.getExitCode());
        assertNotNull(job.getStarted());
        assertNotNull(job.getFinished());
        // the output is in the job folder, shown as a path in the workspace
        String pitPath = WorkspacePaths.JOBS + "/" + job.id + "/pit.tif";
        assertEquals(pitPath, job.getOutputs().get("outPit"));
        assertTrue(new File(jobManager.getJobFolder(job.id), "pit.tif").exists());
        assertTrue(new File(jobManager.getJobFolder(job.id), JobManager.PARAMS_FILE).exists());

        // the database has the same, with the paths of the user
        Job saved = db.getJob(job.id);
        assertEquals(JobState.DONE, saved.getState());
        assertEquals("user1", saved.userId);
        assertEquals("Pitfiller", saved.module);
        assertEquals(values, saved.parameters);
        assertEquals(job.getOutputs(), saved.getOutputs());

        List<Job> jobs = jobManager.listJobs("user1", 10);
        assertEquals(1, jobs.size());
        assertTrue(jobManager.listJobs("user2", 10).isEmpty());
        assertEquals(1, jobManager.listJobs(null, 10).size());

        // the output is the input of a next job of the same user or of an administrator
        assertTrue(jobManager.canRead("user1", false, job.id));
        assertFalse(jobManager.canRead("user2", false, job.id));
        assertTrue(jobManager.canRead("user2", true, job.id));
        Job next = jobManager.submit("user1", false, "Pitfiller", pitValues(pitPath, "pit2.tif"));
        assertDone(waitFinished(jobManager, next.id));
        try {
            jobManager.submit("user2", false, "Pitfiller", pitValues(pitPath, "pit3.tif"));
            fail();
        } catch (JobValidationException e) {
            assertTrue(e.getErrors().get("inElev").contains("not of one of your jobs"));
        }
        Job admins = jobManager.submit("user2", true, "Pitfiller", pitValues(pitPath, "pit3.tif"));
        assertDone(waitFinished(jobManager, admins.id));
    }

    public void testWrongJobs() throws Exception {
        startJobManager(1, 0);
        writeElevation();
        try {
            jobManager.submit(null, false, "Foo", Collections.emptyMap());
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unknown module"));
        }

        assertErrors(Map.of("pMode", "foo"), "pMode", "must be one of");
        assertErrors(pitValues(new File(paths.getWorkspace(), ELEVATION).getAbsolutePath(), "pit.tif"), "inElev",
                "relative to the workspace");
        assertErrors(pitValues(ELEVATION, "../pit.tif"), "outPit", "can't contain '..'");
        assertErrors(pitValues(ELEVATION, "/tmp/pit.tif"), "outPit", "relative to the workspace");
        assertErrors(pitValues("data/missing.tif", "pit.tif"), "inElev", "doesn't exist");
        assertErrors(pitValues("elevation.tif", "pit.tif"), "inElev", "must start with");
        // no job and no job folder is left
        assertTrue(jobManager.listJobs(null, 10).isEmpty());
        assertEquals(1, paths.getJobsFolder().list().length); // the args file

        // a missing input makes the module fail
        Job job = jobManager.submit(null, false, "Gradient", Map.of("outSlope", "slope.tif"));
        job = waitFinished(jobManager, job.id);
        assertEquals(JobState.FAILED, job.getState());
        assertEquals(Integer.valueOf(1), job.getExitCode());
        assertTrue(job.getMessage(), job.getMessage().contains("the module Gradient failed"));
    }

    private void assertErrors( Map<String, String> values, String parameter, String error ) throws Exception {
        String module = values.containsKey("pMode") ? "Gradient" : "Pitfiller";
        try {
            jobManager.submit("user1", false, module, values);
            fail("Accepted: " + values);
        } catch (JobValidationException e) {
            assertNotNull(e.getErrors().toString(), e.getErrors().get(parameter));
            assertTrue(e.getErrors().get(parameter), e.getErrors().get(parameter).contains(error));
        }
    }

    public void testKill() throws Exception {
        startJobManager(1, 0);
        writeElevation();
        Job running = jobManager.submit(null, false, "Pitfiller", pitValues(ELEVATION, "pit.tif"));
        Job queued = jobManager.submit(null, false, "Pitfiller", pitValues(ELEVATION, "pit.tif"));

        // the second job waits for the only slot and never runs
        assertEquals(JobState.QUEUED, jobManager.getJob(queued.id).getState());
        assertTrue(jobManager.kill(queued.id));
        assertEquals(JobState.KILLED, jobManager.getJob(queued.id).getState());
        assertEquals(JobState.KILLED, db.getJob(queued.id).getState());

        // the first is killed while its process starts
        long end = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        while( jobManager.getJob(running.id).getState() == JobState.QUEUED && System.currentTimeMillis() < end ) {
            Thread.sleep(5);
        }
        assertTrue(jobManager.kill(running.id));
        Job killed = waitFinished(jobManager, running.id);
        assertEquals(killed.getMessage(), JobState.KILLED, killed.getState());
        assertFalse(new File(jobManager.getJobFolder(running.id), "pit.tif").exists());
        assertFalse(new File(jobManager.getJobFolder(queued.id), "pit.tif").exists());

        // finished jobs can't be killed
        assertFalse(jobManager.kill(running.id));
        assertFalse(jobManager.kill("nojob"));
    }

    public void testRestartAndCleanup() throws Exception {
        // a job running when the server stopped
        Job interrupted = new Job("interrupted", null, "Pitfiller", Collections.emptyMap(), System.currentTimeMillis());
        interrupted.setState(JobState.RUNNING);
        db.insertJob(interrupted);
        // a job finished long ago, with its folder
        long longAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10);
        Job old = new Job("old", null, "Pitfiller", Collections.emptyMap(), longAgo);
        old.setState(JobState.DONE);
        old.setFinished(longAgo);
        db.insertJob(old);
        File oldFolder = paths.getJobFolder("old");
        assertTrue(oldFolder.mkdirs());

        startJobManager(1, 7);
        Job job = db.getJob("interrupted");
        assertEquals(JobState.FAILED, job.getState());
        assertTrue(job.getMessage().contains("server stopped"));
        assertNotNull(job.getFinished());

        // the removal runs in the background
        long end = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while( db.getJob("old") != null && System.currentTimeMillis() < end ) {
            Thread.sleep(20);
        }
        assertNull(db.getJob("old"));
        assertFalse(oldFolder.exists());
        assertNotNull(db.getJob("interrupted"));
    }
}
