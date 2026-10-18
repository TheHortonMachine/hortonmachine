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
package org.hortonmachine.server;

import java.io.File;
import java.nio.file.Files;
import java.util.function.Predicate;

import org.hortonmachine.cli.ModuleDescriptor;
import org.hortonmachine.cli.ModuleDescriptor.Parameter;
import org.hortonmachine.gears.utils.files.FileUtilities;

import junit.framework.TestCase;

/**
 * Tests of the paths of the users in the workspace.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestWorkspacePaths extends TestCase {

    private File tmpFolder;
    private WorkspacePaths paths;
    private Parameter input;
    private Parameter output;
    private Predicate<String> canReadJob = jobId -> jobId.equals("myjob");

    @Override
    protected void setUp() throws Exception {
        tmpFolder = Files.createTempDirectory("hm-paths-test").toFile();
        paths = new WorkspacePaths(new File(tmpFolder, "ws"));
        paths.createFolders();
        ModuleDescriptor gradient = ModuleDescriptor.of(org.hortonmachine.modules.Gradient.class);
        input = gradient.getParameter("inElev");
        output = gradient.getParameter("outSlope");
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtilities.deleteFileOrDir(tmpFolder);
    }

    private String resolveInput( String value ) {
        return paths.resolve(input, value, "user1", "job1", canReadJob);
    }

    private String resolveOutput( String value ) {
        return paths.resolve(output, value, "user1", "job1", canReadJob);
    }

    public void testFolders() {
        assertTrue(paths.getDataFolder().isDirectory());
        assertTrue(paths.getJobsFolder().isDirectory());
        assertEquals(new File(paths.getWorkspace(), "uploads/user1"), paths.getUploadsFolder("user1"));
        try {
            paths.getUploadsFolder("../user1");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            paths.getJobFolder("a/b");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testResolve() {
        File workspace = paths.getWorkspace();
        assertEquals(new File(workspace, "data/dem/dtm.tif").getPath(), resolveInput("data/dem/dtm.tif"));
        assertEquals(new File(workspace, "data/dem/dtm.tif").getPath(), resolveInput(" data\\dem\\dtm.tif "));
        assertEquals(new File(workspace, "data").getPath(), resolveInput("data"));
        assertEquals(new File(workspace, "uploads/user1/dtm.tif").getPath(), resolveInput("uploads/dtm.tif"));
        assertEquals(new File(workspace, "jobs/myjob/pit.tif").getPath(), resolveInput("jobs/myjob/pit.tif"));

        // the outputs go in the folder of the job
        assertEquals(new File(workspace, "jobs/job1/slope.tif").getPath(), resolveOutput("slope.tif"));
        assertEquals(new File(workspace, "jobs/job1/out/slope.tif").getPath(), resolveOutput("out/./slope.tif"));
    }

    public void testRefused() {
        String[] inputs = {"", "/etc/passwd", "C:\\data\\dtm.tif", "c:/data", "~/dtm.tif", "data/../hm-server.sqlite", "../data/x",
                "data/dem/../../jobs/x", "dtm.tif", "jobs/otherjob/pit.tif", "jobs/", "jobs/../data", "uploads/../user2/x"};
        for( String value : inputs ) {
            try {
                resolveInput(value);
                fail("Accepted: " + value);
            } catch (IllegalArgumentException e) {
                // expected
            }
        }
        String[] outputs = {"/tmp/slope.tif", "../slope.tif", "a/../../slope.tif", "D:/slope.tif"};
        for( String value : outputs ) {
            try {
                resolveOutput(value);
                fail("Accepted: " + value);
            } catch (IllegalArgumentException e) {
                // expected
            }
        }
        // no uploads without a user
        try {
            paths.resolve(input, "uploads/dtm.tif", null, "job1", canReadJob);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("without a user"));
        }
    }

    public void testGeopackageTables() {
        assertEquals("jobs/job1/roads.gpkg", WorkspacePaths.filePath("jobs/job1/roads.gpkg#roads"));
        assertEquals("roads", WorkspacePaths.tableOf("jobs/job1/roads.gpkg#roads"));
        assertEquals("ROADS.GPKG", WorkspacePaths.filePath("ROADS.GPKG#r"));
        assertEquals("slope.tif", WorkspacePaths.filePath("slope.tif"));
        assertNull(WorkspacePaths.tableOf("slope.tif"));
        assertNull(WorkspacePaths.tableOf("notes#1.txt"));
    }

    public void testToWorkspacePath() {
        File workspace = paths.getWorkspace();
        assertEquals("jobs/job1/slope.tif", paths.toWorkspacePath(new File(workspace, "jobs/job1/slope.tif").getPath()));
        assertEquals("data/dem/dtm.tif", paths.toWorkspacePath(new File(workspace, "data/dem/dtm.tif").getPath()));
        assertEquals("uploads/dtm.tif", paths.toWorkspacePath(new File(workspace, "uploads/user1/dtm.tif").getPath()));
        assertNull(paths.toWorkspacePath(new File(workspace, "hm-server.sqlite").getPath()));
        assertNull(paths.toWorkspacePath(new File(tmpFolder, "elsewhere.tif").getPath()));
        assertNull(paths.toWorkspacePath("slope.tif"));
        assertNull(paths.toWorkspacePath("12.5"));
    }
}
