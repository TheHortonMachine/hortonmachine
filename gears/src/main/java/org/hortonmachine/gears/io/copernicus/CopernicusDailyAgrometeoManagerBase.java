package org.hortonmachine.gears.io.copernicus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.CompressionUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;

import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.Out;
import oms3.annotations.UI;

/**
 * Base class for HMModules that download, locally cache and convert to GeoTIFF a single
 * day of Copernicus CDS "Agrometeorological indicators" (AgERA5, dataset id
 * <code>sis-agrometeorological-indicators</code>) data, for a given variable.
 *
 * <p>
 * The cached data is always the <b>whole available (global) extent</b> - the region
 * inputs (<code>pNorth</code>/<code>pSouth</code>/<code>pEast</code>/<code>pWest</code>)
 * never affect what's requested from CDS or how the cache is keyed, only how the cached
 * global raster is cropped on read. This means a second run asking for a different region
 * of the same variable/day is served entirely from the already-cached global file - no new
 * CDS job at all - instead of triggering a fresh download per distinct region.
 * </p>
 *
 * <p>
 * On every run, a deterministic local cache path is computed from the variable, statistic,
 * version and timestamp; if the converted tif already exists there, it is the sole cache
 * marker and is read (possibly region-cropped) without any network call at all. Otherwise
 * the data is submitted as an OGC API - Processes job via {@link OGCProcessesManager},
 * polled to completion, downloaded, unzipped and converted from NetCDF to GeoTIFF into that
 * same deterministic path; the downloaded zip and unzipped netcdf are only temporary working
 * files, deleted once the tif is written.
 * </p>
 *
 * <p>
 * This dataset is daily-only (its <code>statistic</code> values are all 24-hour/day-time/
 * night-time aggregates - there is no hourly/monthly/yearly aggregate to request), which is
 * why there is no time-resolution input here and why concrete subclasses are named
 * <code>CopernicusDaily*Manager</code>.
 * </p>
 *
 * @author Andrea Antonello
 */
public abstract class CopernicusDailyAgrometeoManagerBase extends HMModel {

    public static final String DATASET_ID = "sis-agrometeorological-indicators";
    public static final String BASE_URL = "https://cds.climate.copernicus.eu/api/retrieve/v1";

    // required synoptic samples the daily statistic is aggregated from, not a resolution choice - always all 5
    private static final List<String> ALL_TIMES = List.of("06_00", "09_00", "12_00", "15_00", "18_00");

    // AgERA5's documented native grid spacing - passed to OmsRasterReader when cropping so
    // it performs a plain crop at the source resolution, not a resample to some other grid
    private static final double NATIVE_RESOLUTION_DEGREES = 0.1;

    @Description("Copernicus CDS personal API token.")
    @In
    public String pApiToken;

    @Description("The day to fetch, format yyyy-MM-dd.")
    @In
    public String pTimestamp;

