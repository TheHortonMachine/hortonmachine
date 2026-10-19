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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hortonmachine.cli.ModuleDescriptor.DataType;
import org.hortonmachine.cli.ModuleDescriptor.Parameter;
import org.hortonmachine.server.ServerConfig;
import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.files.FileOrigin;
import org.hortonmachine.server.files.FileOrigins;
import org.hortonmachine.server.users.User;

/**
 * The paths in the workspace suggested for an input of a module, in this order:
 * <ol>
 * <li>the matching files of the area of the user, the outputs of the jobs labelled with the module and the time;</li>
 * <li>the matching files of the shared area;</li>
 * <li>for an administrator, the matching files of the areas of the other users.</li>
 * </ol>
 * The suggestions are at most {@link #setMaxSuggestions(int) a maximum}: the own area has half of it, so that a large
 * shared area doesn't hide the own files.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class FileSuggestions {
    private static final Logger LOGGER = Logger.getLogger(FileSuggestions.class.getName());
    private static final int MAX_DEPTH = 6;

    private static final Set<String> RASTER_EXTENSIONS = Set.of("tif", "tiff", "asc", "nc", "grd", "png", "jpg");
    private static final Set<String> VECTOR_EXTENSIONS = Set.of("gpkg", "shp", "geojson", "json", "kml", "gml", "csv");
    private static final Set<String> LAS_EXTENSIONS = Set.of("las", "laz");
    private static final Set<String> CSV_EXTENSIONS = Set.of("csv", "txt");

    private final WorkspacePaths paths;
    private final FileOrigins origins;
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

    public FileSuggestions( WorkspacePaths paths, FileOrigins origins ) {
        this.paths = paths;
        this.origins = origins;
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

        List<Suggestion> suggestions = new ArrayList<>();
        for( String path : collect(paths.getAreaFolder(user.username), areaPath(user.username), folders, extensions,
                Math.max(1, max / 2)) ) {
            suggestions.add(new Suggestion(path, label(path)));
        }
        for( String path : collect(paths.getAreaFolder(WorkspacePaths.SHARED), areaPath(WorkspacePaths.SHARED), folders, extensions,
                max - suggestions.size()) ) {
            suggestions.add(new Suggestion(path, null));
        }
        if (user.isAdmin()) {
            File[] areas = paths.getDataFolder().listFiles(File::isDirectory);
            if (areas != null) {
                Arrays.sort(areas);
                for( File area : areas ) {
                    String name = area.getName();
                    if (!name.equals(user.username) && !name.equals(WorkspacePaths.SHARED) && WorkspacePaths.isArea(name)) {
                        for( String path : collect(area, areaPath(name), folders, extensions, max - suggestions.size()) ) {
                            suggestions.add(new Suggestion(path, null));
                        }
                    }
                }
            }
        }
        return suggestions;
    }

    /**
     * @return the folders a user can write the outputs of a job in: those of the own area, for an administrator also
     *         those of the shared area; at most the maximum.
     */
    public List<String> suggestOutputFolders( User user ) throws IOException {
        int max = maxSuggestions;
        List<String> folders = new ArrayList<>(collect(paths.getAreaFolder(user.username), areaPath(user.username), true, null, max));
        if (user.isAdmin()) {
            folders.addAll(collect(paths.getAreaFolder(WorkspacePaths.SHARED), areaPath(WorkspacePaths.SHARED), true, null,
                    max - folders.size()));
        }
        return folders;
    }

    private static String areaPath( String area ) {
        return WorkspacePaths.DATA + "/" + area;
    }

    /**
     * @return the module and time of the job that wrote a file, or <code>null</code>.
     */
    private String label( String path ) {
        try {
            FileOrigin origin = origins.get(path);
            if (origin != null && origin.origin.equals(FileOrigin.PROCESSED)) {
                return origin.module + ", " + Html.formatTime(origin.created);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "The origin of " + path + " can't be read.", e);
        }
        return null;
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
        if (extensions == null) {
            return true;
        }
        int dot = name.lastIndexOf('.');
        return dot > 0 && extensions.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
}
