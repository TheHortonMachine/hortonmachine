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
package org.hortonmachine.gears.io.opendap;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.locationtech.jts.geom.Envelope;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.projection.LatLonProjection;

/**
 * Reader of the grids of an OPeNDAP dataset served with the DAP4 protocol, as by the Hyrax servers of NASA Earthdata.
 *
 * <p>Only the window of an area and a range of timesteps is downloaded, as NetCDF-4 files of the server, split in
 * time so that every request stays below a number of values. The files can be joined along time with
 * {@link org.hortonmachine.gears.io.netcdf.NcmlUtilities} and read as a single dataset by the NetCDF modules.</p>
 *
 * <pre>
 * OpendapSession session = OpendapSession.fromNetrc(OpendapSession.EARTHDATA_LOGIN_HOST);
 * OpendapGridReader reader = new OpendapGridReader(url, session);
 * OpendapSubset subset = reader.findSubset("tmax", area, DefaultGeographicCRS.WGS84, null, null, pm);
 * List&lt;File&gt; files = reader.download(subset, folder, pm);
 * </pre>
 *
 * <p>The grid variables need one dimensional coordinate variables for their x and y dimensions, the last two, and an
 * optional time dimension before them. The window is found with the projection of the grid as netcdf-java reads it
 * from a small subsampled probe of the grid, so all the projections netcdf-java supports can be used.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class OpendapGridReader {

    /** The default maximum number of values of the grid in a single request, about 100 MB of floats. */
    public static final long DEFAULT_MAX_VALUES_PER_REQUEST = 25_000_000L;

    /** The number of cells per side of the probe used to read the projection. */
    private static final int PROBE_SIZE = 32;

    /** The number of points per side of the area used to find its bounds in the grid. */
    private static final int DENSIFY_POINTS = 100;

    private static final Set<String> DAP4_TYPES = Set.of("Char", "Byte", "Int8", "UInt8", "Int16", "UInt16", "Int32", "UInt32",
            "Int64", "UInt64", "Float32", "Float64", "String", "URL", "Opaque", "Enum");

    private final String datasetUrl;
    private final OpendapSession session;
    private long maxValuesPerRequest = DEFAULT_MAX_VALUES_PER_REQUEST;

    private String datasetName;
    private List<OpendapVariable> variables;

    /**
     * @param datasetUrl the url of the dataset, without the extensions of the DAP4 responses, as
     *          <code>https://opendap.earthdata.nasa.gov/collections/C2532426483-ORNL_CLOUD/granules/Daymet_Daily_V4R1.daymet_v4_daily_na_tmax_2020.nc</code>.
     * @param session the session, with the authentication if needed.
     */
    public OpendapGridReader( String datasetUrl, OpendapSession session ) {
        String url = datasetUrl.trim();
        for( String extension : new String[]{".dmr.xml", ".dmr", ".dap.nc4", ".dap", ".html"} ) {
            if (url.endsWith(extension)) {
                url = url.substring(0, url.length() - extension.length());
                break;
            }
        }
        this.datasetUrl = url;
        this.session = session;
    }

    /**
     * @param maxValuesPerRequest the maximum number of values of the grid in a single request, to stay below the
     *          limits of the server.
     */
    public void setMaxValuesPerRequest( long maxValuesPerRequest ) {
        this.maxValuesPerRequest = maxValuesPerRequest;
    }

    /**
     * @return the url of the dataset.
     */
    public String getDatasetUrl() {
        return datasetUrl;
    }

    /**
     * @return the name of the dataset, as given by the server.
     * @throws IOException if the DMR can't be read.
     */
    public String getDatasetName() throws IOException {
        readDmr();
        return datasetName;
    }

    /**
     * @return the variables of the dataset.
     * @throws IOException if the DMR can't be read.
     */
    public List<OpendapVariable> getVariables() throws IOException {
        readDmr();
        return variables;
    }

    /**
     * @param name the name of the variable.
     * @return the variable, or <code>null</code> if the dataset doesn't have it.
     * @throws IOException if the DMR can't be read.
     */
    public OpendapVariable getVariable( String name ) throws IOException {
        for( OpendapVariable variable : getVariables() ) {
            if (variable.name().equals(name)) {
                return variable;
            }
        }
        return null;
    }

    private void readDmr() throws IOException {
        if (variables == null) {
            String dmr = session.getText(datasetUrl + ".dmr");
            parseDmr(dmr);
        }
    }

    /**
     * Parse the variables of the root of a DMR. Groups are not supported.
     */
    void parseDmr( String dmr ) throws IOException {
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(new StringReader(dmr)));
        } catch (Exception e) {
            throw new IOException("Unable to parse the DMR of " + datasetUrl + ": " + e.getMessage(), e);
        }
        Element root = document.getDocumentElement();
        if (!root.getNodeName().equals("Dataset")) {
            throw new IOException("Not a DMR, the root element is " + root.getNodeName() + ": " + datasetUrl);
        }
        String name = root.getAttribute("name");
        Map<String, Integer> dimensions = new LinkedHashMap<>();
        List<OpendapVariable> parsed = new ArrayList<>();
        NodeList children = root.getChildNodes();
        for( int i = 0; i < children.getLength(); i++ ) {
            if (!(children.item(i) instanceof Element element)) {
                continue;
            }
            if (element.getNodeName().equals("Dimension")) {
                dimensions.put(element.getAttribute("name"), Integer.parseInt(element.getAttribute("size")));
            }
        }
        for( int i = 0; i < children.getLength(); i++ ) {
            if (!(children.item(i) instanceof Element element) || !DAP4_TYPES.contains(element.getNodeName())) {
                continue;
            }
            List<String> dims = new ArrayList<>();
            List<Integer> sizes = new ArrayList<>();
            Map<String, String> attributes = new LinkedHashMap<>();
            NodeList variableChildren = element.getChildNodes();
            for( int j = 0; j < variableChildren.getLength(); j++ ) {
                if (!(variableChildren.item(j) instanceof Element child)) {
                    continue;
                }
                if (child.getNodeName().equals("Dim")) {
                    String dimName = child.getAttribute("name");
                    if (dimName.isEmpty()) {
                        // an anonymous dimension
                        dims.add("");
                        sizes.add(Integer.parseInt(child.getAttribute("size")));
                    } else {
                        String shortName = dimName.startsWith("/") ? dimName.substring(1) : dimName;
                        Integer size = dimensions.get(shortName);
                        if (size == null) {
                            throw new IOException("Unknown dimension " + dimName + " of " + element.getAttribute("name"));
                        }
                        dims.add(shortName);
                        sizes.add(size);
                    }
                } else if (child.getNodeName().equals("Attribute") && !child.getAttribute("type").equals("Container")) {
                    attributes.put(child.getAttribute("name"), attributeValue(child));
                }
            }
            parsed.add(new OpendapVariable(element.getAttribute("name"), element.getNodeName(), Collections.unmodifiableList(dims),
                    Collections.unmodifiableList(sizes), Collections.unmodifiableMap(attributes)));
        }
        datasetName = name;
        variables = Collections.unmodifiableList(parsed);
    }

    private static String attributeValue( Element attribute ) {
        if (attribute.hasAttribute("value")) {
            return attribute.getAttribute("value");
        }
        List<String> values = new ArrayList<>();
        NodeList children = attribute.getChildNodes();
        for( int i = 0; i < children.getLength(); i++ ) {
            Node child = children.item(i);
            if (child instanceof Element element && element.getNodeName().equals("Value")) {
                values.add(element.hasAttribute("value") ? element.getAttribute("value") : element.getTextContent().trim());
            }
        }
        return String.join(",", values);
    }

    /**
     * Find the window of a grid variable that covers an area.
     *
     * @param variable the name of the grid variable.
     * @param area the area, or <code>null</code> for the whole grid.
     * @param areaCrs the crs of the area, if <code>null</code> lon/lat WGS84.
     * @param fromTimestep the first timestep, if <code>null</code> the first of the dataset.
     * @param toTimestep the timestep at which to stop, excluded, if <code>null</code> the end of the dataset.
     * @param pm the monitor, can be <code>null</code>.
     * @return the subset.
     * @throws Exception if the grid is not supported, the area is outside of it or the server answers with an error.
     */
    public OpendapSubset findSubset( String variable, Envelope area, CoordinateReferenceSystem areaCrs, Integer fromTimestep,
            Integer toTimestep, IHMProgressMonitor pm ) throws Exception {
        if (pm == null) {
            pm = new DummyProgressMonitor();
        }
        OpendapVariable grid = getVariable(variable);
        if (grid == null) {
            throw new IllegalArgumentException("The dataset has no variable " + variable + ": " + datasetUrl);
        }
        int rank = grid.dimensions().size();
        if (rank != 2 && rank != 3) {
            throw new IllegalArgumentException("Only grids of dimensions (y, x) or (time, y, x) are supported, " + variable
                    + " has " + grid.dimensions());
        }
        String xDim = grid.dimensions().get(rank - 1);
        String yDim = grid.dimensions().get(rank - 2);
        String timeDim = rank == 3 ? grid.dimensions().get(0) : null;
        int nx = grid.sizes().get(rank - 1);
        int ny = grid.sizes().get(rank - 2);
        OpendapVariable xVariable = getVariable(xDim);
        OpendapVariable yVariable = getVariable(yDim);
        if (xVariable == null || yVariable == null) {
            throw new IllegalArgumentException("The grid " + variable + " has no coordinate variables for its dimensions " + yDim
                    + " and " + xDim);
        }

        // the timesteps
        int from = 0;
        int to = 1;
        if (timeDim != null) {
            int nt = grid.sizes().get(0);
            from = fromTimestep == null ? 0 : fromTimestep;
            to = toTimestep == null ? nt : Math.min(toTimestep, nt);
            if (from < 0 || from >= to) {
                throw new IllegalArgumentException("Wrong timestep range " + fromTimestep + " - " + toTimestep + ", the grid has "
                        + nt + " timesteps.");
            }
        }

        // the variables to download with the grid
        List<String> coordinateVariables = new ArrayList<>(List.of(xDim, yDim));
        if (timeDim != null && getVariable(timeDim) != null) {
            coordinateVariables.add(timeDim);
        }
        List<String> spatialVariables = new ArrayList<>();
        String coordinates = grid.attribute("coordinates");
        if (coordinates != null) {
            for( String coordinate : coordinates.trim().split("[\\s,]+") ) {
                OpendapVariable auxiliary = getVariable(coordinate);
                if (auxiliary != null && auxiliary.dimensions().equals(List.of(yDim, xDim))) {
                    spatialVariables.add(coordinate);
                }
            }
        }
        List<String> otherVariables = new ArrayList<>();
        String gridMapping = grid.attribute("grid_mapping");
        if (gridMapping != null && getVariable(gridMapping.trim()) != null) {
            otherVariables.add(gridMapping.trim());
        }

        int fromCol = 0;
        int toCol = nx - 1;
        int fromRow = 0;
        int toRow = ny - 1;
        if (area != null) {
            File probeFile = File.createTempFile("opendap_probe_", ".nc4");
            File axesFile = File.createTempFile("opendap_axes_", ".nc4");
            try {
                // the projection of the grid as netcdf-java reads it, from a subsampled probe
                pm.message("Reading the projection of " + variable + "...");
                int strideX = Math.max(1, nx / PROBE_SIZE);
                int strideY = Math.max(1, ny / PROBE_SIZE);
                String xRange = "[0:" + strideX + ":" + (nx - 1) + "]";
                String yRange = "[0:" + strideY + ":" + (ny - 1) + "]";
                StringBuilder probe = new StringBuilder("/").append(variable);
                if (timeDim != null) {
                    probe.append("[0]");
                }
                probe.append(yRange).append(xRange);
                probe.append(";/").append(xDim).append(xRange).append(";/").append(yDim).append(yRange);
                if (coordinateVariables.contains(timeDim)) {
                    probe.append(";/").append(timeDim).append("[0]");
                }
                for( String other : otherVariables ) {
                    probe.append(";/").append(other);
                }
                session.download(dataUrl(probe.toString()), probeFile, null);
                Projection projection;
                try (GridDataset gds = GridDataset.open(probeFile.getAbsolutePath())) {
                    GridDatatype probeGrid = gds.findGridDatatype(variable);
                    if (probeGrid == null) {
                        throw new IllegalArgumentException("netcdf-java doesn't read " + variable + " as a grid.");
                    }
                    GridCoordSystem coordSys = probeGrid.getCoordinateSystem();
                    projection = coordSys.getProjection();
                }
                boolean isLatLon = projection == null || projection instanceof LatLonProjection;
                // netcdf-java projections work in km
                double factor = isLatLon ? 1 : kmToAxisUnits(xVariable.attribute("units"));

                // the full axes
                session.download(dataUrl("/" + xDim + ";/" + yDim), axesFile, null);
                double[] xAxis;
                double[] yAxis;
                try (NetcdfFile nc = NetcdfFiles.open(axesFile.getAbsolutePath())) {
                    xAxis = toDoubles(nc.findVariable(xDim).read());
                    yAxis = toDoubles(nc.findVariable(yDim).read());
                }

                // the bounds of the area in the grid
                Envelope lonLat = toLonLat(area, areaCrs);
                boolean longitudes360 = isLatLon && max(xAxis) > 180;
                Envelope gridArea = new Envelope();
                for( int i = 0; i <= DENSIFY_POINTS; i++ ) {
                    double fraction = (double) i / DENSIFY_POINTS;
                    double lon = lonLat.getMinX() + fraction * lonLat.getWidth();
                    double lat = lonLat.getMinY() + fraction * lonLat.getHeight();
                    double[][] boundary = {{lon, lonLat.getMinY()}, {lon, lonLat.getMaxY()}, {lonLat.getMinX(), lat},
                            {lonLat.getMaxX(), lat}};
                    for( double[] point : boundary ) {
                        double x;
                        double y;
                        if (isLatLon || projection == null) {
                            x = longitudes360 && point[0] < 0 ? point[0] + 360 : point[0];
                            y = point[1];
                        } else {
                            ProjectionPoint projected = projection.latLonToProj(LatLonPoint.create(point[1], point[0]));
                            x = projected.getX() * factor;
                            y = projected.getY() * factor;
                        }
                        if (!Double.isNaN(x) && !Double.isNaN(y) && !Double.isInfinite(x) && !Double.isInfinite(y)) {
                            gridArea.expandToInclude(x, y);
                        }
                    }
                }
                int[] cols = indexRange(xAxis, gridArea.getMinX(), gridArea.getMaxX());
                int[] rows = indexRange(yAxis, gridArea.getMinY(), gridArea.getMaxY());
                if (gridArea.isNull() || cols == null || rows == null) {
                    throw new IllegalArgumentException("The area is outside of the grid " + variable + ".");
                }
                fromCol = cols[0];
                toCol = cols[1];
                fromRow = rows[0];
                toRow = rows[1];
            } finally {
                probeFile.delete();
                axesFile.delete();
            }
        }
        OpendapSubset subset = new OpendapSubset(variable, xDim, yDim, timeDim, fromCol, toCol, fromRow, toRow, from, to,
                Collections.unmodifiableList(coordinateVariables), Collections.unmodifiableList(spatialVariables),
                Collections.unmodifiableList(otherVariables));
        pm.message("Subset of " + subset);
        return subset;
    }

    private static Envelope toLonLat( Envelope area, CoordinateReferenceSystem areaCrs ) throws Exception {
        if (areaCrs == null || CRS.equalsIgnoreMetadata(areaCrs, DefaultGeographicCRS.WGS84)) {
            return area;
        }
        HMCrsTransformer transformer = new HMCrsTransformer(areaCrs, DefaultGeographicCRS.WGS84);
        return transformer.transformDensified(area, DENSIFY_POINTS);
    }

    /**
     * @return the factor from km to the given units of a projected axis.
     */
    static double kmToAxisUnits( String units ) {
        String u = units == null ? "" : units.trim().toLowerCase(Locale.ROOT);
        switch( u ) {
        case "m":
        case "meter":
        case "meters":
        case "metre":
        case "metres":
            return 1000;
        case "km":
        case "kilometer":
        case "kilometers":
        case "kilometre":
        case "kilometres":
            return 1;
        default:
            throw new IllegalArgumentException("Unsupported units of the projected x axis: " + units);
        }
    }

    /**
     * Find the cells of an axis that overlap a range.
     *
     * @param axis the monotonic values of the cell centers.
     * @param min the start of the range.
     * @param max the end of the range.
     * @return the first and last index of the cells overlapping the range, or <code>null</code> if none does.
     */
    static int[] indexRange( double[] axis, double min, double max ) {
        int first = -1;
        int last = -1;
        int n = axis.length;
        for( int i = 0; i < n; i++ ) {
            double half;
            if (n == 1) {
                half = 0;
            } else if (i == 0) {
                half = Math.abs(axis[1] - axis[0]) / 2;
            } else if (i == n - 1) {
                half = Math.abs(axis[n - 1] - axis[n - 2]) / 2;
            } else {
                half = Math.min(Math.abs(axis[i] - axis[i - 1]), Math.abs(axis[i + 1] - axis[i])) / 2;
            }
            if (axis[i] + half >= min && axis[i] - half <= max) {
                if (first < 0) {
                    first = i;
                }
                last = i;
            }
        }
        return first < 0 ? null : new int[]{first, last};
    }

    private static double[] toDoubles( Array array ) {
        double[] values = new double[(int) array.getSize()];
        for( int i = 0; i < values.length; i++ ) {
            values[i] = array.getDouble(i);
        }
        return values;
    }

    private static double max( double[] values ) {
        double max = Double.NEGATIVE_INFINITY;
        for( double value : values ) {
            max = Math.max(max, value);
        }
        return max;
    }

    /**
     * Download a subset, split in time so that every request stays below the maximum number of values. Files already
     * present are kept, so an interrupted download can be resumed.
     *
     * @param subset the subset, as found by {@link #findSubset}.
     * @param folder the folder in which to write the files.
     * @param pm the monitor, can be <code>null</code>. Its cancel state stops the download.
     * @return the files, in time order.
     * @throws IOException if a single timestep is above the maximum number of values or the server answers with an
     *          error.
     */
    public List<File> download( OpendapSubset subset, File folder, IHMProgressMonitor pm ) throws IOException {
        if (pm == null) {
            pm = new DummyProgressMonitor();
        }
        long cells = subset.cellsPerTimestep();
        if (cells > maxValuesPerRequest) {
            throw new IOException("A single timestep of the area has " + cells + " cells, more than the " + maxValuesPerRequest
                    + " values of a request: use a smaller area, or download the whole files and process them locally.");
        }
        int stepsPerChunk = (int) Math.max(1, Math.min(subset.timesteps(), maxValuesPerRequest / cells));
        folder.mkdirs();
        String baseName = getDatasetName();
        int dot = baseName.lastIndexOf('.');
        if (dot > 0) {
            baseName = baseName.substring(0, dot);
        }
        String rowRange = "[" + subset.fromRow() + ":" + subset.toRow() + "]";
        String colRange = "[" + subset.fromCol() + ":" + subset.toCol() + "]";

        List<File> files = new ArrayList<>();
        int chunks = (subset.timesteps() + stepsPerChunk - 1) / stepsPerChunk;
        pm.beginTask("Downloading " + subset.variable() + " with " + chunks + (chunks == 1 ? " request..." : " requests..."),
                chunks);
        try {
            for( int t0 = subset.fromTimestep(); t0 < subset.toTimestep(); t0 += stepsPerChunk ) {
                if (pm.isCanceled()) {
                    throw new IOException("Download canceled by user");
                }
                int t1 = Math.min(subset.toTimestep(), t0 + stepsPerChunk) - 1;
                String timeRange = "[" + t0 + ":" + t1 + "]";
                String name = baseName + "__" + subset.variable();
                if (subset.timeDimension() != null) {
                    name += String.format("__t%05d-%05d", t0, t1);
                }
                File file = new File(folder, name + ".nc4");
                files.add(file);
                if (file.exists()) {
                    pm.message("Already downloaded: " + file.getName());
                    pm.worked(1);
                    continue;
                }

                StringBuilder ce = new StringBuilder("/").append(subset.variable());
                if (subset.timeDimension() != null) {
                    ce.append(timeRange);
                }
                ce.append(rowRange).append(colRange);
                for( String coordinate : subset.coordinateVariables() ) {
                    ce.append(";/").append(coordinate);
                    if (coordinate.equals(subset.xDimension())) {
                        ce.append(colRange);
                    } else if (coordinate.equals(subset.yDimension())) {
                        ce.append(rowRange);
                    } else {
                        ce.append(timeRange);
                    }
                }
                for( String spatial : subset.spatialVariables() ) {
                    ce.append(";/").append(spatial).append(rowRange).append(colRange);
                }
                for( String other : subset.otherVariables() ) {
                    ce.append(";/").append(other);
                }
                long size = session.download(dataUrl(ce.toString()), file, pm);
                pm.message("Downloaded " + file.getName() + String.format(" (%.1f MB)", size / 1E6));
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
        return files;
    }

    /**
     * @param constraint a DAP4 constraint expression, as <code>/tmax[0:9][100:200][300:400];/x[300:400]</code>.
     * @return the url of the NetCDF-4 response of the constraint.
     */
    public String dataUrl( String constraint ) {
        return datasetUrl + ".dap.nc4?dap4.ce=" + URLEncoder.encode(constraint, StandardCharsets.UTF_8);
    }
}
