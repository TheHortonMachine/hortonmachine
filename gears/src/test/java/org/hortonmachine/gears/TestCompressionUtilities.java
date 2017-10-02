package org.hortonmachine.gears;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.hortonmachine.gears.utils.CompressionUtilities;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.files.FileUtilities;
/**
 * Test compression.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestCompressionUtilities extends HMTestCase {

    public void testCompressionWithFolder() throws Exception {

        Path tempDirectory = createTestFiles();

        String tmpFolder = tempDirectory.getParent().toString();

        // with folder
        Path tempWithFolderZip = Files.createTempFile("jgt-textcompression-withfolder", ".zip");
        CompressionUtilities.zipFolder(tempDirectory.toString(), tempWithFolderZip.toString(), true);

        assertTrue(tempWithFolderZip.toFile().exists());

        // delete
        FileUtilities.deleteFileOrDir(tempDirectory.toFile());
        assertFalse(tempDirectory.toFile().exists());

        String unzipFolderName = CompressionUtilities.unzipFolder(tempWithFolderZip.toString(), tmpFolder, false);

        Path outputF1Path = Paths.get(tmpFolder, tempDirectory.getFileName().toString(), "f1.txt");
        assertTrue(outputF1Path.toFile().exists());

        // cleanup
        // unzipped folder
        Path unzipFolderPath = Paths.get(tmpFolder, unzipFolderName);
        FileUtilities.deleteFileOrDir(unzipFolderPath.toFile());
        assertFalse(unzipFolderPath.toFile().exists());
        // zip file
        FileUtilities.deleteFileOrDir(tempWithFolderZip.toFile());
        assertFalse(tempWithFolderZip.toFile().exists());

    }

    private Path createTestFiles() throws IOException {
        Path tempDirectory = Files.createTempDirectory("jgt-test");

        String str1 = "hello f1!";
        Path p1 = Paths.get(tempDirectory.toString(), "f1.txt");
        Files.write(p1, str1.getBytes());

        String str2 = "hello f2!";
        Path p2 = Paths.get(tempDirectory.toString(), "f2.txt");
        Files.write(p2, str2.getBytes());
        return tempDirectory;
    }

    public void testCompressionWithoutFolder() throws Exception {

        Path tempDirectory = createTestFiles();

        // no folder
        Path tempNoFolderZip = Files.createTempFile("jgt-textcompression-nofolder", ".zip");
        CompressionUtilities.zipFolder(tempDirectory.toString(), tempNoFolderZip.toString(), false);

        assertTrue(tempNoFolderZip.toFile().exists());

        // delete
        FileUtilities.deleteFileOrDir(tempDirectory.toFile());
        assertFalse(tempDirectory.toFile().exists());

        CompressionUtilities.unzipFolder(tempNoFolderZip.toString(), tempDirectory.toString(), false);

        Path outputF1Path = Paths.get(tempDirectory.toString(), "f1.txt");
        assertTrue(outputF1Path.toFile().exists());

        // cleanup
        // unzipped folder
        FileUtilities.deleteFileOrDir(tempDirectory.toFile());
        assertFalse(tempDirectory.toFile().exists());
        // zip file
        FileUtilities.deleteFileOrDir(tempNoFolderZip.toFile());
        assertFalse(tempNoFolderZip.toFile().exists());

    }

}
