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
package org.hortonmachine.cli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.modules.HMTestCase;
import org.hortonmachine.modules.HMTestMaps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests of the command line.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestHmCli extends HMTestCase {

    private ByteArrayOutputStream outBytes;
    private ByteArrayOutputStream errBytes;
    private File tmpFolder;

    @Override
    protected void setUp() throws Exception {
        tmpFolder = Files.createTempDirectory("hm-cli-test").toFile();
    }

    @Override
    protected void tearDown() throws Exception {
        File[] files = tmpFolder.listFiles();
        if (files != null) {
            for( File file : files ) {
                file.delete();
            }
        }
        tmpFolder.delete();
    }

    private int cli( String... args ) {
        outBytes = new ByteArrayOutputStream();
        errBytes = new ByteArrayOutputStream();
        return new HmCli(new PrintStream(outBytes, true, StandardCharsets.UTF_8),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8)).run(args);
    }

    private String out() {
        return outBytes.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return errBytes.toString(StandardCharsets.UTF_8);
    }

    private File writeElevation() throws Exception {
        File elevationFile = new File(tmpFolder, "dtm_test.tif");
        GridCoverage2D elevation = CoverageUtilities.buildCoverage("elevation", HMTestMaps.mapData,
                HMTestMaps.envelopeParams, HMTestMaps.crs, true);
        OmsRasterWriter.writeRaster(elevationFile.getAbsolutePath(), elevation);
        return elevationFile;
    }

    public void testList() {
        assertEquals(HmCli.EXIT_OK, cli("list"));
        assertTrue(out().contains("Pitfiller"));
        assertTrue(out().contains("Gradient"));
        // no Oms modules
        assertFalse(out().contains("OmsPitfiller"));

        assertEquals(HmCli.EXIT_OK, cli("list", "pitfill"));
        assertTrue(out().contains("Pitfiller"));
        assertFalse(out().contains("Gradient"));
    }

    public void testHelp() {
        assertEquals(HmCli.EXIT_OK, cli("help", "Gradient"));
        assertTrue(out().contains("--inElev=<raster>"));
        assertTrue(out().contains("--doDegrees[=true|false]"));
        assertTrue(out().contains("One of: Finite Differences, Horn, Evans."));
    }

    public void testDescribe() throws Exception {
        assertEquals(HmCli.EXIT_OK, cli("describe", "Gradient"));
        JsonNode root = new ObjectMapper().readTree(out());
        assertEquals(HmCli.DESCRIBE_FORMAT, root.get("format").asInt());
        JsonNode module = root.get("modules").get(0);
        assertEquals("Gradient", module.get("name").asText());
        assertEquals("org.hortonmachine.modules.Gradient", module.get("class").asText());

        JsonNode parameters = module.get("parameters");
        assertEquals(4, parameters.size());
        assertParameter(parameters.get(0), "inElev", "input", "raster");
        assertParameter(parameters.get(1), "pMode", "parameter", "choice");
        assertEquals(3, parameters.get(1).get("choices").size());
        assertEquals("Finite Differences", parameters.get(1).get("default").asText());
        assertParameter(parameters.get(2), "doDegrees", "parameter", "boolean");
        assertFalse(parameters.get(2).get("default").asBoolean());
        // the kind of data written is read from the Oms module
        assertParameter(parameters.get(3), "outSlope", "output", "raster");

        assertEquals(HmCli.EXIT_OK, cli("describe"));
        root = new ObjectMapper().readTree(out());
        assertTrue(root.get("modules").size() > 100);
    }

    private void assertParameter( JsonNode parameter, String name, String kind, String type ) {
        assertEquals(name, parameter.get("name").asText());
        assertEquals(kind, parameter.get("kind").asText());
        assertEquals(type, parameter.get("type").asText());
    }

    public void testRun() throws Exception {
        File elevationFile = writeElevation();
        File pitFile = new File(tmpFolder, "pit_test.tif");

        int exitCode = cli("run", "Pitfiller", "--inElev=" + elevationFile.getAbsolutePath(),
                "--outPit=" + pitFile.getAbsolutePath());
        assertEquals(err(), HmCli.EXIT_OK, exitCode);
        assertTrue(out().contains(CliProgressMonitor.PROGRESS_PREFIX + "100%"));
        assertTrue(out().contains(HmCli.OUTPUT_PREFIX + "outPit = " + pitFile.getAbsolutePath()));

        GridCoverage2D pit = OmsRasterReader.readRaster(pitFile.getAbsolutePath());
        checkMatrixEqual(pit.getRenderedImage(), HMTestMaps.pitData, 0);
    }

    public void testRunWithParamsFile() throws Exception {
        File elevationFile = writeElevation();
        File slopeFile = new File(tmpFolder, "slope_test.tif");
        File paramsFile = new File(tmpFolder, "params.json");
        String json = "{\"inElev\": \"" + elevationFile.getAbsolutePath() + "\", \"doDegrees\": true, \"outSlope\": \""
                + slopeFile.getAbsolutePath() + "\"}";
        Files.writeString(paramsFile.toPath(), json);

        // the command line overrides the file
        int exitCode = cli("run", "Gradient", "--params=" + paramsFile.getAbsolutePath(), "--pMode=Horn");
        assertEquals(err(), HmCli.EXIT_OK, exitCode);
        assertTrue(out().contains("Horn"));
        assertTrue(slopeFile.exists());
    }

    public void testWrongUsage() throws Exception {
        File elevationFile = writeElevation();
        String inElev = "--inElev=" + elevationFile.getAbsolutePath();

        assertEquals(HmCli.EXIT_USAGE, cli());
        assertEquals(HmCli.EXIT_USAGE, cli("foo"));

        assertEquals(HmCli.EXIT_USAGE, cli("run", "gradien"));
        assertTrue(err().contains("Did you mean: Gradient?"));

        assertEquals(HmCli.EXIT_USAGE, cli("run", "Gradient", "--inElevv=x"));
        assertTrue(err().contains("has no parameter inElevv"));

        assertEquals(HmCli.EXIT_USAGE, cli("run", "Gradient", inElev, "--pMode=foo"));
        assertTrue(err().contains("must be one of"));

        assertEquals(HmCli.EXIT_USAGE, cli("run", "Gradient", inElev, "--doDegrees=maybe"));
        assertTrue(err().contains("not true or false"));

        assertEquals(HmCli.EXIT_USAGE, cli("run", "Gradient", "--inElev=" + new File(tmpFolder, "missing.tif")));
        assertTrue(err().contains("doesn't exist"));

        assertEquals(HmCli.EXIT_USAGE, cli("run", "Gradient", inElev, "--outSlope=" + new File(tmpFolder, "nofolder/out.tif")));
        assertTrue(err().contains("The folder of the output"));

        // a number that is not a number
        assertEquals(HmCli.EXIT_USAGE, cli("run", "CutOut", "--pMax=abc"));
        assertTrue(err().contains("not a valid"));
    }

    public void testModuleFailure() throws Exception {
        // a missing mandatory input makes the module fail
        assertEquals(HmCli.EXIT_FAILURE, cli("run", "Gradient", "--outSlope=" + new File(tmpFolder, "out.tif")));
        assertTrue(err().contains("the module Gradient failed"));
    }
}