    @Description("Region north boundary (WGS84), used only to crop the cached global raster on read - never affects what's downloaded or cached. If north/south/east/west are all null, the whole global extent is returned.")
    @UI(HMConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description("Region south boundary (WGS84), used only to crop the cached global raster on read.")
    @UI(HMConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description("Region east boundary (WGS84), used only to crop the cached global raster on read.")
    @UI(HMConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description("Region west boundary (WGS84), used only to crop the cached global raster on read.")
    @UI(HMConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description("Dataset version.")
    @In
    public String pVersion = "2_0";

    @Description("Local folder used as cache/repository for downloaded and converted data.")
    @UI(HMConstants.FOLDEROUT_UI_HINT)
    @In
    public String pRepoFolder;

    @Description("Seconds to wait between job status polls.")
    @In
    public Integer pPollIntervalSeconds = 10;

    @Description("Minutes to wait for the job to complete before giving up.")
    @In
    public Integer pTimeoutMinutes = 60;

    @Description("Path to the (possibly cached) converted GeoTIFF of the original bounds and resolution.")
    @Out
    public File outputTif;

    @Description("The raster of the requested size, read only once regardless of whether it was cached or freshly downloaded.")
    @Out
    public GridCoverage2D outputRaster;

    @Description("True if this run found an existing cached file and made no network calls at all.")
    @Out
    public boolean wasCached;

    /**
     * @return the CDS <code>variable</code> value to request.
     */
    protected abstract String getVariable();

    /**
     * @return the CDS <code>statistic</code> value to request, or <code>null</code> if this
     *         variable has no valid statistic (some AgERA5 variables are already
     *         daily-cumulated quantities with nothing to aggregate - CDS's own constraints
     *         confirm this per variable).
     */
    protected abstract String getStatistic();

    public void process() throws Exception {
        checkNull(pApiToken, pTimestamp, pRepoFolder);

        File basePath = computeBasePath();
        File tifFile = new File(basePath.getPath() + ".tif");

        // the tif is the cache marker: this always produces exactly one tif per
        // request, so its presence alone means no network call is needed at all
        if (tifFile.exists()) {
            outputRaster = readGlobalRasterCropped(tifFile);
            outputTif = tifFile;
            wasCached = true;
            return;
        }

        Map<String, Object> inputs = buildInputs();

        File workDir = new File(basePath.getPath() + "_download_tmp");
        Files.createDirectories(workDir.toPath());
        try {
            File zipFile = new File(workDir, "result.zip");

            try (OGCProcessesManager client = new OGCProcessesManager(BASE_URL, pm)) {
                client.open();
                client.setApiToken(pApiToken);

                OGCJob job = client.submitJob(DATASET_ID, inputs);
                job = client.waitForCompletion(job, pPollIntervalSeconds * 1000L, pTimeoutMinutes * 60_000L);

                client.downloadResult(job.getJobId(), null, zipFile.getAbsolutePath());
            }

            CompressionUtilities.unzipFolder(zipFile.getAbsolutePath(), workDir.getAbsolutePath(), false);

            File ncFile = findNetcdfFile(workDir);
            if (ncFile == null) {
                throw new IOException("No NetCDF file found in the downloaded zip: " + zipFile);
            }

            GridCoverage2D raster = convertToRaster(ncFile);
            if (raster == null) {
                throw new IOException("NetCDF conversion produced no grid from: " + ncFile);
            }

            Files.createDirectories(tifFile.getParentFile().toPath());
            dumpRaster(raster, tifFile.getAbsolutePath());

            outputRaster = readGlobalRasterCropped(tifFile);
            outputTif = tifFile;
            wasCached = false;
        } finally {
            FileUtilities.deleteFileOrDir(workDir);
        }
    }

    /**
     * Reads the cached global raster, cropped to the requested region if one was given
     * (via {@link OmsRasterReader}'s own bounded-read support), or in full otherwise.
     */
    private GridCoverage2D readGlobalRasterCropped( File globalTif ) throws Exception {
        OmsRasterReader reader = new OmsRasterReader();
        reader.file = globalTif.getAbsolutePath();
        reader.pm = pm;
        if (hasRegion()) {
            reader.pNorth = pNorth;
            reader.pSouth = pSouth;
            reader.pEast = pEast;
            reader.pWest = pWest;
            // a target resolution is required whenever bounds are set - use the source's
            // own native resolution so this is a plain crop, not a resample
            reader.pXres = NATIVE_RESOLUTION_DEGREES;
            reader.pYres = NATIVE_RESOLUTION_DEGREES;
        }
        reader.process();
        return reader.outRaster;
    }

    private GridCoverage2D convertToRaster( File ncFile ) throws Exception {
        OmsNetcdf2GridCoverageConverter converter = new OmsNetcdf2GridCoverageConverter();
        converter.inPath = ncFile.getAbsolutePath();
        converter.pm = pm;
        converter.initProcess();

        GridCoverage2D raster = null;
        while( converter.doProcess ) {
            converter.process();
            raster = converter.outRaster;
        }
        return raster;
    }

    private File findNetcdfFile( File folder ) {
        File[] files = folder.listFiles();
        if (files == null) {
            return null;
        }
        for( File f : files ) {
            if (f.isDirectory()) {
                File found = findNetcdfFile(f);
                if (found != null) {
                    return found;
                }
            } else {
                String lower = f.getName().toLowerCase(Locale.ROOT);
                if (lower.endsWith(".nc") || lower.endsWith(".netcdf")) {
                    return f;
                }
            }
        }
        return null;
    }

    private boolean hasRegion() {
        return pNorth != null && pSouth != null && pEast != null && pWest != null;
    }

    /**
     * The CDS request is always for the whole global extent - region inputs never affect
     * it, only the on-read crop in {@link #readGlobalRasterCropped(File)}.
     */
    private Map<String, Object> buildInputs() {
        LocalDate date = LocalDate.parse(pTimestamp);
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("variable", getVariable());
        String statistic = getStatistic();
        if (statistic != null) {
            inputs.put("statistic", List.of(statistic));
        }
        inputs.put("year", List.of(String.valueOf(date.getYear())));
        inputs.put("month", List.of(String.format(Locale.ROOT, "%02d", date.getMonthValue())));
        inputs.put("day", List.of(String.format(Locale.ROOT, "%02d", date.getDayOfMonth())));
        inputs.put("time", ALL_TIMES);
        inputs.put("version", pVersion);
        return inputs;
    }

    /**
     * @return the shared, extension-less path both the cached (always global) zip and the
     *         converted tif are named from (e.g. {@code <path>.zip} / {@code <path>.tif}).
     */
    private File computeBasePath() {
        LocalDate date = LocalDate.parse(pTimestamp);
        String yyyyMMdd = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        File dir = new File(pRepoFolder, getVariable());
        String statistic = getStatistic();
        if (statistic != null) {
            // only variables with an actual statistic choice get a folder for it -
            // variables with none (getStatistic() == null) have nothing to disambiguate
            dir = new File(dir, statistic);
        }
        return new File(new File(dir, pVersion), yyyyMMdd);
    }

}
