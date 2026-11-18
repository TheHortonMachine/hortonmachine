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

import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.netcdf.NcmlUtilities;
import org.hortonmachine.gears.io.netcdf.OmsNetcdfTemporalAggregator;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.locationtech.jts.geom.Envelope;

import com.sun.net.httpserver.HttpServer;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 * Test {@link OpendapGridReader} against a local server that works as a Hyrax DAP4 server on the Daymet subset of
 * the test data: it serves its DMR and subsets it by constraint expression.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestOpendapGridReader extends HMTestCase {

    /** The Daymet grid, to check the window with GeoTools instead of netcdf-java. */
    private static final String DAYMET_WKT = "PROJCS[\"Daymet\", GEOGCS[\"WGS 84\", DATUM[\"WGS_1984\", SPHEROID[\"WGS 84\", 6378137.0, 298.257223563]], "
            + "PRIMEM[\"Greenwich\", 0.0], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Longitude\", EAST], AXIS[\"Latitude\", NORTH]], "
            + "PROJECTION[\"Lambert_Conformal_Conic_2SP\"], PARAMETER[\"central_meridian\", -100.0], PARAMETER[\"latitude_of_origin\", 42.5], "
            + "PARAMETER[\"standard_parallel_1\", 25.0], PARAMETER[\"standard_parallel_2\", 60.0], PARAMETER[\"false_easting\", 0.0], "
            + "PARAMETER[\"false_northing\", 0.0], UNIT[\"m\", 1.0], AXIS[\"Easting\", EAST], AXIS[\"Northing\", NORTH]]";

    private File source;
    private HttpServer server;
    private String datasetUrl;
    private final AtomicInteger dataRequests = new AtomicInteger();
    private File workFolder;

    @Override
    protected void setUp() throws Exception {
        source = new File(getClass().getClassLoader().getResource("formats/netcdf/daymet_v4r1_tmax_2020_boulder.nc4").toURI());
        workFolder = Files.createTempDirectory("hm_opendap_test").toFile();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/data/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            byte[] body;
            int status = 200;
            try {
                if (path.equals("/data/daymet.nc.dmr")) {
                    body = dmr(source).getBytes(StandardCharsets.UTF_8);
                } else if (path.equals("/data/daymet.nc.dap.nc4")) {
                    dataRequests.incrementAndGet();
                    String query = URLDecoder.decode(exchange.getRequestURI().getRawQuery(), StandardCharsets.UTF_8);
                    body = subset(source, query.substring("dap4.ce=".length()));
                } else {
                    status = 404;
                    body = "not found".getBytes(StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                status = 500;
                body = e.toString().getBytes(StandardCharsets.UTF_8);
            }
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        datasetUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/data/daymet.nc";
    }

    @Override
    protected void tearDown() throws Exception {
        server.stop(0);
        FileUtilities.deleteFileOrDir(workFolder);
    }

    public void testParseRealDmr() throws Exception {
        File dmrFile = new File(getClass().getClassLoader().getResource("formats/netcdf/daymet_v4r1_tmax_2020.dmr").toURI());
        OpendapGridReader reader = new OpendapGridReader("http://unused/daymet.nc.dmr", OpendapSession.anonymous());
        assertEquals("http://unused/daymet.nc", reader.getDatasetUrl());
        reader.parseDmr(FileUtilities.readFile(dmrFile));

        assertEquals("daymet_v4_daily_na_tmax_2020.nc", reader.getDatasetName());
        OpendapVariable tmax = reader.getVariable("tmax");
        assertEquals("Float32", tmax.type());
        assertEquals(List.of("time", "y", "x"), tmax.dimensions());
        assertEquals(List.of(365, 8075, 7814), tmax.sizes());
        assertEquals("lambert_conformal_conic", tmax.attribute("grid_mapping"));
        assertEquals("lat lon", tmax.attribute("coordinates"));
        assertEquals("degrees C", tmax.attribute("units"));

        OpendapVariable mapping = reader.getVariable("lambert_conformal_conic");
        assertTrue(mapping.dimensions().isEmpty());
        assertEquals("25.,60.", mapping.attribute("standard_parallel"));
        assertEquals(List.of("y", "x"), reader.getVariable("lat").dimensions());
        assertNull(reader.getVariable("missing"));

        // unsupported grids fail before any request
        try {
            reader.findSubset("missing", null, null, null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            // time_bnds has no coordinate variable for its last dimension
            reader.findSubset("time_bnds", null, null, null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testIndexRange() throws Exception {
        double[] ascending = {0, 1, 2, 3};
        assertArrayEquals(new int[]{1, 2}, OpendapGridReader.indexRange(ascending, 0.6, 2.2));
        assertArrayEquals(new int[]{0, 3}, OpendapGridReader.indexRange(ascending, -10, 10));
        // a range inside a single cell
        assertArrayEquals(new int[]{2, 2}, OpendapGridReader.indexRange(ascending, 1.8, 1.9));
        assertNull(OpendapGridReader.indexRange(ascending, 3.6, 5));

        double[] descending = {30, 20, 10};
        assertArrayEquals(new int[]{0, 2}, OpendapGridReader.indexRange(descending, 12, 26));
        assertArrayEquals(new int[]{1, 1}, OpendapGridReader.indexRange(descending, 16, 24));

        assertEquals(1000.0, OpendapGridReader.kmToAxisUnits("m"), DELTA);
        assertEquals(1.0, OpendapGridReader.kmToAxisUnits("km"), DELTA);
    }

    private static void assertArrayEquals( int[] expected, int[] actual ) {
        assertNotNull(actual);
        assertEquals(java.util.Arrays.toString(expected), java.util.Arrays.toString(actual));
    }

    public void testSubsetDownloadAndAggregation() throws Exception {
        OpendapGridReader reader = new OpendapGridReader(datasetUrl, OpendapSession.anonymous());
        assertEquals(7, reader.getVariables().size());

        // the area between the centers of the cells 5,3 and 14,12, from the lat/lon of the file
        double[] xAxis;
        double[] yAxis;
        Envelope area = new Envelope();
        try (NetcdfFile nc = NetcdfFiles.open(source.getAbsolutePath())) {
            xAxis = toDoubles(nc.findVariable("x").read());
            yAxis = toDoubles(nc.findVariable("y").read());
            Array lats = nc.findVariable("lat").read();
            Array lons = nc.findVariable("lon").read();
            Index index = lats.getIndex();
            for( int[] cell : new int[][]{{5, 3}, {14, 12}} ) {
                index.set(cell[1], cell[0]);
                area.expandToInclude(lons.getDouble(index), lats.getDouble(index));
            }
        }

        OpendapSubset subset = reader.findSubset("tmax", area, DefaultGeographicCRS.WGS84, 2, 9, pm);
        assertEquals("x", subset.xDimension());
        assertEquals("y", subset.yDimension());
        assertEquals("time", subset.timeDimension());
        assertEquals(List.of("x", "y", "time"), subset.coordinateVariables());
        assertEquals(List.of("lat", "lon"), subset.spatialVariables());
        assertEquals(List.of("lambert_conformal_conic"), subset.otherVariables());
        assertEquals(2, subset.fromTimestep());
        assertEquals(9, subset.toTimestep());

        // the same window with the projection of GeoTools
        HMCrsTransformer transformer = new HMCrsTransformer(DefaultGeographicCRS.WGS84, CRS.parseWKT(DAYMET_WKT));
        Envelope lcc = transformer.transformDensified(area, 100);
        int[] cols = OpendapGridReader.indexRange(xAxis, lcc.getMinX(), lcc.getMaxX());
        int[] rows = OpendapGridReader.indexRange(yAxis, lcc.getMinY(), lcc.getMaxY());
        assertEquals(cols[0], subset.fromCol());
        assertEquals(cols[1], subset.toCol());
        assertEquals(rows[0], subset.fromRow());
        assertEquals(rows[1], subset.toRow());
        // the window contains the two cells and is inside the grid
        assertTrue(subset.fromCol() <= 5 && subset.toCol() >= 14 && subset.toCol() < 20);
        assertTrue(subset.fromRow() <= 3 && subset.toRow() >= 12 && subset.toRow() < 20);

        // 2 timesteps per request
        reader.setMaxValuesPerRequest(subset.cellsPerTimestep() * 2 + 1);
        dataRequests.set(0);
        File chunksFolder = new File(workFolder, "chunks");
        List<File> files = reader.download(subset, chunksFolder, pm);
        assertEquals(4, files.size());
        assertEquals(4, dataRequests.get());
        assertEquals("daymet__tmax__t00002-00003.nc4", files.get(0).getName());
        assertEquals("daymet__tmax__t00008-00008.nc4", files.get(3).getName());

        // the files present are not downloaded again
        dataRequests.set(0);
        assertEquals(files, reader.download(subset, chunksFolder, pm));
        assertEquals(0, dataRequests.get());

        // the mean of the joined files is the one of the window of the source
        File ncml = NcmlUtilities.joinExisting(files, "time", new File(workFolder, "tmax.ncml"));
        GridCoverage2D mean = mean(ncml, null, null);
        GridCoverage2D sourceMean = mean(source, 2, 9);

        ReferencedEnvelope envelope = mean.getEnvelope2D();
        assertEquals(xAxis[subset.fromCol()] - 500, envelope.getMinX(), DELTA);
        assertEquals(yAxis[subset.fromRow()] + 500, envelope.getMaxY(), DELTA);
        Raster meanData = mean.getRenderedImage().getData();
        Raster sourceData = sourceMean.getRenderedImage().getData();
        assertEquals(subset.cols(), meanData.getWidth());
        assertEquals(subset.rows(), meanData.getHeight());
        for( int row = 0; row < subset.rows(); row++ ) {
            for( int col = 0; col < subset.cols(); col++ ) {
                assertEquals(sourceData.getSampleDouble(col + subset.fromCol(), row + subset.fromRow(), 0),
                        meanData.getSampleDouble(col, row, 0), 1E-6);
            }
        }
    }

    public void testSingleTimestepTooLarge() throws Exception {
        OpendapGridReader reader = new OpendapGridReader(datasetUrl, OpendapSession.anonymous());
        OpendapSubset subset = reader.findSubset("tmax", null, null, null, null, pm);
        assertEquals(400, subset.cellsPerTimestep());
        assertEquals(10, subset.timesteps());
        reader.setMaxValuesPerRequest(399);
        try {
            reader.download(subset, workFolder, pm);
            fail("A timestep above the maximum values must fail.");
        } catch (IOException e) {
            // expected
        }
    }

    public void testDaymetUrls() throws Exception {
        assertEquals(
                "https://opendap.earthdata.nasa.gov/collections/C2532426483-ORNL_CLOUD/granules/Daymet_Daily_V4R1.daymet_v4_daily_na_srad_2025.nc",
                DaymetOpendap.dailyUrl(DaymetOpendap.SRAD, DaymetOpendap.NORTH_AMERICA, 2025));
        try {
            DaymetOpendap.dailyUrl("temperature", DaymetOpendap.NORTH_AMERICA, 2025);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    private GridCoverage2D mean( File file, Integer from, Integer to ) throws Exception {
        OmsNetcdfTemporalAggregator aggregator = new OmsNetcdfTemporalAggregator();
        aggregator.pm = pm;
        aggregator.inPath = file.getAbsolutePath();
        aggregator.pGridName = "tmax";
        if (from != null)
            aggregator.pFromTimestep = from;
        aggregator.pToTimestep = to;
        aggregator.process();
        return aggregator.outRaster;
    }

    private static double[] toDoubles( Array array ) {
        double[] values = new double[(int) array.getSize()];
        for( int i = 0; i < values.length; i++ ) {
            values[i] = array.getDouble(i);
        }
        return values;
    }

    // ---------------------------------------------------------------------------------
    // the Hyrax emulation
    // ---------------------------------------------------------------------------------

    private static String dap4Type( DataType type ) {
        switch( type ) {
        case FLOAT:
            return "Float32";
        case DOUBLE:
            return "Float64";
        case SHORT:
            return "Int16";
        case INT:
            return "Int32";
        case BYTE:
            return "Int8";
        case STRING:
        case CHAR:
            return "String";
        default:
            throw new IllegalArgumentException("Type not handled by the test: " + type);
        }
    }

    private static String xml( String text ) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * The DMR of a NetCDF file, as Hyrax writes it.
     */
    private static String dmr( File file ) throws IOException {
        try (NetcdfFile nc = NetcdfFiles.open(file.getAbsolutePath())) {
            StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
            sb.append("<Dataset xmlns=\"http://xml.opendap.org/ns/DAP/4.0#\" dapVersion=\"4.0\" dmrVersion=\"1.0\" name=\"daymet.nc\">\n");
            for( Dimension dimension : nc.getRootGroup().getDimensions() ) {
                sb.append("  <Dimension name=\"").append(dimension.getShortName()).append("\" size=\"").append(dimension.getLength())
                        .append("\"/>\n");
            }
            for( Variable variable : nc.getVariables() ) {
                String type = dap4Type(variable.getDataType());
                sb.append("  <").append(type).append(" name=\"").append(variable.getShortName()).append("\">\n");
                for( Dimension dimension : variable.getDimensions() ) {
                    sb.append("    <Dim name=\"/").append(dimension.getShortName()).append("\"/>\n");
                }
                for( Attribute attribute : variable.attributes() ) {
                    // the HDF5 internal attributes, as _ChunkSizes, are not in the DMR
                    if (attribute.getShortName().startsWith("_") && !attribute.getShortName().equals("_FillValue")) {
                        continue;
                    }
                    sb.append("    <Attribute name=\"").append(xml(attribute.getShortName())).append("\" type=\"")
                            .append(attribute.isString() ? "String" : dap4Type(attribute.getDataType())).append("\">\n");
                    for( int i = 0; i < attribute.getLength(); i++ ) {
                        String value = attribute.isString() ? attribute.getStringValue(i) : attribute.getNumericValue(i).toString();
                        sb.append("      <Value>").append(xml(value)).append("</Value>\n");
                    }
                    sb.append("    </Attribute>\n");
                }
                sb.append("  </").append(type).append(">\n");
            }
            sb.append("</Dataset>\n");
            return sb.toString();
        }
    }

    /**
     * The NetCDF response of a DAP4 constraint expression, as <code>/v[0:2][1:3:9][4:5];/x[4:5];/crs</code>, written
     * as NetCDF-3 (netcdf-java can't write NetCDF-4 without the native library).
     */
    private static byte[] subset( File file, String constraint ) throws Exception {
        File out = File.createTempFile("hm_opendap_response", ".nc");
        try (NetcdfFile nc = NetcdfFiles.open(file.getAbsolutePath())) {
            NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf3(out.getAbsolutePath());
            builder.addAttribute(new Attribute("Conventions", "CF-1.6"));
            Map<String, Integer> dimensions = new LinkedHashMap<>();
            List<Variable> variables = new ArrayList<>();
            List<Section> sections = new ArrayList<>();
            Pattern rangePattern = Pattern.compile("\\[([^\\]]*)\\]");
            for( String part : constraint.split(";") ) {
                int bracket = part.indexOf('[');
                String name = (bracket < 0 ? part : part.substring(0, bracket)).substring(1);
                Variable variable = nc.findVariable(name);
                if (variable == null) {
                    throw new IllegalArgumentException("No variable " + name);
                }
                List<Range> ranges = new ArrayList<>();
                if (bracket >= 0) {
                    Matcher matcher = rangePattern.matcher(part.substring(bracket));
                    while( matcher.find() ) {
                        String[] values = matcher.group(1).split(":");
                        int first = Integer.parseInt(values[0]);
                        int last = Integer.parseInt(values[values.length - 1]);
                        int stride = values.length == 3 ? Integer.parseInt(values[1]) : 1;
                        ranges.add(new Range(first, last, stride));
                    }
                } else {
                    for( Dimension dimension : variable.getDimensions() ) {
                        ranges.add(new Range(0, dimension.getLength() - 1));
                    }
                }
                StringBuilder dims = new StringBuilder();
                for( int i = 0; i < variable.getRank(); i++ ) {
                    String dimName = variable.getDimension(i).getShortName();
                    if (dimensions.putIfAbsent(dimName, ranges.get(i).length()) == null) {
                        builder.addDimension(dimName, ranges.get(i).length());
                    }
                    dims.append(dimName).append(" ");
                }
                Variable.Builder< ? > variableBuilder = builder.addVariable(name, variable.getDataType(), dims.toString().trim());
                for( Attribute attribute : variable.attributes() ) {
                    if (!attribute.getShortName().startsWith("_") || attribute.getShortName().equals("_FillValue")) {
                        variableBuilder.addAttribute(attribute);
                    }
                }
                variables.add(variable);
                sections.add(new Section(ranges));
            }
            try (NetcdfFormatWriter writer = builder.build()) {
                for( int i = 0; i < variables.size(); i++ ) {
                    Variable variable = variables.get(i);
                    Array data = variable.getRank() == 0 ? variable.read() : variable.read(sections.get(i)).copy();
                    writer.write(writer.findVariable(variable.getShortName()), data);
                }
            }
            return Files.readAllBytes(out.toPath());
        } finally {
            out.delete();
        }
    }
}
