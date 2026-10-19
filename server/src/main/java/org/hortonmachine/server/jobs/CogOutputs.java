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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hortonmachine.gears.io.cog.HMCogWriter;
import org.hortonmachine.server.ServerConfig;
import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.stac.StacFiles;

/**
 * The GeoTIFF outputs of the successful jobs become Cloud Optimized GeoTIFFs, if the
 * {@link ServerConfig.Setting#COG_OUTPUTS setting} says so, so that the clients of the api read them in parts.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class CogOutputs implements JobListener {
    private static final Logger LOGGER = Logger.getLogger(CogOutputs.class.getName());

    private final ServerConfig config;

    /**
     * @param config the configuration, whose settings change while the server runs.
     */
    public CogOutputs( ServerConfig config ) {
        this.config = config;
    }

    @Override
    public void afterRun( Job job, File jobFolder, long endTime, Consumer<String> task ) throws Exception {
        if (!config.cogOutputs) {
            return;
        }
        List<String> warnings = new ArrayList<>();
        for( File file : outputFiles(job, jobFolder) ) {
            String name = file.getName().toLowerCase();
            if ((name.endsWith(".tif") || name.endsWith(".tiff")) && !StacFiles.isCog(file)) {
                task.accept("Converting " + file.getName() + " to COG");
                try {
                    toCog(file);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "The output " + file + " can't be converted to COG.", e);
                    warnings.add("Warning: " + file.getName() + " was not converted to COG: " + e.getMessage());
                }
            }
        }
        if (!warnings.isEmpty()) {
            throw new Exception(String.join("\n", warnings));
        }
    }

    /**
     * Convert a GeoTIFF to COG in place: written next to it, then moved over it.
     */
    public static void toCog( File file ) throws Exception {
        File cog = new File(file.getParentFile(), file.getName() + ".cog.tmp");
        try {
            new HMCogWriter().convert2Cog(null, file.getAbsolutePath(), cog.getAbsolutePath());
            Files.move(cog.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            // statistics of the old file would win over those in the COG
            new File(file.getAbsolutePath() + ".aux.xml").delete();
        } finally {
            cog.delete();
        }
    }

    /**
     * @return the files written by a job as outputs, each once.
     */
    static List<File> outputFiles( Job job, File jobFolder ) {
        String ownFolder = WorkspacePaths.JOBS + "/" + job.id + "/";
        List<File> files = new ArrayList<>();
        for( String value : job.getOutputs().values() ) {
            if (value.startsWith(ownFolder)) {
                File file = new File(jobFolder, WorkspacePaths.filePath(value.substring(ownFolder.length())));
                if (file.isFile() && !files.contains(file)) {
                    files.add(file);
                }
            }
        }
        return files;
    }
}
