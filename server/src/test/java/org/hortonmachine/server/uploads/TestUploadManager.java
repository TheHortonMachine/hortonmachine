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
package org.hortonmachine.server.uploads;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.ServerConfig;
import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.db.ServerDb;
import org.hortonmachine.server.files.FileOrigin;
import org.hortonmachine.server.files.FileOrigins;
import org.hortonmachine.server.uploads.UploadManager.Upload;

import junit.framework.TestCase;

/**
 * Tests of the uploads: names, quota, zip files.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestUploadManager extends TestCase {
    private static final String USER = "user-1";

    private File workspace;
    private WorkspacePaths paths;
    private ServerDb db;
    private FileOrigins origins;

    @Override
    protected void setUp() throws Exception {
        workspace = Files.createTempDirectory("hm-uploads-test").toFile();
        paths = new WorkspacePaths(workspace);
        paths.createFolders();
        ServerConfig config = new ServerConfig();
        config.workspace = workspace;
        db = ServerDb.open(config);
        origins = new FileOrigins(db, paths);
    }

    @Override
    protected void tearDown() throws Exception {
        db.close();
        FileUtilities.deleteFileOrDir(workspace);
    }

    private UploadManager newManager( long quotaBytes, long maxUnpackedBytes ) {
        return new UploadManager(paths, origins, quotaBytes, maxUnpackedBytes);
    }

    private static ByteArrayInputStream bytes( int size ) {
        return new ByteArrayInputStream(new byte[size]);
    }

    /**
     * @param namesAndContents the entry names and their texts, a name ending with / is a folder.
     */
    private static byte[] zip( String... namesAndContents ) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for( int i = 0; i < namesAndContents.length; i += 2 ) {
                zip.putNextEntry(new ZipEntry(namesAndContents[i]));
                zip.write(namesAndContents[i + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private File uploads() {
        return paths.getAreaFolder(USER);
    }

    public void testNames() throws Exception {
        assertEquals("a_b_.tif", UploadManager.cleanName("a:b?.tif"));
        assertEquals("dtm/2024", UploadManager.cleanPath(" /dtm//./2024/ "));
        assertEquals("dtm/2024", UploadManager.cleanPath("dtm\\2024"));
        assertEquals("", UploadManager.cleanPath(null));
        try {
            UploadManager.cleanPath("dtm/../../etc");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            UploadManager.cleanName("..");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testSaveListDelete() throws Exception {
        UploadManager manager = newManager(0, 1000);
        // the browsers of some systems send the whole path
        assertEquals("dtm/dtm.tif", manager.save(USER, USER,"dtm", "C:\\Users\\me\\dtm.tif", 10, bytes(10)));
        assertEquals("roads.gpkg", manager.save(USER, USER,"", "roads.gpkg", 5, bytes(5)));
        assertEquals(10, new File(uploads(), "dtm/dtm.tif").length());

        List<Upload> list = manager.list(USER);
        assertEquals(3, list.size());
        assertEquals("dtm", list.get(0).path);
        assertTrue(list.get(0).isFolder);
        assertEquals("data/" + USER + "/dtm/dtm.tif", list.get(1).getWorkspacePath());
        assertEquals(15, manager.getUsedBytes(USER));

        // replaced
        manager.save(USER, USER,"", "roads.gpkg", 7, bytes(7));
        assertEquals(17, manager.getUsedBytes(USER));

        try {
            manager.getFile(USER, "../other/x.tif");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        assertFalse(manager.delete(USER, "nothing.tif"));
        assertTrue(manager.delete(USER, "dtm"));
        assertEquals(1, manager.list(USER).size());
        manager.deleteAll(USER);
        assertFalse(uploads().exists());
        // nothing left in the temporary folder
        assertEquals(0, paths.getTmpFolder().list().length);
    }

    public void testQuota() throws Exception {
        UploadManager manager = newManager(100, 1000);
        manager.save(USER, USER,"", "a.tif", 60, bytes(60));
        try {
            manager.save(USER, USER,"", "b.tif", 60, bytes(60));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("limit"));
        }
        // a wrong size given by the client is caught while copying
        try {
            manager.save(USER, USER,"", "b.tif", 10, bytes(60));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("limit"));
        }
        assertFalse(new File(uploads(), "b.tif").exists());
        // replacing the file frees its size
        manager.save(USER, USER,"", "a.tif", 90, bytes(90));
        assertEquals(90, manager.getUsedBytes(USER));
        assertEquals(0, paths.getTmpFolder().list().length);
    }

    public void testUnzip() throws Exception {
        UploadManager manager = newManager(0, 1000);
        byte[] zip = zip("roads/", "", "roads/roads.shp", "shp", "roads/roads.dbf", "dbf", "__MACOSX/roads/._roads.shp", "junk");
        List<String> files = manager.unzip(USER, USER,"vectors", "roads.zip", new ByteArrayInputStream(zip));
        assertEquals(List.of("vectors/roads/roads.shp", "vectors/roads/roads.dbf"), files);
        assertEquals("shp", FileUtilities.readFile(new File(uploads(), "vectors/roads/roads.shp")).trim());
        assertFalse(new File(uploads(), "vectors/__MACOSX").exists());
        assertEquals(0, paths.getTmpFolder().list().length);
    }

    public void testUnzipRefused() throws Exception {
        UploadManager manager = newManager(0, 10);
        // out of the folder: nothing is written
        try {
            manager.unzip(USER, USER,"", "evil.zip", new ByteArrayInputStream(zip("good.txt", "good", "../../evil.txt", "evil")));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(".."));
        }
        assertFalse(new File(uploads(), "good.txt").exists());
        assertFalse(new File(workspace, "evil.txt").exists());

        // absolute names stay in the folder
        manager.unzip(USER, USER,"", "abs.zip", new ByteArrayInputStream(zip("/etc/abs.txt", "abs")));
        assertTrue(new File(uploads(), "etc/abs.txt").isFile());

        // larger than allowed once unpacked
        try {
            manager.unzip(USER, USER,"", "big.zip", new ByteArrayInputStream(zip("big.txt", "more than ten bytes")));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("larger"));
        }
        assertFalse(new File(uploads(), "big.txt").exists());

        // not a zip file
        try {
            manager.unzip(USER, USER,"", "fake.zip", new ByteArrayInputStream("not a zip".getBytes(StandardCharsets.UTF_8)));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Not a zip"));
        }
        assertEquals(0, paths.getTmpFolder().list().length);
    }

    public void testUnzipQuota() throws Exception {
        UploadManager manager = newManager(20, 1000);
        manager.save(USER, USER,"", "a.txt", 15, bytes(15));
        try {
            manager.unzip(USER, USER,"", "b.zip", new ByteArrayInputStream(zip("b.txt", "0123456789")));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("limit"));
        }
        assertFalse(new File(uploads(), "b.txt").exists());
        // replacing the file frees its size
        manager.unzip(USER, USER,"", "a.zip", new ByteArrayInputStream(zip("a.txt", "0123456789")));
        assertEquals(10, manager.getUsedBytes(USER));
    }

    public void testOrigins() throws Exception {
        UploadManager manager = newManager(0, 1000);
        String path = "data/" + USER + "/dtm/dtm.tif";
        manager.save(USER, USER, "dtm", "dtm.tif", 10, bytes(10));
        FileOrigin origin = origins.get(path);
        assertEquals(FileOrigin.UPLOADED, origin.origin);
        assertEquals(USER, origin.userId);
        assertTrue(origin.history.isEmpty());

        // replaced: the previous one in the history
        manager.save(USER, "admin", "dtm", "dtm.tif", 12, bytes(12));
        origin = origins.get(path);
        assertEquals("admin", origin.userId);
        assertEquals(1, origin.history.size());
        assertEquals(USER, origin.history.get(0).userId);
        assertNotNull(origin.history.get(0).replaced);

        // a file put there from outside is external in the history
        FileUtilities.writeFile("outside", new File(uploads(), "dtm/outside.tif"));
        manager.unzip(USER, USER, "dtm", "o.zip", new ByteArrayInputStream(zip("outside.tif", "zipped")));
        origin = origins.get("data/" + USER + "/dtm/outside.tif");
        assertEquals(FileOrigin.EXTERNAL, origin.history.get(0).origin);

        // deleted with the files
        manager.delete(USER, "dtm");
        assertNull(origins.get(path));

        // the shared area has no quota
        UploadManager limited = newManager(5, 1000);
        limited.save(WorkspacePaths.SHARED, "admin", "", "big.tif", 50, bytes(50));
        assertEquals(50, limited.getUsedBytes(WorkspacePaths.SHARED));
        assertEquals(FileOrigin.UPLOADED, origins.get("data/shared/big.tif").origin);
    }
}
