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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hortonmachine.cli.ModuleDescriptor.DataType;
import org.hortonmachine.cli.ModuleDescriptor.Parameter;
import org.hortonmachine.server.ServerConfig;
import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.jobs.Job;
import org.hortonmachine.server.jobs.JobManager;
import org.hortonmachine.server.jobs.JobState;
import org.hortonmachine.server.users.User;

/**
 * The paths in the workspace suggested for an input of a module, in this order:
 * <ol>
 * <li>the matching outputs of the jobs done by the user, all of them for an administrator, newest job first,
 *     labelled with the module and the time of the job;</li>
 * <li>the matching files of the uploads of the user;</li>
 * <li>the matching files of the shared data folder.</li>
 * </ol>
 * The suggestions are at most {@link #setMaxSuggestions(int) a maximum}: each source has a share of it, the jobs a third,
 * the uploads half of what is left, the data the rest, so that a large data folder doesn't hide the outputs of the jobs.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class FileSuggestions {
    private static final int MAX_DEPTH = 6;

    private static final Set<String> RASTER_EXTENSIONS = Set.of("tif", "tiff", "asc", "nc", "grd", "png", "jpg");
    private static final Set<String> VECTOR_EXTENSIONS = Set.of("gpkg", "shp", "geojson", "json", "kml", "gml", "csv");
    private static final Set<String> LAS_EXTENSIONS = Set.of("las", "laz");
    private static final Set<String> CSV_EXTENSIONS = Set.of("csv", "txt");

    private final WorkspacePaths paths;
    private final JobManager jobManager;
    // changed by the settings page while running
    private volatile int maxSuggestions = ServerConfig.DEFAULT_MAX_SUGGESTIONS;

    /**
     * A suggested path.
     */
    public static class Suggestion {
        /** The path in the workspace, as given to the module. */
        public final String value;
        /** What the path is, as the module and time of its job, or <code>null</code>. */
        public final String label;

        public Suggestion( String value, String label ) {
            this.value = value;
            this.label = label;
        }
    }

    public FileSuggestions( WorkspacePaths paths, JobManager jobManager ) {
        this.paths = paths;
        this.jobManager = jobManager;
    }

    /**
     * @param maxSuggestions the maximum number of suggestions for an input.
     */
    public void setMaxSuggestions( int maxSuggestions ) {
        this.maxSuggestions = maxSuggestions;
    }

    /**
     * @param user the user.
     * @param parameter an input path parameter.
     * @return the suggestions, in the order of the sources, at most the maximum.
     */
    public List<Suggestion> suggest( User user, Parameter parameter ) throws Exception {
        int max = maxSuggestions;
        boolean folders = parameter.dataType == DataType.FOLDER;
        Set<String> extensions = parameter.extension != null ? Set.of(parameter.extension.toLowerCase(Locale.ROOT))
                : extensions(parameter.dataType);

        // the outputs of the jobs, newest first
        List<Suggestion> jobOutputs = new ArrayList<>();
        int jobsShare = Math.max(1, max / 3);
        List<Job> jobs = jobManager.listFinishedJobs(user.isAdmin() ? null : user.id);
        jobs.sort(Comparator.comparingLong((Job job) -> job.created).reversed());
        for( Job job : jobs ) {
            if (jobOutputs.size() >= jobsShare) {
                break;
            }
            if (job.getState() == JobState.DONE) {
                String label = job.module + ", " + Html.formatTime(job.created);
                for( String path : collect(jobManager.getJobFolder(job.id), WorkspacePaths.JOBS + "/" + job.id, folders, extensions,
                        jobsShare - jobOutputs.size()) ) {
                    jobOutputs.add(new Suggestion(path, label));
                }
            }
        }

        List<Suggestion> suggestions = new ArrayList<>(jobOutputs);
        int uploadsShare = Math.max(1, (max - suggestions.size()) / 2);
        for( String path : collect(paths.getUploadsFolder(user.id), WorkspacePaths.UPLOADS, folders, extensions, uploadsShare) ) {
            suggestions.add(new Suggestion(path, null));
        }
        if (folders) {
            suggestions.add(new Suggestion(WorkspacePaths.DATA, null));
        }
        for( String path : collect(paths.getDataFolder(), WorkspacePaths.DATA, folders, extensions, max - suggestions.size()) ) {
            suggestions.add(new Suggestion(path, null));
        }
        return suggestions;
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

    /**
     * @return the matching paths of a folder, sorted, at most the given number.
     */
    private static List<String> collect( File folder, String prefix, boolean folders, Set<String> extensions, int max )
            throws IOException {
        if (!folder.isDirectory() || max <= 0) {
            return List.of();
        }
        Path root = folder.toPath();
        try (Stream<Path> walk = Files.walk(root, MAX_DEPTH)) {
            return walk.filter(path -> !path.equals(root)).filter(path -> folders ? Files.isDirectory(path) : isWanted(path, extensions))
                    .map(path -> prefix + "/" + root.relativize(path).toString().replace('\\', '/')).sorted().limit(max)
                    .collect(Collectors.toList());
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
