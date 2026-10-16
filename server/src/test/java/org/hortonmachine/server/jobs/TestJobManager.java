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

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.ServerConfig;
import org.hortonmachine.server.db.ServerDb;

import junit.framework.TestCase;

/**
 * Tests of the jobs, run in their own processes.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestJobManager extends TestCase {

    private File tmpFolder;
    private ServerDb db;
    private JobManager jobManager;

    @Override
    protected void setUp() throws Exception {
        tmpFolder = Files.createTempDirectory("hm-jobs-test").toFile();
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
        jobManager = new JobManager(db, new File(tmpFolder, "jobs"), slots, "512m", ttlDays);
        jobManager.start();
        return jobManager;
    }

    /**
     * A small elevation with a pit in the middle.
     */
    private File writeElevation() throws Exception {
        double[][] elevation = { //
                {10, 10, 10, 10, 10}, //
                {10, 9, 9, 9, 10}, //
                {10, 9, 5, 9, 10}, //
                {10, 9, 9, 9, 10}, //
                {10, 10, 10, 10, 8}};
        RegionMap region = RegionMap.fromBoundsAndGrid(0, 50, 0, 50, 5, 5);
        GridCoverage2D coverage = CoverageUtilities.buildCoverage("elevation", elevation, region,
                HMCrsRegistry.INSTANCE.getCrs("EPSG:32632"), true);
        File file = new File(tmpFolder, "elevation.tif");
        OmsRasterWriter.writeRaster(file.getAbsolutePath(), coverage);
        return file;
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

    public void testRunJob() throws Exception {
        startJobManager(1, 0);
        File elevation = writeElevation();
        File pit = new File(tmpFolder, "pit.tif");
        Map<String, String> values = new LinkedHashMap<>();
        values.put("inElev", elevation.getAbsolutePath());
        values.put("outPit", pit.getAbsolutePath());

        Job job = jobManager.submit("user1", "Pitfiller", values);
        job = waitFinished(jobManager, job.id);
        File log = new File(jobManager.getJobFolder(job.id), JobManager.LOG_FILE);
        assertEquals(FileUtilities.readFile(log), JobState.DONE, job.getState());
        assertEquals(Integer.valueOf(0), job.getExitCode());
        assertNotNull(job.getStarted());
        assertNotNull(job.getFinished());
        assertEquals(pit.getAbsolutePath(), job.getOutputs().get("outPit"));
        assertTrue(pit.exists());
        assertTrue(new File(jobManager.getJobFolder(job.id), JobManager.PARAMS_FILE).exists());

        // the database has the same
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
    }

    public void testWrongJobs() throws Exception {
        startJobManager(1, 0);
        try {
            jobManager.submit(null, "Foo", Collections.emptyMap());
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unknown module"));
        }
        try {
            jobManager.submit(null, "Gradient", Map.of("pMode", "foo"));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be one of"));
        }
        assertTrue(jobManager.listJobs(null, 10).isEmpty());

        // a missing input makes the module fail
        Job job = jobManager.submit(null, "Gradient", Map.of("outSlope", new File(tmpFolder, "slope.tif").getAbsolutePath()));
        job = waitFinished(jobManager, job.id);
        assertEquals(JobState.FAILED, job.getState());
        assertEquals(Integer.valueOf(1), job.getExitCode());
        assertTrue(job.getMessage(), job.getMessage().contains("the module Gradient failed"));
    }

    public void testKill() throws Exception {
        startJobManager(1, 0);
        File elevation = writeElevation();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("inElev", elevation.getAbsolutePath());
        values.put("outPit", new File(tmpFolder, "pit1.tif").getAbsolutePath());
        Job running = jobManager.submit(null, "Pitfiller", values);
        values.put("outPit", new File(tmpFolder, "pit2.tif").getAbsolutePath());
        Job queued = jobManager.submit(null, "Pitfiller", values);

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
        assertFalse(new File(tmpFolder, "pit1.tif").exists());
        assertFalse(new File(tmpFolder, "pit2.tif").exists());

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
        File oldFolder = new File(tmpFolder, "jobs/old");
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
