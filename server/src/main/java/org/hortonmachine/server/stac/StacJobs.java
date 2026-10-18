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
package org.hortonmachine.server.stac;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hortonmachine.Version;
import org.hortonmachine.gears.io.cog.HMCogWriter;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.ServerConfig;
import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.db.ServerDb;
import org.hortonmachine.server.jobs.Job;
import org.hortonmachine.server.jobs.JobListener;
import org.hortonmachine.server.web.Html;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The STAC items of the jobs: at the end of each successful job its GeoTIFF outputs become COGs, if the
 * {@link ServerConfig.Setting#COG_OUTPUTS setting} says so, and its item is written in the job folder as
 * {@value #ITEM_FILE} and put in the index; it leaves the index with the job.
 *
 * <p>An item has all the files of the outputs as assets, with hrefs relative to the job folder.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class StacJobs implements JobListener {
    private static final Logger LOGGER = Logger.getLogger(StacJobs.class.getName());

    /** The item of a job, in its folder. */
    public static final String ITEM_FILE = "stac-item.json";
    /** The start of the ids of the collections of the jobs, followed by the module. */
    public static final String COLLECTION_PREFIX = "jobs-";

    public static final String STAC_VERSION = "1.1.0";
    public static final List<String> EXTENSIONS = List.of( //
            "https://stac-extensions.github.io/projection/v2.0.0/schema.json", //
            "https://stac-extensions.github.io/raster/v1.1.0/schema.json", //
            "https://stac-extensions.github.io/file/v2.1.0/schema.json", //
            "https://stac-extensions.github.io/processing/v1.2.0/schema.json", //
            "https://stac-extensions.github.io/timestamps/v1.1.0/schema.json");

    private final ServerDb db;
    private final ServerConfig config;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * @param config the configuration, whose settings change while the server runs.
     */
    public StacJobs( ServerDb db, ServerConfig config ) {
        this.db = db;
        this.config = config;
    }

    /**
     * @return the id of the collection of the jobs of a module.
     */
    public static String collection( String module ) {
        return COLLECTION_PREFIX + module;
    }

    @Override
    public void afterRun( Job job, File jobFolder, long endTime, Consumer<String> task ) throws Exception {
        List<String> warnings = new ArrayList<>();
        List<Output> outputs = outputFiles(job, jobFolder);
        if (config.cogOutputs) {
            for( Output output : outputs ) {
                File file = output.file;
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
        }
        task.accept("Writing the catalog item");
        ObjectNode item = jobItem(job, jobFolder, outputs, endTime);
        String json = mapper.writeValueAsString(item);
        FileUtilities.writeFile(json, new File(jobFolder, ITEM_FILE));
        db.putStacItem(toRecord(job, jobFolder, item, json, endTime));
        if (!warnings.isEmpty()) {
            throw new JobWarningsException(warnings);
        }
    }

    @Override
    public void deleted( String jobId ) {
        try {
            db.deleteStacItem(jobId);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "The item of the job " + jobId + " can't be removed from the catalog.", e);
        }
    }

    /**
     * The warnings of the end of a job, which is still successful.
     */
    public static class JobWarningsException extends Exception {
        private static final long serialVersionUID = 1L;
        public final List<String> warnings;

        JobWarningsException( List<String> warnings ) {
            super(String.join("\n", warnings));
            this.warnings = warnings;
        }
    }

    /**
     * Convert a GeoTIFF to COG in place: written next to it, then moved over it.
     */
    static void toCog( File file ) throws Exception {
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
     * A file written by a job as output.
     */
    static class Output {
        final String name;
        final File file;

        Output( String name, File file ) {
            this.name = name;
            this.file = file;
        }
    }

    /**
     * @return the files written by the job as outputs, in the order of the outputs.
     */
    static List<Output> outputFiles( Job job, File jobFolder ) {
        String ownFolder = WorkspacePaths.JOBS + "/" + job.id + "/";
        List<Output> files = new ArrayList<>();
        for( Map.Entry<String, String> entry : job.getOutputs().entrySet() ) {
            String value = entry.getValue();
            if (value.startsWith(ownFolder)) {
                File file = new File(jobFolder, WorkspacePaths.filePath(value.substring(ownFolder.length())));
                if (file.isFile() && files.stream().noneMatch(output -> output.file.equals(file))) {
                    files.add(new Output(entry.getKey(), file));
                }
            }
        }
        return files;
    }

    /**
     * Build the item of a successful job.
     *
     * @param endTime the end of the run of the module.
     */
    ObjectNode jobItem( Job job, File jobFolder, List<Output> outputs, long endTime ) throws Exception {
        ObjectNode item = mapper.createObjectNode();
        item.put("type", "Feature");
        item.put("stac_version", STAC_VERSION);
        ArrayNode extensions = item.putArray("stac_extensions");
        EXTENSIONS.forEach(extensions::add);
        item.put("id", job.id);
        item.put("collection", collection(job.module));

        ObjectNode assets = mapper.createObjectNode();
        Geometry footprint = null;
        String folderPath = jobFolder.getAbsoluteFile().toPath().normalize().toString();
        for( Output output : outputs ) {
            File file = output.file;
            StacFiles.Description description = StacFiles.describe(file, file.getName());
            String relative = file.getAbsoluteFile().toPath().normalize().toString().substring(folderPath.length() + 1)
                    .replace('\\', '/');
            ObjectNode asset = assets.putObject(output.name);
            asset.put("href", "./" + Html.urlPath(relative));
            asset.setAll(description.asset);
            if (description.footprint != null) {
                footprint = footprint == null ? description.footprint : footprint.union(description.footprint);
            }
        }
        putGeometry(item, footprint);

        ObjectNode properties = item.putObject("properties");
        properties.put("title", job.module + ", " + Html.formatTime(job.created));
        long startTime = job.getStarted() != null ? job.getStarted() : job.created;
        properties.put("datetime", time(endTime));
        properties.put("start_datetime", time(startTime));
        properties.put("end_datetime", time(endTime));
        properties.put("created", time(endTime));
        properties.put("updated", time(endTime));
        int ttlDays = config.jobTtlDays;
        if (ttlDays > 0) {
            properties.put("expires", time(endTime + TimeUnit.DAYS.toMillis(ttlDays)));
        }
        properties.putObject("processing:software").put("HortonMachine", Version.getVersion());
        properties.put("processing:lineage", "The module " + job.module + " of the HortonMachine server.");
        properties.put("processing:datetime", time(endTime));
        properties.put("hm:module", job.module);
        ObjectNode parameters = properties.putObject("hm:parameters");
        job.parameters.forEach(parameters::put);

        item.putArray("links");
        item.set("assets", assets);
        return item;
    }

    private StacRecord toRecord( Job job, File jobFolder, ObjectNode item, String json, long endTime ) {
        long startTime = job.getStarted() != null ? job.getStarted() : job.created;
        double[] bbox = null;
        if (item.has("bbox")) {
            ArrayNode array = (ArrayNode) item.get("bbox");
            bbox = new double[]{array.get(0).asDouble(), array.get(1).asDouble(), array.get(2).asDouble(), array.get(3).asDouble()};
        }
        long size = 0;
        long modified = 0;
        for( File file : jobFolder.listFiles() ) {
            size += file.length();
            modified = Math.max(modified, file.lastModified());
        }
        return new StacRecord(job.id, collection(job.module), job.userId, WorkspacePaths.JOBS + "/" + job.id, size, modified, bbox,
                startTime, endTime, json);
    }

    private static String time( long millis ) {
        return Instant.ofEpochMilli(millis).toString();
    }

    /**
     * Put the geometry and the bbox of an item, both null if it has no footprint.
     */
    static void putGeometry( ObjectNode item, Geometry footprint ) {
        if (footprint == null || footprint.isEmpty()) {
            item.putNull("geometry");
            return;
        }
        item.set("geometry", GeoJson.toGeoJson(footprint));
        Envelope envelope = footprint.getEnvelopeInternal();
        item.putArray("bbox").add(envelope.getMinX()).add(envelope.getMinY()).add(envelope.getMaxX()).add(envelope.getMaxY());
    }
}
