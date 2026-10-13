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
package org.hortonmachine.hmachine.models.hm;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration;
import org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject;
import org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProjectFilesGenerator;
import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.CalibrationTimeParameterCodes;
import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.ProjectNeededParameterCodes;
import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.ProjectTimeParameterCodes;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPDatabase;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPFeatureType;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.joda.time.DateTime;
import org.locationtech.jts.geom.Geometry;

/**
 * Test for the {@link OmsTrentoPProject}, {@link OmsTrentoPCalibration} and
 * {@link OmsTrentoPProjectFilesGenerator} modules, on a small storm sewer network of 25 pipes
 * draining 2 ha, with its outlet in pipe 31, kept in two GeoPackages.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestTrentoP extends HMTestCase {

    private static final int OUTLET = 31;
    private static final double DELTA = 0.0000001;

    private File projectDb;
    private File calibrationDb;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        projectDb = copyResource("sewer_project.gpkg");
        calibrationDb = copyResource("sewer_calibration.gpkg");
    }

    @Override
    protected void tearDown() throws Exception {
        projectDb.delete();
        calibrationDb.delete();
        super.tearDown();
    }

    public void testProject() throws Exception {
        OmsTrentoPProject project = runProject(null, null);

        Map<Integer, SimpleFeature> pipes = byId(project.outPipes);
        assertEquals(25, pipes.size());

        // the commercial diameters [cm], never smaller downstream
        Map<Integer, Double> expectedDiameters = Map.ofEntries(Map.entry(1, 20.0), Map.entry(2, 14.0), Map.entry(3, 20.0),
                Map.entry(4, 14.0), Map.entry(5, 20.0), Map.entry(6, 14.0), Map.entry(7, 14.0), Map.entry(8, 14.0),
                Map.entry(9, 14.0), Map.entry(10, 14.0), Map.entry(11, 14.0), Map.entry(13, 14.0), Map.entry(14, 14.0),
                Map.entry(15, 14.0), Map.entry(19, 14.0), Map.entry(20, 14.0), Map.entry(21, 20.0), Map.entry(23, 14.0),
                Map.entry(24, 14.0), Map.entry(25, 14.0), Map.entry(26, 20.0), Map.entry(27, 31.5), Map.entry(28, 20.0),
                Map.entry(29, 40.0), Map.entry(31, 40.0));
        for( Map.Entry<Integer, Double> entry : expectedDiameters.entrySet() ) {
            assertEquals("pipe " + entry.getKey(), entry.getValue(), value(pipes.get(entry.getKey()), TrentoPFeatureType.DIAMETER_STR),
                    DELTA);
        }

        // the topology comes from the geometries, starting at the outlet
        assertEquals(0, intValue(pipes.get(OUTLET), TrentoPFeatureType.ID_PIPE_WHERE_DRAIN_STR));
        assertEquals(OUTLET, intValue(pipes.get(29), TrentoPFeatureType.ID_PIPE_WHERE_DRAIN_STR));
        assertEquals(29, intValue(pipes.get(27), TrentoPFeatureType.ID_PIPE_WHERE_DRAIN_STR));
        assertEquals(3, intValue(pipes.get(1), TrentoPFeatureType.ID_PIPE_WHERE_DRAIN_STR));

        // at the outlet: the whole area [ha] and the design discharge [l/s]
        assertEquals(2.006065, value(pipes.get(OUTLET), TrentoPFeatureType.TOTAL_SUB_NET_AREA_STR), 0.000001);
        assertEquals(285.40, value(pipes.get(OUTLET), TrentoPFeatureType.DISCHARGE_STR), 0.01);
    }

    public void testProjectDoesNotDependOnTheOrderOfThePipes() throws Exception {
        OmsTrentoPProject project = runProject(null, null);

        List<SimpleFeature> pipesList = FeatureUtilities.featureCollectionToList(readLayer(projectDb, Constants.NETWORK_PROJECT_LAYER));
        java.util.Collections.reverse(pipesList);
        DefaultFeatureCollection reversedPipes = new DefaultFeatureCollection();
        reversedPipes.addAll(pipesList);
        OmsTrentoPProject reversedProject = runProject(reversedPipes, null);

        Map<Integer, SimpleFeature> pipes = byId(project.outPipes);
        Map<Integer, SimpleFeature> reversed = byId(reversedProject.outPipes);
        for( Integer id : pipes.keySet() ) {
            assertEquals("pipe " + id, value(pipes.get(id), TrentoPFeatureType.DIAMETER_STR),
                    value(reversed.get(id), TrentoPFeatureType.DIAMETER_STR), DELTA);
            assertEquals("pipe " + id, value(pipes.get(id), TrentoPFeatureType.DISCHARGE_STR),
                    value(reversed.get(id), TrentoPFeatureType.DISCHARGE_STR), 0.001);
            // and a pipe is never smaller than the ones that drain into it
            int drain = intValue(pipes.get(id), TrentoPFeatureType.ID_PIPE_WHERE_DRAIN_STR);
            if (drain != 0) {
                assertTrue("pipe " + id, value(pipes.get(drain), TrentoPFeatureType.DIAMETER_STR) >= value(pipes.get(id),
                        TrentoPFeatureType.DIAMETER_STR));
            }
        }
    }

    public void testProjectWithSlopesBelowOnePercent() throws Exception {
        OmsTrentoPProject project = runProject(null, null);

        // the same network, with all the areas at 0.5 %
        SimpleFeatureCollection pipes = readLayer(projectDb, Constants.NETWORK_PROJECT_LAYER);
        DefaultFeatureCollection flatPipes = new DefaultFeatureCollection();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(pipes.getSchema());
        for( SimpleFeature pipe : FeatureUtilities.featureCollectionToList(pipes) ) {
            builder.init(pipe);
            builder.set(TrentoPFeatureType.AVERAGE_SLOPE_STR, 0.5);
            flatPipes.add(builder.buildFeature(pipe.getID()));
        }
        OmsTrentoPProject flatProject = runProject(flatPipes, null);

        // the residence time of the areas grows with s^-gamma, gamma = 0.2
        double expectedRatio = Math.pow(0.5, -0.2);
        Map<Integer, SimpleFeature> steep = byId(project.outPipes);
        Map<Integer, SimpleFeature> flat = byId(flatProject.outPipes);
        for( Integer id : steep.keySet() ) {
            double steepTime = value(steep.get(id), TrentoPFeatureType.RESIDENCE_TIME_STR);
            double flatTime = value(flat.get(id), TrentoPFeatureType.RESIDENCE_TIME_STR);
            assertTrue("pipe " + id, Double.isFinite(flatTime));
            assertEquals("pipe " + id, expectedRatio, flatTime / steepTime, 0.0001);
        }
    }

    public void testProjectNeedsItsParameters() throws Exception {
        HashMap<String, Number> parameters = readParameters(projectDb);
        parameters.remove(ProjectNeededParameterCodes.TAU.getKey());
        try {
            runProject(null, parameters);
            fail("A needed parameter without value has to be refused.");
        } catch (ModelsIllegalargumentException e) {
            assertTrue(e.getMessage().contains(ProjectNeededParameterCodes.TAU.getKey()));
        }
    }

    public void testCalibrationWithRainfallCurve() throws Exception {
        // the time step, the duration and the maximum rain duration come from the parameters
        OmsTrentoPCalibration calibration = calibration(readParameters(calibrationDb));
        calibration.process();

        // the rain duration that gives the highest discharge, searched from 5 up to 60 minutes: the network is
        // small and quick, so the shortest rain, the most intense, wins
        assertEquals(5, calibration.outTpMax.intValue());

        Map<Integer, SimpleFeature> pipes = byId(calibration.outPipes);
        assertEquals(25, pipes.size());
        assertEquals(227.044, value(pipes.get(OUTLET), OmsTrentoPCalibration.QMAX_FIELD), 0.001);
        assertEquals(0.513498, value(pipes.get(1), OmsTrentoPCalibration.FILLMAX_FIELD), 0.000001);
        // the 24 time steps of 5 minutes of the 120 minutes of simulation
        assertEquals(24, calibration.outDischarge.size());
        for( SimpleFeature pipe : pipes.values() ) {
            assertTrue(value(pipe, OmsTrentoPCalibration.QMAX_FIELD) > 0);
        }
    }

    public void testCalibrationWithShortMaximumRain() throws Exception {
        HashMap<String, Number> parameters = readParameters(calibrationDb);
        parameters.put(ProjectTimeParameterCodes.MINIMUM_TIME.getKey(), 15);
        OmsTrentoPCalibration calibration = calibration(parameters);
        // shorter than the 15 minutes the search starts from: only it is used
        calibration.tpMaxCalibration = 10;
        calibration.process();

        assertEquals(10, calibration.outTpMax.intValue());
        Map<Integer, SimpleFeature> pipes = byId(calibration.outPipes);
        assertEquals(195.9, value(pipes.get(OUTLET), OmsTrentoPCalibration.QMAX_FIELD), 0.1);
    }

    public void testCalibrationRainHasTheDepthOfTheRainfallCurve() throws Exception {
        HashMap<String, Number> parameters = readParameters(calibrationDb);
        // a single rain of 30 minutes
        int duration = 30;
        parameters.put(ProjectTimeParameterCodes.MINIMUM_TIME.getKey(), duration);
        OmsTrentoPCalibration calibration = calibration(parameters);
        calibration.tpMaxCalibration = duration;
        calibration.process();
        assertEquals(duration, calibration.outTpMax.intValue());

        // the volume out of the outlet [m3], from the discharges [l/s] every 5 minutes
        double volume = 0;
        for( HashMap<Integer, double[]> discharges : calibration.outDischarge.values() ) {
            volume += discharges.get(OUTLET)[0] * 5 * 60 / 1000.0;
        }

        // the depth of the rainfall curve [mm] on the effective area [ha]: 1 mm on 1 ha is 10 m3
        double a = parameters.get(ProjectNeededParameterCodes.A.getKey()).doubleValue();
        double n = parameters.get(ProjectNeededParameterCodes.N.getKey()).doubleValue();
        double depth = a * Math.pow(duration / 60.0, n);
        double rainVolume = depth * effectiveArea(calibration) * 10;
        assertEquals(1.0, volume / rainVolume, 0.02);
    }

    public void testCalibrationWithRainSeries() throws Exception {
        HashMap<String, Number> parameters = readParameters(calibrationDb);
        parameters.remove(ProjectNeededParameterCodes.A.getKey());
        parameters.remove(ProjectNeededParameterCodes.N.getKey());
        // without a maximum rain duration the whole rain is used
        parameters.remove(CalibrationTimeParameterCodes.MAXIMUM_RAIN_TIME.getKey());

        // 0.5 mm/min for 30 minutes, every 5 minutes, then no rain until the end of the simulation
        double intensity = 0.5;
        LinkedHashMap<DateTime, double[]> rain = new LinkedHashMap<>();
        DateTime time = new DateTime(2020, 6, 1, 12, 0);
        for( int i = 0; i < 24; i++ ) {
            rain.put(time, new double[]{i < 6 ? intensity : 0.0});
            time = time.plusMinutes(5);
        }

        OmsTrentoPCalibration calibration = calibration(parameters);
        calibration.inRain = rain;
        calibration.process();

        // the outlet can't get more than the effective rain on the whole network [l/s]
        Map<Integer, SimpleFeature> pipes = byId(calibration.outPipes);
        double maxDischarge = intensity * effectiveArea(calibration) * Constants.HAOVERH_TO_METEROVERS;
        double outletDischarge = value(pipes.get(OUTLET), OmsTrentoPCalibration.QMAX_FIELD);
        assertTrue(outletDischarge > 0.5 * maxDischarge);
        assertTrue(outletDischarge <= maxDischarge);
    }

    public void testCalibrationNeedsBothParametersOfTheCurve() throws Exception {
        HashMap<String, Number> parameters = readParameters(calibrationDb);
        parameters.remove(ProjectNeededParameterCodes.N.getKey());
        try {
            calibration(parameters).process();
            fail("The rainfall curve needs both its parameters.");
        } catch (ModelsIllegalargumentException e) {
            assertTrue(e.getMessage().contains(ProjectNeededParameterCodes.N.getKey()));
        }
    }

    public void testFilesGenerator() throws Exception {
        File db = File.createTempFile("trentop_generated", ".gpkg");
        db.delete();
        try {
            // a project and a calibration in the same GeoPackage
            for( String mode : new String[]{Variables.PROJECT, Variables.CALIBRATION} ) {
                OmsTrentoPProjectFilesGenerator generator = new OmsTrentoPProjectFilesGenerator();
                generator.pm = pm;
                generator.outDatabase = db.getAbsolutePath();
                generator.pMode = mode;
                generator.pCode = "EPSG:32632";
                generator.process();
            }

            try (ASpatialDb sdb = TrentoPDatabase.open(db.getAbsolutePath(), null, null)) {
                for( String table : new String[]{Constants.NETWORK_PROJECT_LAYER, Constants.NETWORK_CALIBRATION_LAYER,
                        Constants.AREAS_LAYER, Constants.JUNCTIONS_LAYER, Constants.PARAMETERS_TABLE, Constants.DIAMETERS_TABLE} ) {
                    assertTrue(table, sdb.hasTable(table));
                }
                List<String> junctionColumns = sdb.getTableColumns(SqlName.m(Constants.JUNCTIONS_LAYER)).stream().map(c -> c[0])
                        .toList();
                assertTrue(junctionColumns.toString(), junctionColumns.contains(TrentoPFeatureType.ELEVATION_STR));
                assertEquals(0, TrentoPDatabase.readLayer(sdb, Constants.JUNCTIONS_LAYER).size());

                // the optional parameters have their default, the needed ones no value
                HashMap<String, Number> parameters = TrentoPDatabase.readParameters(sdb);
                assertFalse(parameters.containsKey(ProjectNeededParameterCodes.TAU.getKey()));
                assertEquals(0.25, parameters.get(ProjectTimeParameterCodes.STEP.getKey()).doubleValue(), DELTA);
                // the calibration added its own parameters to the ones of the project
                assertEquals(15.0, parameters.get(CalibrationTimeParameterCodes.STEP.getKey()).doubleValue(), DELTA);

                List<double[]> diameters = TrentoPDatabase.readDiameters(sdb);
                assertEquals(TrentoPDatabase.EXAMPLE_DIAMETERS.length, diameters.size());
                assertEquals(14.0, diameters.get(0)[0], DELTA);
            }
        } finally {
            db.delete();
        }
    }

    public void testOutputLayerIsReplaced() throws Exception {
        OmsTrentoPProject project = runProject(null, null);
        try (ASpatialDb db = TrentoPDatabase.open(projectDb.getAbsolutePath(), null, null)) {
            TrentoPDatabase.writeLayer(db, project.outPipes, Constants.NETWORK_PROJECT_OUTPUT_LAYER, pm);
            TrentoPDatabase.writeLayer(db, project.outPipes, Constants.NETWORK_PROJECT_OUTPUT_LAYER, pm);
            assertEquals(25, TrentoPDatabase.readLayer(db, Constants.NETWORK_PROJECT_OUTPUT_LAYER).size());
        }
    }

    /**
     * Run the project on the sample, with other pipes or parameters, if given.
     */
    private OmsTrentoPProject runProject( SimpleFeatureCollection pipes, HashMap<String, Number> parameters ) throws Exception {
        OmsTrentoPProject project = new OmsTrentoPProject();
        project.pm = pm;
        project.pOutPipe = OUTLET;
        try (ASpatialDb db = TrentoPDatabase.open(projectDb.getAbsolutePath(), null, null)) {
            project.inParameters = parameters != null ? parameters : TrentoPDatabase.readParameters(db);
            project.inDiameters = TrentoPDatabase.readDiameters(db);
            project.inPipes = pipes != null ? pipes : TrentoPDatabase.readLayer(db, Constants.NETWORK_PROJECT_LAYER);
            project.inAreas = TrentoPDatabase.readLayer(db, Constants.AREAS_LAYER);
            project.inJunctions = TrentoPDatabase.readLayer(db, Constants.JUNCTIONS_LAYER);
        }
        project.process();
        return project;
    }

    /**
     * @return the calibration of the sample, with the given parameters, to run.
     */
    private OmsTrentoPCalibration calibration( HashMap<String, Number> parameters ) throws Exception {
        OmsTrentoPCalibration calibration = new OmsTrentoPCalibration();
        calibration.pm = pm;
        calibration.pOutPipe = OUTLET;
        calibration.inParameters = parameters;
        calibration.inPipes = readLayer(calibrationDb, Constants.NETWORK_CALIBRATION_LAYER);
        calibration.inAreas = readLayer(calibrationDb, Constants.AREAS_LAYER);
        calibration.inJunctions = readLayer(calibrationDb, Constants.JUNCTIONS_LAYER);
        return calibration;
    }

    /**
     * @return the sum of the drained areas times their runoff coefficients, in ha.
     */
    private static double effectiveArea( OmsTrentoPCalibration calibration ) {
        Map<Integer, SimpleFeature> pipes = byId(calibration.outPipes);
        double effectiveArea = 0;
        for( SimpleFeature area : FeatureUtilities.featureCollectionToList(calibration.inAreas) ) {
            int id = intValue(area, TrentoPFeatureType.ID_STR);
            double runoff = value(pipes.get(id), TrentoPFeatureType.RUNOFF_COEFFICIENT_STR);
            effectiveArea += ((Geometry) area.getDefaultGeometry()).getArea() / 10000.0 * runoff;
        }
        return effectiveArea;
    }

    private static HashMap<String, Number> readParameters( File database ) throws Exception {
        try (ASpatialDb db = TrentoPDatabase.open(database.getAbsolutePath(), null, null)) {
            return TrentoPDatabase.readParameters(db);
        }
    }

    private static SimpleFeatureCollection readLayer( File database, String layer ) throws Exception {
        try (ASpatialDb db = TrentoPDatabase.open(database.getAbsolutePath(), null, null)) {
            return TrentoPDatabase.readLayer(db, layer);
        }
    }

    /**
     * Copy a database of the resources to a temporary file, so that the tests do not change it.
     */
    private File copyResource( String name ) throws Exception {
        File resource = new File(getClass().getClassLoader().getResource("trentop/" + name).toURI());
        File copy = File.createTempFile("trentop_", "_" + name);
        Files.copy(resource.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return copy;
    }

    private static Map<Integer, SimpleFeature> byId( SimpleFeatureCollection pipes ) {
        Map<Integer, SimpleFeature> map = new HashMap<>();
        for( SimpleFeature pipe : FeatureUtilities.featureCollectionToList(pipes) ) {
            map.put(intValue(pipe, TrentoPFeatureType.ID_STR), pipe);
        }
        return map;
    }

    private static double value( SimpleFeature feature, String field ) {
        return ((Number) feature.getAttribute(field)).doubleValue();
    }

    private static int intValue( SimpleFeature feature, String field ) {
        return ((Number) feature.getAttribute(field)).intValue();
    }
}
