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
package org.hortonmachine.server.web;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import org.hortonmachine.cli.ModuleDescriptor.DataType;
import org.hortonmachine.cli.ModuleDescriptor.Parameter;
import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.jobs.Job;
import org.hortonmachine.server.jobs.JobManager;
import org.hortonmachine.server.jobs.JobState;
import org.hortonmachine.server.users.User;

/**
 * The paths in the workspace suggested for an input of a module: the matching files of the data folder,
 * of the uploads of the user and of the outputs of the last jobs of the user.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class FileSuggestions {
    /** The maximum number of suggestions. */
    public static final int MAX_SUGGESTIONS = 500;
    private static final int MAX_DEPTH = 6;
    private static final int RECENT_JOBS = 20;

    private static final Set<String> RASTER_EXTENSIONS = Set.of("tif", "tiff", "asc", "nc", "grd", "png", "jpg");
    private static final Set<String> VECTOR_EXTENSIONS = Set.of("gpkg", "shp", "geojson", "json", "kml", "gml", "csv");
    private static final Set<String> LAS_EXTENSIONS = Set.of("las", "laz");
    private static final Set<String> CSV_EXTENSIONS = Set.of("csv", "txt");

    private final WorkspacePaths paths;
    private final JobManager jobManager;

    public FileSuggestions( WorkspacePaths paths, JobManager jobManager ) {
        this.paths = paths;
        this.jobManager = jobManager;
    }

    /**
     * @param user the user.
     * @param parameter an input path parameter.
     * @return the paths in the workspace, sorted, at most {@value #MAX_SUGGESTIONS}.
     */
    public List<String> suggest( User user, Parameter parameter ) throws Exception {
        boolean folders = parameter.dataType == DataType.FOLDER;
        Set<String> extensions = parameter.extension != null ? Set.of(parameter.extension.toLowerCase(Locale.ROOT))
                : extensions(parameter.dataType);
        List<String> suggestions = new ArrayList<>();
        if (folders) {
            suggestions.add(WorkspacePaths.DATA);
        }
        collect(paths.getDataFolder(), WorkspacePaths.DATA, folders, extensions, suggestions);
        collect(paths.getUploadsFolder(user.id), WorkspacePaths.UPLOADS, folders, extensions, suggestions);
        for( Job job : jobManager.listJobs(user.id, RECENT_JOBS) ) {
            if (job.getState() == JobState.DONE) {
                collect(jobManager.getJobFolder(job.id), WorkspacePaths.JOBS + "/" + job.id, folders, extensions, suggestions);
            }
        }
        suggestions.sort(null);
        return suggestions.size() > MAX_SUGGESTIONS ? suggestions.subList(0, MAX_SUGGESTIONS) : suggestions;
    }

    private static Set<String> extensions( DataType dataType ) {
        switch( dataType ) {
        case RASTER:
            return RASTER_EXTENSIONS;
        case VECTOR:
            return VECTOR_EXTENSIONS;
        case LAS:
            return LAS_EXTENSIONS;
        case CSV:
            return CSV_EXTENSIONS;
        default:
            // any file
            return null;
        }
    }

    private static void collect( File folder, String prefix, boolean folders, Set<String> extensions, List<String> suggestions )
            throws IOException {
        if (!folder.isDirectory()) {
            return;
        }
        Path root = folder.toPath();
        try (Stream<Path> walk = Files.walk(root, MAX_DEPTH)) {
            walk.filter(path -> !path.equals(root)).filter(path -> folders ? Files.isDirectory(path) : isWanted(path, extensions))
                    .limit(MAX_SUGGESTIONS - suggestions.size()).forEach(path -> suggestions
                            .add(prefix + "/" + root.relativize(path).toString().replace('\\', '/')));
        }
    }

    private static boolean isWanted( Path path, Set<String> extensions ) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String name = path.getFileName().toString();
        if (name.equals(JobManager.PARAMS_FILE) || name.equals(JobManager.LOG_FILE)) {
            return false;
        }
        if (extensions == null) {
            return true;
        }
        int dot = name.lastIndexOf('.');
        return dot > 0 && extensions.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
}
