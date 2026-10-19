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
import java.util.Map;

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
        return paths.resolve(input, value, "user1", false, "job1");
    }

    private String resolveOutput( String value ) {
        return paths.resolve(output, value, "user1", false, "job1");
    }

    private static void assertRefused( Runnable run, String value ) {
        try {
            run.run();
            fail("Accepted: " + value);
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testFolders() {
        assertTrue(paths.getDataFolder().isDirectory());
        assertTrue(paths.getAreaFolder(WorkspacePaths.SHARED).isDirectory());
        assertTrue(paths.getJobsFolder().isDirectory());
        assertEquals(new File(paths.getWorkspace(), "data/user1"), paths.getAreaFolder("user1"));
        assertEquals(new File(paths.getWorkspace(), "jobs/job1/outputs"), paths.getJobOutputsFolder("job1"));
        assertRefused(() -> paths.getAreaFolder("../user1"), "../user1");
        assertRefused(() -> paths.getJobFolder("a/b"), "a/b");
    }

    public void testAreas() {
        assertEquals("shared", WorkspacePaths.areaOf("data/shared/dem/dtm.tif"));
        assertEquals("user1", WorkspacePaths.areaOf("data/user1"));
        assertNull(WorkspacePaths.areaOf("data"));
        assertNull(WorkspacePaths.areaOf("jobs/job1/pit.tif"));
        assertTrue(WorkspacePaths.canRead("shared", "user1", false));
        assertTrue(WorkspacePaths.canRead("user1", "user1", false));
        assertFalse(WorkspacePaths.canRead("user2", "user1", false));
        assertTrue(WorkspacePaths.canRead("user2", "admin", true));
        assertFalse(WorkspacePaths.canWrite("shared", "user1", false));
        assertTrue(WorkspacePaths.canWrite("user1", "user1", false));
        assertTrue(WorkspacePaths.canWrite("shared", "admin", true));
    }

    public void testResolve() {
        File workspace = paths.getWorkspace();
        assertEquals(new File(workspace, "data/shared/dem/dtm.tif").getPath(), resolveInput("data/shared/dem/dtm.tif"));
        assertEquals(new File(workspace, "data/shared/dem/dtm.tif").getPath(), resolveInput(" data\\shared\\dem\\dtm.tif "));
        assertEquals(new File(workspace, "data/user1/dtm.tif").getPath(), resolveInput("data/user1/dtm.tif"));
        assertEquals(new File(workspace, "data/shared").getPath(), resolveInput("data/shared/"));
        // the administrators read all the areas
        assertEquals(new File(workspace, "data/user2/dtm.tif").getPath(), paths.resolve(input, "data/user2/dtm.tif", "admin", true, "job1"));

        // the outputs go in the outputs folder of the job, before they are moved
        assertEquals(new File(workspace, "jobs/job1/outputs/slope.tif").getPath(), resolveOutput("slope.tif"));
        assertEquals(new File(workspace, "jobs/job1/outputs/out/slope.tif").getPath(), resolveOutput("out/./slope.tif"));
    }

    public void testRefused() {
        String[] inputs = {"", "/etc/passwd", "C:\\data\\dtm.tif", "c:/data", "~/dtm.tif", "data/../hm-server.sqlite", "../data/x",
                "data/shared/../../jobs/x", "dtm.tif", "data", "data/dtm.tif/", "jobs/job1/pit.tif", "data/user2/dtm.tif",
                "data/shared/../user2/x", "uploads/dtm.tif"};
        for( String value : inputs ) {
            assertRefused(() -> resolveInput(value), value);
        }
        String[] outputs = {"/tmp/slope.tif", "../slope.tif", "a/../../slope.tif", "D:/slope.tif", "."};
        for( String value : outputs ) {
            assertRefused(() -> resolveOutput(value), value);
        }
    }

    public void testOutputFolder() {
        assertEquals("data/user1/dem", paths.checkOutputFolder("data/user1/dem/", "user1", false));
        assertEquals("data/user1", paths.checkOutputFolder("data\\user1", "user1", false));
        assertEquals("data/shared/dem", paths.checkOutputFolder("data/shared/dem", "admin", true));
        for( String value : new String[]{"data/shared/dem", "data/user2/dem", "data", "jobs/job1", "data/user1/../user2", "/tmp"} ) {
            assertRefused(() -> paths.checkOutputFolder(value, "user1", false), value);
        }

        // next to the first input, in the own area
        assertEquals("data/user1/flanginec/dem", WorkspacePaths.defaultOutputFolder("data/shared/flanginec/dem/dtm.tif", "user1"));
        assertEquals("data/user1/flanginec", WorkspacePaths.defaultOutputFolder("data/user1/flanginec/roads.gpkg#roads", "user1"));
        assertEquals("data/user1/flanginec", WorkspacePaths.defaultOutputFolder("data/user2/flanginec/dtm.tif", "user1"));
        assertEquals("data/user1/results", WorkspacePaths.defaultOutputFolder("data/shared/dtm.tif", "user1"));
        assertEquals("data/user1/results", WorkspacePaths.defaultOutputFolder(null, "user1"));
        assertEquals("data/shared/results", WorkspacePaths.defaultOutputFolder(null, null));
    }

    public void testMigrateLegacyLayout() throws Exception {
        File workspace = new File(tmpFolder, "old");
        assertTrue(new File(workspace, "data/flanginec").mkdirs());
        assertTrue(new File(workspace, "uploads/user1/flanginec").mkdirs());
        FileUtilities.writeFile("dem", new File(workspace, "data/flanginec/dtm.tif"));
        FileUtilities.writeFile("root", new File(workspace, "data/notes.txt"));
        FileUtilities.writeFile("mine", new File(workspace, "uploads/user1/flanginec/mine.tif"));
        FileUtilities.writeFile("loose", new File(workspace, "uploads/user1/loose.txt"));
        WorkspacePaths old = new WorkspacePaths(workspace);

        // the uploads go in the area named by the user
        Map<String, String> usernames = Map.of("user1", "jane");
        assertEquals("{user1=jane}", old.migrateLegacyLayout(usernames).toString());
        old.createFolders();
        assertTrue(new File(workspace, "data/shared/flanginec/dtm.tif").isFile());
        assertTrue(new File(workspace, "data/shared/notes.txt").isFile());
        assertTrue(new File(workspace, "data/jane/flanginec/mine.tif").isFile());
        assertTrue(new File(workspace, "data/jane/loose.txt").isFile());
        assertFalse(new File(workspace, "uploads").exists());
        assertFalse(new File(workspace, "data/flanginec").exists());

        // once only
        assertTrue(old.migrateLegacyLayout(usernames).isEmpty());
        assertTrue(new File(workspace, "data/shared/flanginec/dtm.tif").isFile());

        // an area named by the id of its user is renamed by the user, a clash keeps both
        assertTrue(new File(workspace, "data/user2/flanginec").mkdirs());
        FileUtilities.writeFile("john", new File(workspace, "data/user2/flanginec/mine.tif"));
        assertTrue(new File(workspace, "data/john/flanginec").mkdirs());
        FileUtilities.writeFile("john old", new File(workspace, "data/john/flanginec/old.tif"));
        old.migrateLegacyLayout(Map.of("user1", "jane", "user2", "john"));
        assertFalse(new File(workspace, "data/user2").exists());
        assertTrue(new File(workspace, "data/john/flanginec/old.tif").isFile());
        assertTrue(new File(workspace, "data/john/flanginec_2/mine.tif").isFile());
    }

    public void testAreaNames() {
        assertTrue(WorkspacePaths.isArea("jane"));
        assertTrue(WorkspacePaths.isArea("john.doe"));
        assertTrue(WorkspacePaths.isArea(WorkspacePaths.SHARED));
        assertFalse(WorkspacePaths.isArea(".hidden"));
        assertFalse(WorkspacePaths.isArea(".."));
        assertFalse(WorkspacePaths.isArea("a/b"));
        assertEquals(new File(paths.getWorkspace(), "data/john.doe/dem/dtm.tif").getPath(),
                paths.resolve(input, "data/john.doe/dem/dtm.tif", "john.doe", false, "job1"));
        assertRefused(() -> paths.getAreaFolder(".."), "..");
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
        assertEquals("jobs/job1/outputs/slope.tif", paths.toWorkspacePath(new File(workspace, "jobs/job1/outputs/slope.tif").getPath()));
        assertEquals("data/shared/dem/dtm.tif", paths.toWorkspacePath(new File(workspace, "data/shared/dem/dtm.tif").getPath()));
        assertEquals("data/user1/dtm.tif", paths.toWorkspacePath(new File(workspace, "data/user1/dtm.tif").getPath()));
        assertNull(paths.toWorkspacePath(new File(workspace, "hm-server.sqlite").getPath()));
        assertNull(paths.toWorkspacePath(new File(tmpFolder, "elsewhere.tif").getPath()));
        assertNull(paths.toWorkspacePath("slope.tif"));
        assertNull(paths.toWorkspacePath("12.5"));
    }
}
