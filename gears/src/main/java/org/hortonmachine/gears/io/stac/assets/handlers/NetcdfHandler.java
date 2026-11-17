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
package org.hortonmachine.gears.io.stac.assets.handlers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter;
import org.hortonmachine.gears.io.stac.HMStacAsset;
import org.hortonmachine.gears.io.stac.PlanetaryComputerMicrosoft;
import org.hortonmachine.gears.io.stac.assets.IHMStacAssetHandler;
import org.hortonmachine.gears.io.stac.auth.HMStacAccess;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

import ucar.nc2.Dimension;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.projection.LatLonProjection;

/**
 * Handler of NetCDF assets (NetCDF-3 and NetCDF-4/HDF5), read with netcdf-java.
 *
 * <p>A file usually holds several grid variables, so the grids are listed with {@link #listGrids(IHMProgressMonitor)}
 * and read by name and timestep with {@link #readGrid(String, int, int, IHMProgressMonitor, GridConsumer)}, through
 * {@link OmsNetcdf2GridCoverageConverter}: projected grids of known crs keep it, the others are resampled to lat/long
 * WGS84.</p>
 *
 * <p>Plain http(s) assets are read in place with range requests, so that listing the grids reads only the headers.
 * Protected assets, s3 ones and those of servers that don't support range requests are downloaded once to a
 * temporary file, kept for the session.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class NetcdfHandler implements IHMStacAssetHandler {
    public final static String[] ACCEPTED_TYPES = {"application/netcdf", "application/x-netcdf", "application/x-netcdf4",
            "application/netcdf4"};

    /** The extensions accepted when the catalog doesn't declare a specific media type. */
    private final static String[] ACCEPTED_EXTENSIONS = {".nc", ".nc4", ".netcdf", ".cdf"};

    /** The downloaded copies of the assets that can't be read in place, by href, kept for the session. */
    private static final Map<String, File> DOWNLOADED = new ConcurrentHashMap<>();

    /**
     * The info about a grid variable of a NetCDF file.
     *
     * @param name the full name of the grid, to read it.
     * @param description the description of the variable, or <code>null</code>.
     * @param units the units of the values, or <code>null</code>.
     * @param dimensions the dimensions of the variable with their sizes, as "time=12, lat=180, lon=360".
     * @param dates the dates of the timesteps, empty if the grid has no time axis.
     * @param projection the projection of the grid, as found by netcdf-java.
     * @param readable <code>true</code> if the grid can be converted to a raster.
     * @param note a note for the user, as why the grid can't be read or that only its first level is read, or
     *          <code>null</code>.
     */
    public record GridInfo( String name, String description, String units, String dimensions, List<Date> dates,
            String projection, boolean readable, String note ) {

        /**
         * @return the number of timesteps of the grid, 1 if it has no time axis.
         */
        public int timestepsCount() {
            return dates.isEmpty() ? 1 : dates.size();
        }
    }

    /**
     * Takes the rasters read from a grid, one per timestep.
     */
    @FunctionalInterface
    public interface GridConsumer {
        /**
         * @param timestep the index of the timestep.
         * @param date the date of the timestep, <code>null</code> if the grid has no time.
         * @param coverage the raster of the timestep.
         * @throws Exception
         */
        void accept( int timestep, Date date, GridCoverage2D coverage ) throws Exception;
    }

    private HMStacAsset asset;
    private boolean supported = false;
    private String assetUrl;

    @Override
    public void initialize( HMStacAsset asset ) throws IOException {
        this.asset = asset;
        this.assetUrl = asset.getAssetNode().get("href").stringValue(null);
        String type = asset.getType() == null ? "" : asset.getType().replace(" ", "").toLowerCase();
        for( String acceptedType : ACCEPTED_TYPES ) {
            if (type.contains(acceptedType)) {
                supported = true;
                return;
            }
        }
        // catalogs often don't declare the type of netcdf files or give a generic one
        if (assetUrl != null && (type.isEmpty() || type.equals("application/octet-stream"))) {
            String path = assetUrl.toLowerCase();
            int query = path.indexOf('?');
            if (query >= 0)
                path = path.substring(0, query);
            for( String extension : ACCEPTED_EXTENSIONS ) {
                if (path.endsWith(extension)) {
                    supported = true;
                    return;
                }
            }
        }
    }

    @Override
    public boolean supports() {
        return supported;
    }

    @Override
    public HMStacAsset getAsset() {
        return asset;
    }

    @Override
    public String getAssetUrl() {
        return assetUrl;
    }

    private void checkSupported() {
        if (!supported) {
            throw new UnsupportedOperationException("Asset type " + asset.getType() + " is not supported by NetcdfHandler.");
        }
    }

    /**
     * List the grid variables of the file, the ones with at least two dimensions.
     *
     * @param monitor the monitor, can be <code>null</code>.
     * @return the grids.
     * @throws Exception if the file can't be opened.
     */
    public List<GridInfo> listGrids( IHMProgressMonitor monitor ) throws Exception {
        checkSupported();
        IHMProgressMonitor pm = monitor != null ? monitor : new DummyProgressMonitor();
        return withLocation(pm, location -> {
            try (GridDataset gds = GridDataset.open(location)) {
                List<GridInfo> grids = new ArrayList<>();
                for( GridDatatype grid : gds.getGrids() ) {
                    List<Dimension> dimensions = grid.getDimensions();
                    if (dimensions.size() < 2)
                        continue;
                    grids.add(toGridInfo(grid));
                }
                return grids;
            }
        });
    }

    private static GridInfo toGridInfo( GridDatatype grid ) {
        String dims = grid.getDimensions().stream().map(d -> d.getShortName() + "=" + d.getLength())
                .collect(Collectors.joining(", "));
        GridCoordSystem coordSys = grid.getCoordinateSystem();
        List<Date> dates = new ArrayList<>();
        if (coordSys.hasTimeAxis1D()) {
            for( CalendarDate date : coordSys.getTimeAxis1D().getCalendarDates() ) {
                dates.add(date.toDate());
            }
        }
        Object projection = coordSys.getProjection();
        String projectionName = projection == null ? "unknown"
                : projection instanceof LatLonProjection ? "lat/long" : projection.getClass().getSimpleName();
        CoordinateAxis xAxis = coordSys.getXHorizAxis();
        CoordinateAxis yAxis = coordSys.getYHorizAxis();
        boolean readable = xAxis != null && yAxis != null && xAxis.getRank() == 1 && yAxis.getRank() == 1;
        String note = null;
        if (!readable) {
            note = "two dimensional or missing x/y axes (e.g. a satellite swath), not supported";
        } else if (coordSys.getVerticalAxis() != null && coordSys.getVerticalAxis().getSize() > 1) {
            note = "only the first of " + coordSys.getVerticalAxis().getSize() + " levels is read";
        }
        return new GridInfo(grid.getFullName(), grid.getDescription(), grid.getUnitsString(), dims,
                Collections.unmodifiableList(dates), projectionName, readable, note);
    }

    /**
     * Read the timesteps of a grid as rasters.
     *
     * @param gridName the full name of the grid, as in {@link GridInfo#name()}.
     * @param fromTimestep the index of the first timestep to read.
     * @param toTimestep the index of the timestep at which to stop, excluded.
     * @param monitor the monitor, can be <code>null</code>. If canceled, the reading stops after the current timestep.
     * @param consumer takes the raster of every timestep.
     * @throws Exception
     */
    public void readGrid( String gridName, int fromTimestep, int toTimestep, IHMProgressMonitor monitor, GridConsumer consumer )
            throws Exception {
        checkSupported();
        IHMProgressMonitor pm = monitor != null ? monitor : new DummyProgressMonitor();
        withLocation(pm, location -> {
            OmsNetcdf2GridCoverageConverter converter = new OmsNetcdf2GridCoverageConverter();
            converter.pm = pm;
            converter.inPath = location;
            converter.pGridName = gridName;
            converter.pFromTimestep = fromTimestep;
            converter.pToTimestep = toTimestep;
            try {
                converter.initProcess();
                int timestep = fromTimestep;
                while( converter.doProcess && !pm.isCanceled() ) {
                    converter.process();
                    consumer.accept(timestep++, converter.currentDate, converter.outRaster);
                }
            } finally {
                converter.close();
            }
            return null;
        });
    }

    @Override
    public <T> T read( Class<T> targetType, IHMProgressMonitor monitor ) throws Exception {
        checkSupported();
        if (targetType.isAssignableFrom(GridCoverage2D.class)) {
            // the first timestep of the first readable grid
            for( GridInfo grid : listGrids(monitor) ) {
                if (grid.readable()) {
                    return targetType.cast(readFirstTimestep(grid.name(), monitor));
                }
            }
            return null;
        } else if (targetType.isAssignableFrom(File.class)) {
            File tempFile = File.createTempFile("netcdf_asset_", ".nc");
            downloadAsset(tempFile.getAbsolutePath(), monitor);
            return targetType.cast(tempFile);
        }
        return null;
    }

    /**
     * Read the first timestep of every readable grid, by grid name.
     */
    @Override
    public <T> Map<String, T> readAll( Class<T> targetType, IHMProgressMonitor monitor ) throws Exception {
        checkSupported();
        Map<String, T> objectsMap = new LinkedHashMap<>();
        if (targetType.isAssignableFrom(GridCoverage2D.class)) {
            for( GridInfo grid : listGrids(monitor) ) {
                if (grid.readable()) {
                    objectsMap.put(grid.name(), targetType.cast(readFirstTimestep(grid.name(), monitor)));
                }
            }
        } else {
            T result = read(targetType, monitor);
            if (result != null)
                objectsMap.put(asset.getId(), result);
        }
        return objectsMap;
    }

    private GridCoverage2D readFirstTimestep( String gridName, IHMProgressMonitor monitor ) throws Exception {
        GridCoverage2D[] result = new GridCoverage2D[1];
        readGrid(gridName, 0, 1, monitor, ( t, d, coverage ) -> result[0] = coverage);
        return result[0];
    }

    /** An action on the location netcdf-java opens the asset from. */
    @FunctionalInterface
    private interface LocationAction<T> {
        T run( String location ) throws Exception;
    }

    /**
     * Run an action on the location of the asset: in place if possible, else on a downloaded copy. A plain http(s)
     * asset that fails to be read in place, e.g. from a server without range requests, is downloaded and the action
     * run again.
     */
    private <T> T withLocation( IHMProgressMonitor pm, LocationAction<T> action ) throws Exception {
        File downloaded = DOWNLOADED.get(assetUrl);
        if (downloaded != null && downloaded.exists())
            return action.run(downloaded.getAbsolutePath());

        if (assetUrl.startsWith("file:"))
            return action.run(Paths.get(URI.create(assetUrl)).toString());
        boolean http = assetUrl.startsWith("http://") || assetUrl.startsWith("https://");
        if (!http && !assetUrl.contains("://"))
            return action.run(assetUrl);

        HMStacAccess access = asset.getAccess();
        boolean authenticated = access != null && access.isAuthenticated(assetUrl);
        if (http && !authenticated) {
            String url = PlanetaryComputerMicrosoft.isAzureBlob(assetUrl)
                    ? PlanetaryComputerMicrosoft.getHrefWithToken(assetUrl)
                    : assetUrl;
            try {
                return action.run(url);
            } catch (Exception e) {
                if (pm.isCanceled())
                    throw e;
                pm.message("Unable to read " + assetUrl + " in place (" + e.getMessage() + "), downloading it.");
            }
        }
        return action.run(download(pm).getAbsolutePath());
    }

    /**
     * Download the asset to a temporary file, kept for the session.
     */
    private File download( IHMProgressMonitor pm ) throws Exception {
        File file = File.createTempFile("stac_netcdf_", ".nc");
        file.deleteOnExit();
        try {
            downloadAsset(file.getAbsolutePath(), pm);
        } catch (Exception e) {
            file.delete();
            throw e;
        }
        // netcdf-java tells the formats by their content, but an html error page would give an obscure error
        try (InputStream is = Files.newInputStream(file.toPath())) {
            byte[] start = is.readNBytes(64);
            String text = new String(start, StandardCharsets.ISO_8859_1).trim().toLowerCase(Locale.ROOT);
            if (text.startsWith("<")) {
                file.delete();
                throw new IOException("The server returned a web page instead of a NetCDF file: " + assetUrl);
            }
        }
        DOWNLOADED.put(assetUrl, file);
        return file;
    }
}
