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
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hortonmachine.cli.ModuleDescriptor.Parameter;
import org.hortonmachine.gears.libs.modules.HMParameterKind;

/**
 * The folders of the workspace and the paths the users give to the modules.
 *
 * <p>All the files are in the data folder, divided in areas:
 * <ul>
 * <li><code>data/shared/...</code>: the files shared with all the users, written by the administrators only;</li>
 * <li><code>data/&lt;username&gt;/...</code>: the files of a user, uploaded or written by the jobs, seen and written by
 *     the user and the administrators only; the usernames never change, so they name the areas, readable in the paths.</li>
 * </ul>
 * The users never see or give real paths, only these paths in the workspace, as inputs of the modules and as the folder
 * of their outputs. The outputs are file names: a job writes them in its own folder and moves them in the output folder
 * once it succeeded. Absolute paths and <code>..</code> are not accepted, so no path leads out of these folders.
 * Links placed by the administrator in the data folder are followed; the server never creates links.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class WorkspacePaths {
    public static final String DATA = "data";
    /** The area of the files shared with all the users, in the data folder. */
    public static final String SHARED = "shared";
    public static final String JOBS = "jobs";
    /** The folder of the files being uploaded, never given to the users. */
    public static final String TMP = "tmp";
    /** The folder of a job where it writes its outputs, before they are moved in the output folder. */
    public static final String JOB_OUTPUTS = "outputs";
    /** The folder of the outputs of a user when no input says where. */
    public static final String DEFAULT_OUTPUT_FOLDER = "results";
    /** The folder of the uploads of the older versions, moved into the data folder at the start. */
    static final String LEGACY_UPLOADS = "uploads";

    /** The names of the areas: {@link #SHARED} and the usernames, not hidden, never . or .. */
    private static final String AREA_PATTERN = "[A-Za-z0-9_-][A-Za-z0-9._-]*";

    /** A geopackage path with its table, as the modules want them: roads.gpkg#roads. */
    private static final Pattern GEOPACKAGE_TABLE = Pattern.compile("(?i)(.*\\.gpkg)#(.*)");

    private final Path workspace;
    private final Path dataFolder;
    private final Path jobsFolder;
    private final Path tmpFolder;

    public WorkspacePaths( File workspace ) {
        this.workspace = workspace.toPath().toAbsolutePath().normalize();
        dataFolder = this.workspace.resolve(DATA);
        jobsFolder = this.workspace.resolve(JOBS);
        tmpFolder = this.workspace.resolve(TMP);
    }

    /**
     * Create the folders of the workspace, if missing.
     */
    public void createFolders() {
        for( Path folder : new Path[]{workspace, dataFolder, dataFolder.resolve(SHARED), jobsFolder, tmpFolder} ) {
            File file = folder.toFile();
            if (!file.isDirectory() && !file.mkdirs()) {
                throw new IllegalStateException("The folder can't be created: " + file);
            }
        }
    }

    public File getWorkspace() {
        return workspace.toFile();
    }

    /**
     * @return the folder of all the data: the shared area and the areas of the users.
     */
    public File getDataFolder() {
        return dataFolder.toFile();
    }

    /**
     * @param area {@link #SHARED} or the name of a user.
     * @return the folder of an area of the data.
     */
    public File getAreaFolder( String area ) {
        if (!isArea(area)) {
            throw new IllegalArgumentException("Not a valid area: " + area);
        }
        return dataFolder.resolve(area).toFile();
    }

    /**
     * @return <code>true</code> if the name can be the one of an area.
     */
    public static boolean isArea( String name ) {
        return name != null && name.matches(AREA_PATTERN);
    }

    /**
     * @return the area of a path in the workspace, as <code>shared</code> for <code>data/shared/dem/dtm.tif</code>, or
     *         <code>null</code> if it is not in an area.
     */
    public static String areaOf( String workspacePath ) {
        String[] segments = workspacePath.replace('\\', '/').split("/");
        return segments.length >= 2 && segments[0].equals(DATA) && !segments[1].isEmpty() ? segments[1] : null;
    }

    /**
     * @param ownArea the area of the user, its name.
     * @return <code>true</code> if a user can read the files of an area: the shared one, the own one, all for the
     *         administrators.
     */
    public static boolean canRead( String area, String ownArea, boolean isAdmin ) {
        return area != null && (isAdmin || area.equals(SHARED) || area.equals(ownArea));
    }

    /**
     * @param ownArea the area of the user, its name.
     * @return <code>true</code> if a user can write the files of an area: the own one, all for the administrators.
     */
    public static boolean canWrite( String area, String ownArea, boolean isAdmin ) {
        return area != null && (isAdmin || area.equals(ownArea));
    }

    public File getJobsFolder() {
        return jobsFolder.toFile();
    }

    /**
     * @return the folder of the files being uploaded and unpacked, on the same disk as the uploads.
     */
    public File getTmpFolder() {
        return tmpFolder.toFile();
    }

    public File getJobFolder( String jobId ) {
        return jobsFolder.resolve(checkId(jobId)).toFile();
    }

    /**
     * @return the folder where a job writes its outputs, before they are moved in their output folder.
     */
    public File getJobOutputsFolder( String jobId ) {
        return new File(getJobFolder(jobId), JOB_OUTPUTS);
    }

    /**
     * @return the real file of a path in the workspace, without the table of a geopackage.
     */
    public File toFile( String workspacePath ) {
        return workspace.resolve(filePath(workspacePath)).normalize().toFile();
    }

    /**
     * @return the bytes of the files of a folder and its subfolders, without following links; 0 if missing.
     */
    public static long folderSize( Path folder ) throws IOException {
        if (!Files.isDirectory(folder)) {
            return 0;
        }
        long[] size = {0};
        Files.walkFileTree(folder, new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attributes ) {
                size[0] += attributes.size();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed( Path file, IOException e ) {
                // removed meanwhile or not readable
                return FileVisitResult.CONTINUE;
            }
        });
        return size[0];
    }

    private static String checkId( String id ) {
        if (id == null || !id.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Not a valid id: " + id);
        }
        return id;
    }

    /**
     * Clean a path given by a user: / as separator, no absolute paths, no <code>..</code>, no empty names.
     *
     * @param name what the path is, for the messages.
     * @return the path, without leading and trailing /.
     * @throws IllegalArgumentException if the path is not allowed.
     */
    private static String cleanUserPath( String name, String value ) {
        String path = value.trim().replace('\\', '/');
        if (path.isEmpty()) {
            throw new IllegalArgumentException("The path of " + name + " is empty.");
        }
        if (path.startsWith("/") || path.matches("^[A-Za-z]:.*") || path.startsWith("~")) {
            throw new IllegalArgumentException("The path of " + name + " must be relative to the workspace: " + value);
        }
        for( String segment : path.split("/") ) {
            if (segment.equals("..")) {
                throw new IllegalArgumentException("The path of " + name + " can't contain '..': " + value);
            }
        }
        while( path.endsWith("/") ) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Resolve a path in the data folder, checking the area.
     *
     * @param write <code>true</code> if the path is written, else it is read.
     * @return the absolute path.
     */
    private Path resolveData( String name, String value, String path, String ownArea, boolean isAdmin, boolean write ) {
        String area = areaOf(path);
        if (area == null || !isArea(area)) {
            throw new IllegalArgumentException(
                    "The path of " + name + " must start with " + DATA + "/" + SHARED + "/ or " + DATA + "/<your name>/: " + value);
        }
        if (write ? !canWrite(area, ownArea, isAdmin) : !canRead(area, ownArea, isAdmin)) {
            throw new IllegalArgumentException("The path of " + name + " is not in an area you can " + (write ? "write" : "read")
                    + ": " + value);
        }
        Path root = getAreaFolder(area).toPath();
        Path resolved = workspace.resolve(path).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("The path of " + name + " is not allowed: " + value);
        }
        return resolved;
    }

    /**
     * Resolve the path given by a user for a path parameter.
     *
     * @param parameter the parameter, a {@link Parameter#isPath() path}.
     * @param value the path given by the user: for an input a path in the data, as
     *          <code>data/shared/dem/dtm.tif</code>, for an output a path in the output folder, as <code>pit.tif</code>.
     * @param ownArea the area of the user, its name, or <code>null</code> if without user, who reads everything.
     * @param isAdmin if the user is an administrator, who reads all the areas.
     * @param jobId the job the path is for: outputs go into its folder.
     * @return the absolute path.
     * @throws IllegalArgumentException if the path is not allowed.
     */
    public String resolve( Parameter parameter, String value, String ownArea, boolean isAdmin, String jobId ) {
        String path = cleanUserPath(parameter.name, value);
        if (parameter.kind == HMParameterKind.OUTPUT) {
            Path root = getJobOutputsFolder(jobId).toPath();
            Path resolved = root.resolve(path).normalize();
            if (!resolved.startsWith(root) || resolved.equals(root)) {
                throw new IllegalArgumentException("The path of " + parameter.name + " is not allowed: " + value);
            }
            return resolved.toString();
        }
        return resolveData(parameter.name, value, path, ownArea, isAdmin || ownArea == null, false).toString();
    }

    /**
     * Check the folder given by a user for the outputs of a job.
     *
     * @param value the folder, as <code>data/&lt;username&gt;/dem</code>.
     * @param ownArea the area of the user, its name, or <code>null</code> if without user, who writes everywhere.
     * @return the folder as path in the workspace, without trailing /.
     * @throws IllegalArgumentException if the user can't write there.
     */
    public String checkOutputFolder( String value, String ownArea, boolean isAdmin ) {
        String path = cleanUserPath("the output folder", value);
        if (path.split("/").length < 2) {
            throw new IllegalArgumentException("The output folder must be in an area, as " + DATA + "/<your name>/...: " + value);
        }
        resolveData("the output folder", value, path, ownArea, isAdmin || ownArea == null, true);
        return path;
    }

    /**
     * The output folder of a job when the user gives none: the folder of its first input file in the area of the user,
     * so that the outputs are next to the inputs and in the same collection of the catalog.
     *
     * @param firstInput the path in the workspace of the first input file, or <code>null</code>.
     * @param ownArea the area of the user, its name, or <code>null</code> for the shared area.
     * @return the folder, as path in the workspace: <code>data/&lt;username&gt;/dem</code> for
     *         <code>data/shared/dem/dtm.tif</code>, <code>data/&lt;username&gt;/results</code> without inputs.
     */
    public static String defaultOutputFolder( String firstInput, String ownArea ) {
        String own = DATA + "/" + (ownArea != null ? ownArea : SHARED);
        if (firstInput != null) {
            String[] segments = filePath(firstInput.trim().replace('\\', '/')).split("/");
            // data, area, folders..., file
            if (segments.length > 3 && segments[0].equals(DATA)) {
                return own + "/" + String.join("/", Arrays.copyOfRange(segments, 2, segments.length - 1));
            }
        }
        return own + "/" + DEFAULT_OUTPUT_FOLDER;
    }

    /**
     * @return the path of the file of a path, without the table of a geopackage: <code>roads.gpkg</code> for
     *         <code>roads.gpkg#roads</code>; the path itself for the other files.
     */
    public static String filePath( String path ) {
        Matcher matcher = GEOPACKAGE_TABLE.matcher(path);
        return matcher.matches() ? matcher.group(1) : path;
    }

    /**
     * @return the table of a geopackage path, <code>roads</code> for <code>roads.gpkg#roads</code>, or
     *         <code>null</code>.
     */
    public static String tableOf( String path ) {
        Matcher matcher = GEOPACKAGE_TABLE.matcher(path);
        return matcher.matches() ? matcher.group(2) : null;
    }

    /**
     * Convert a real path to the path in the workspace shown to the users.
     *
     * @param absolutePath the real path.
     * @return the path in the workspace, as <code>data/shared/dem/dtm.tif</code>, or <code>null</code>
     *         if it is not in the workspace.
     */
    public String toWorkspacePath( String absolutePath ) {
        Path path;
        try {
            path = Path.of(absolutePath);
        } catch (RuntimeException e) {
            return null;
        }
        if (!path.isAbsolute()) {
            return null;
        }
        path = path.normalize();
        if (path.startsWith(dataFolder) || path.startsWith(jobsFolder)) {
            return workspace.relativize(path).toString().replace('\\', '/');
        }
        return null;
    }

    /**
     * Move the files of the layouts of the older versions into the areas, once:
     * <ul>
     * <li>the shared data of <code>data/...</code> into <code>data/shared/...</code>;</li>
     * <li>the uploads of <code>uploads/&lt;user id&gt;/...</code> into <code>data/&lt;username&gt;/...</code>;</li>
     * <li>the areas named by the id of their user, <code>data/&lt;user id&gt;/...</code>, into
     *     <code>data/&lt;username&gt;/...</code>.</li>
     * </ul>
     * A file whose place is taken keeps both, the moved one with a suffix. To call before {@link #createFolders()}.
     *
     * @param usernames the names of the users by id; the folders of unknown ids keep their name.
     * @return the areas the uploads were moved into, by the id of their user.
     */
    public Map<String, String> migrateLegacyLayout( Map<String, String> usernames ) throws IOException {
        Map<String, String> movedUsers = new LinkedHashMap<>();
        Path shared = dataFolder.resolve(SHARED);
        if (Files.isDirectory(dataFolder) && !Files.exists(shared)) {
            File[] entries = dataFolder.toFile().listFiles();
            Files.createDirectories(shared);
            if (entries != null) {
                for( File entry : entries ) {
                    moveKeepingBoth(entry.toPath(), shared.resolve(entry.getName()));
                }
            }
        }
        Path uploads = workspace.resolve(LEGACY_UPLOADS);
        File[] userFolders = uploads.toFile().listFiles(File::isDirectory);
        if (userFolders != null) {
            for( File userFolder : userFolders ) {
                String userId = userFolder.getName();
                String areaName = usernames.getOrDefault(userId, userId);
                if (!isArea(areaName) || areaName.equals(SHARED)) {
                    continue;
                }
                moveContent(userFolder, dataFolder.resolve(areaName));
                movedUsers.put(userId, areaName);
            }
            File[] left = uploads.toFile().listFiles();
            if (left != null && Arrays.stream(left).allMatch(file -> file.isDirectory() && isEmptyFolder(file))) {
                for( File file : left ) {
                    file.delete();
                }
                uploads.toFile().delete();
            }
        }
        for( Map.Entry<String, String> user : usernames.entrySet() ) {
            File idArea = dataFolder.resolve(user.getKey()).toFile();
            if (!user.getKey().equals(user.getValue()) && idArea.isDirectory() && isArea(user.getValue())) {
                moveContent(idArea, dataFolder.resolve(user.getValue()));
                idArea.delete();
            }
        }
        return movedUsers;
    }

    private static void moveContent( File folder, Path target ) throws IOException {
        Files.createDirectories(target);
        File[] entries = folder.listFiles();
        if (entries != null) {
            for( File entry : entries ) {
                moveKeepingBoth(entry.toPath(), target.resolve(entry.getName()));
            }
        }
    }

    private static boolean isEmptyFolder( File folder ) {
        String[] names = folder.list();
        return names != null && names.length == 0;
    }

    private static void moveKeepingBoth( Path source, Path target ) throws IOException {
        Path free = target;
        String name = target.getFileName().toString();
        int dot = Files.isDirectory(source) ? -1 : name.lastIndexOf('.');
        for( int i = 2; Files.exists(free, LinkOption.NOFOLLOW_LINKS); i++ ) {
            free = target.resolveSibling(dot > 0 ? name.substring(0, dot) + "_" + i + name.substring(dot) : name + "_" + i);
        }
        Files.move(source, free);
    }
}
