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
import java.nio.file.Path;
import java.util.function.Predicate;

import org.hortonmachine.cli.ModuleDescriptor.Parameter;
import org.hortonmachine.gears.libs.modules.HMParameterKind;

/**
 * The folders of the workspace and the paths the users give to the modules.
 *
 * <p>The users never see or give real paths, only paths inside the workspace:
 * <ul>
 * <li>inputs: <code>data/...</code> in the shared data folder, <code>uploads/...</code> in their own uploads,
 *     <code>jobs/&lt;job id&gt;/...</code> in the folder of one of their jobs;</li>
 * <li>outputs: a relative path, as <code>pit.tif</code>, in the folder of the job.</li>
 * </ul>
 * Absolute paths and <code>..</code> are not accepted, so no path leads out of these folders.
 * Links placed by the administrator in the data folder are followed; the server never creates links.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class WorkspacePaths {
    public static final String DATA = "data";
    public static final String UPLOADS = "uploads";
    public static final String JOBS = "jobs";

    private final Path workspace;
    private final Path dataFolder;
    private final Path uploadsFolder;
    private final Path jobsFolder;

    public WorkspacePaths( File workspace ) {
        this.workspace = workspace.toPath().toAbsolutePath().normalize();
        dataFolder = this.workspace.resolve(DATA);
        uploadsFolder = this.workspace.resolve(UPLOADS);
        jobsFolder = this.workspace.resolve(JOBS);
    }

    /**
     * Create the folders of the workspace, if missing.
     */
    public void createFolders() {
        for( Path folder : new Path[]{workspace, dataFolder, uploadsFolder, jobsFolder} ) {
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
     * @return the folder of the data shared with all the users.
     */
    public File getDataFolder() {
        return dataFolder.toFile();
    }

    /**
     * @return the folder of the uploads of a user.
     */
    public File getUploadsFolder( String userId ) {
        return uploadsFolder.resolve(checkId(userId)).toFile();
    }

    public File getJobsFolder() {
        return jobsFolder.toFile();
    }

    public File getJobFolder( String jobId ) {
        return jobsFolder.resolve(checkId(jobId)).toFile();
    }

    private static String checkId( String id ) {
        if (id == null || !id.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Not a valid id: " + id);
        }
        return id;
    }

    /**
     * Resolve the path given by a user for a path parameter.
     *
     * @param parameter the parameter, a {@link Parameter#isPath() path}.
     * @param value the path given by the user.
     * @param userId the user, or <code>null</code> if without user, who has no uploads.
     * @param jobId the job the path is for: outputs go into its folder.
     * @param canReadJob tells if the user can read the outputs of the job with the given id.
     * @return the absolute path.
     * @throws IllegalArgumentException if the path is not allowed.
     */
    public String resolve( Parameter parameter, String value, String userId, String jobId, Predicate<String> canReadJob ) {
        String path = value.trim().replace('\\', '/');
        if (path.isEmpty()) {
            throw new IllegalArgumentException("The path of " + parameter.name + " is empty.");
        }
        if (path.startsWith("/") || path.matches("^[A-Za-z]:.*") || path.startsWith("~")) {
            throw new IllegalArgumentException("The path of " + parameter.name + " must be relative to the workspace: " + value);
        }
        for( String segment : path.split("/") ) {
            if (segment.equals("..")) {
                throw new IllegalArgumentException("The path of " + parameter.name + " can't contain '..': " + value);
            }
        }

        Path root;
        String rest;
        if (parameter.kind == HMParameterKind.OUTPUT) {
            root = getJobFolder(jobId).toPath();
            rest = path;
        } else {
            int slashIndex = path.indexOf('/');
            String first = slashIndex < 0 ? path : path.substring(0, slashIndex);
            rest = slashIndex < 0 ? "" : path.substring(slashIndex + 1);
            switch( first ) {
            case DATA:
                root = dataFolder;
                break;
            case UPLOADS:
                if (userId == null) {
                    throw new IllegalArgumentException("There are no uploads without a user: " + value);
                }
                root = getUploadsFolder(userId).toPath();
                break;
            case JOBS:
                int idEnd = rest.indexOf('/');
                String otherJobId = idEnd < 0 ? rest : rest.substring(0, idEnd);
                if (otherJobId.isEmpty() || !canReadJob.test(otherJobId)) {
                    throw new IllegalArgumentException("The path of " + parameter.name + " is not of one of your jobs: " + value);
                }
                root = getJobFolder(otherJobId).toPath();
                rest = idEnd < 0 ? "" : rest.substring(idEnd + 1);
                break;
            default:
                throw new IllegalArgumentException("The path of " + parameter.name + " must start with " + DATA + "/, " + UPLOADS
                        + "/ or " + JOBS + "/: " + value);
            }
        }

        Path resolved = root.resolve(rest).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("The path of " + parameter.name + " is not allowed: " + value);
        }
        return resolved.toString();
    }

    /**
     * Convert a real path to the path in the workspace shown to the users.
     *
     * @param absolutePath the real path.
     * @return the path in the workspace, as <code>jobs/&lt;job id&gt;/pit.tif</code>, or <code>null</code>
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
        if (path.startsWith(uploadsFolder)) {
            // the user id is not part of the paths given by the users
            Path inUploads = uploadsFolder.relativize(path);
            if (inUploads.getNameCount() < 2) {
                return null;
            }
            return UPLOADS + "/" + inUploads.subpath(1, inUploads.getNameCount()).toString().replace('\\', '/');
        }
        if (path.startsWith(dataFolder) || path.startsWith(jobsFolder)) {
            return workspace.relativize(path).toString().replace('\\', '/');
        }
        return null;
    }
}
