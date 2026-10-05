/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.stone;

import static org.hortonmachine.gears.libs.modules.HMConstants.HYDROGEOMORPHOLOGY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

/**
 * The STONE model for the three-dimensional simulation of rockfall trajectories.
 *
 * <p>Port of the GRASS r.stone addon, itself a port of the original STONE C code by Fausto
 * Guzzetti and Massimiliano Alvioli (Copyright Fausto Guzzetti and Massimiliano Alvioli,
 * GPL v3 or later).</p>
 */
@Description(OmsStone.OMSSTONE_DESCRIPTION)
@Author(name = OmsStone.OMSSTONE_AUTHORNAMES, contact = OmsStone.OMSSTONE_AUTHORCONTACTS)
@Keywords(OmsStone.OMSSTONE_KEYWORDS)
@Label(OmsStone.OMSSTONE_LABEL)
@Name(OmsStone.OMSSTONE_NAME)
@Status(OmsStone.OMSSTONE_STATUS)
@License(OmsStone.OMSSTONE_LICENSE)
@Bibliography({OmsStone.BIBLIOGRAPHY_GUZZETTI_ET_AL_2002, OmsStone.BIBLIOGRAPHY_ALVIOLI_ET_AL_2021})
public class OmsStone extends HMModel {
    public static final String BIBLIOGRAPHY_GUZZETTI_ET_AL_2002 = "Guzzetti, F., Crosta, G., Detti, R., Agliardi, F. (2002). STONE: a computer program for the three-dimensional simulation of rock-falls. Computers & Geosciences, 28(9), 1079-1093.";
    public static final String BIBLIOGRAPHY_ALVIOLI_ET_AL_2021 = "Alvioli, M., et al. (2021). Rockfall susceptibility and network-ranked susceptibility along the Italian railway. Engineering Geology, 293, 106301.";

    @Description(OMSSTONE_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev;

    @Description(OMSSTONE_inSources_DESCRIPTION)
    @In
    public GridCoverage2D inSources;

    @Description(OMSSTONE_inNormalRestitution_DESCRIPTION)
    @In
    public GridCoverage2D inNormalRestitution;

    @Description(OMSSTONE_inTangentialRestitution_DESCRIPTION)
    @In
    public GridCoverage2D inTangentialRestitution;

    @Description(OMSSTONE_inFriction_DESCRIPTION)
    @In
    public GridCoverage2D inFriction;

    @Description(OMSSTONE_pStartVelocity_DESCRIPTION)
    @In
    public double pStartVelocity = 1.0;

    @Description(OMSSTONE_pStopVelocity_DESCRIPTION)
    @In
    public double pStopVelocity = 3.0;

    @Description(OMSSTONE_pAngleStochRange_DESCRIPTION)
    @In
    public int pAngleStochRange = 10;

    @Description(OMSSTONE_pNormalRestitutionStochRange_DESCRIPTION)
    @In
    public int pNormalRestitutionStochRange = 10;

    @Description(OMSSTONE_pTangentialRestitutionStochRange_DESCRIPTION)
    @In
    public int pTangentialRestitutionStochRange = 10;

    @Description(OMSSTONE_pFrictionStochRange_DESCRIPTION)
    @In
    public int pFrictionStochRange = 10;

    @Description(OMSSTONE_pAngleStochFunction_DESCRIPTION)
    @UI("combo:" + GAUSSIAN + "," + CAUCHY + "," + UNIFORM)
    @In
    public String pAngleStochFunction = GAUSSIAN;

    @Description(OMSSTONE_pStochFunction_DESCRIPTION)
    @UI("combo:" + GAUSSIAN + "," + UNIFORM)
    @In
    public String pStochFunction = GAUSSIAN;

    @Description(OMSSTONE_pMaxTrajectories_DESCRIPTION)
    @In
    public int pMaxTrajectories = 0;

    @Description(OMSSTONE_pStep_DESCRIPTION)
    @In
    public double pStep = 5.0;

    @Description(OMSSTONE_outCounter_DESCRIPTION)
    @Out
    public GridCoverage2D outCounter = null;

    @Description(OMSSTONE_outMaxVelocity_DESCRIPTION)
    @Out
    public GridCoverage2D outMaxVelocity = null;

    @Description(OMSSTONE_outMaxDz_DESCRIPTION)
    @Out
    public GridCoverage2D outMaxDz = null;

    @Description(OMSSTONE_outTrajectories_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outTrajectories = null;

    public static final String OMSSTONE_DESCRIPTION = "The STONE model for the three-dimensional simulation of rockfall trajectories.";
    public static final String OMSSTONE_DOCUMENTATION = "";
    public static final String OMSSTONE_KEYWORDS = "Rockfall, Stone, Hazard, Trajectories";
    public static final String OMSSTONE_LABEL = HYDROGEOMORPHOLOGY;
    public static final String OMSSTONE_NAME = "stone";
    public static final int OMSSTONE_STATUS = Status.EXPERIMENTAL;
    public static final String OMSSTONE_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSSTONE_AUTHORNAMES = "Fausto Guzzetti, Massimiliano Alvioli, Andrea Antonello";
    public static final String OMSSTONE_AUTHORCONTACTS = "https://g-ant.eu";
    public static final String OMSSTONE_inElev_DESCRIPTION = "The map of the digital elevation model (DEM).";
    public static final String OMSSTONE_inSources_DESCRIPTION = "The map of sources and stop areas: positive integer values are the number of trajectories simulated from the cell, -1 marks areas where boulders stop (e.g. lakes).";
    public static final String OMSSTONE_inNormalRestitution_DESCRIPTION = "The map of the normal restitution coefficient used at impact points, as integer percentage, from 0 (total energy damping) to 100 (elastic restitution).";
    public static final String OMSSTONE_inTangentialRestitution_DESCRIPTION = "The map of the tangential restitution coefficient used at impact points, as integer percentage, from 0 (total energy damping) to 100 (elastic restitution).";
    public static final String OMSSTONE_inFriction_DESCRIPTION = "The map of the rolling friction coefficient tan(beta) (e.g. 0.85 for alluvial deposits, 0.30 for bedrock).";
    public static final String OMSSTONE_pStartVelocity_DESCRIPTION = "The start velocity of the boulders [m/s].";
    public static final String OMSSTONE_pStopVelocity_DESCRIPTION = "The velocity under which a boulder stops [m/s].";
    public static final String OMSSTONE_pAngleStochRange_DESCRIPTION = "The variability of the start angle around the direction of steepest descent, as percentage of 45 degrees.";
    public static final String OMSSTONE_pNormalRestitutionStochRange_DESCRIPTION = "The variability of the normal restitution around the cell value, in percentage points.";
    public static final String OMSSTONE_pTangentialRestitutionStochRange_DESCRIPTION = "The variability of the tangential restitution around the cell value, in percentage points.";
    public static final String OMSSTONE_pFrictionStochRange_DESCRIPTION = "The variability of the friction coefficient around the cell value, in hundredths.";
    public static final String OMSSTONE_pAngleStochFunction_DESCRIPTION = "The distribution of the start angle around the direction of steepest descent.";
    public static final String OMSSTONE_pStochFunction_DESCRIPTION = "The distribution of the restitution and friction coefficients around the cell values.";

    public static final String GAUSSIAN = "Gaussian";
    public static final String CAUCHY = "Cauchy";
    public static final String UNIFORM = "Uniform";
    public static final String OMSSTONE_outCounter_DESCRIPTION = "The map of the number of trajectories passing through each cell.";
    public static final String OMSSTONE_outMaxVelocity_DESCRIPTION = "The map of the maximum velocity of the boulders in each cell [m/s].";
    public static final String OMSSTONE_outMaxDz_DESCRIPTION = "The map of the maximum height of the trajectories over the ground in each cell [m].";
    public static final String OMSSTONE_pStep_DESCRIPTION = "The spatial step of the trajectories [m]: boulders are moved and recorded about every step. It is limited to the cell size. The results depend on it (finer steps give longer runouts), the default of 5 m is the one of the original model.";
    public static final String OMSSTONE_pMaxTrajectories_DESCRIPTION = "The number of boulders, about, whose trajectories are stored, sampled over all sources (0 for none).";
    public static final String OMSSTONE_outTrajectories_DESCRIPTION = "The 3D lines of the sampled trajectories, with the source cell, the maximum velocity [m/s] and the maximum height over the ground [m].";

    private List<StoneTrajectory> trajectories = new ArrayList<>();

    /**
     * @return the sampled trajectories with velocities and heights at each point, in the
     *          coordinates of the input maps (empty if {@link #pMaxTrajectories} is 0).
     */
    public List<StoneTrajectory> getTrajectories() {
        return trajectories;
    }

    @Execute
    public void process() throws Exception {
        if (!concatOr(outCounter == null, doReset)) {
            return;
        }
        checkNull(inElev, inSources, inNormalRestitution, inTangentialRestitution, inFriction);

        try (HMRaster elev = HMRaster.fromGridCoverage(inElev);
                HMRaster sources = HMRaster.fromGridCoverage(inSources);
                HMRaster nrest = HMRaster.fromGridCoverage(inNormalRestitution);
                HMRaster trest = HMRaster.fromGridCoverage(inTangentialRestitution);
                HMRaster friction = HMRaster.fromGridCoverage(inFriction)) {
            int cols = elev.getCols();
            int rows = elev.getRows();
            for( HMRaster raster : List.of(sources, nrest, trest, friction) ) {
                if (raster.getCols() != cols || raster.getRows() != rows) {
                    throw new ModelsIllegalargumentException("All input maps need to have the grid of the elevation map.",
                            this, pm);
                }
            }
            double cellSize = elev.getYRes();
            if (!NumericsUtilities.dEq(cellSize, elev.getXRes())) {
                pm.errorMessage("The resolution in x (" + elev.getXRes() + ") is different from the one in y (" + cellSize
                        + "), using the one in y.");
            }

            int angleDistribution = List.of(GAUSSIAN, CAUCHY, UNIFORM).indexOf(pAngleStochFunction);
            if (angleDistribution < 0) {
                throw new ModelsIllegalargumentException("Unknown distribution for the start angle: " + pAngleStochFunction,
                        this, pm);
            }
            int coefficientsDistribution = List.of(GAUSSIAN, UNIFORM).indexOf(pStochFunction);
            if (coefficientsDistribution < 0) {
                throw new ModelsIllegalargumentException("Unknown distribution for the coefficients: " + pStochFunction, this,
                        pm);
            }

            StoneSimulation simulation = new StoneSimulation(rows, cols, cellSize, pStartVelocity, pStopVelocity,
                    pAngleStochRange, pNormalRestitutionStochRange, pTangentialRestitutionStochRange, pFrictionStochRange,
                    angleDistribution, coefficientsDistribution, getStep(cellSize));
            double[] sourcesValues = readPadded(sources, 0);
            int[] sourcesCounts = new int[sourcesValues.length];
            for( int i = 0; i < sourcesValues.length; i++ ) {
                sourcesCounts[i] = (int) sourcesValues[i];
            }
            simulation.setInputs(//
                    readPadded(elev, Double.NaN), //
                    sourcesCounts, //
                    readPadded(friction, Double.NaN), //
                    readPadded(nrest, 0), //
                    readPadded(trest, 0));

            simulation.setMaxTrajectories(Math.max(0, pMaxTrajectories));
            simulation.run(pm);
            if (pm.isCanceled()) {
                return;
            }

            outCounter = writeCounter(elev, simulation.getCounter());
            outMaxVelocity = writeMaxima(elev, "maxvel", simulation.getMaxVelocity());
            outMaxDz = writeMaxima(elev, "maxdz", simulation.getMaxHeight());

            // the local coordinates of the simulation have origin in the south-west corner
            RegionMap region = elev.getRegionMap();
            trajectories = simulation.getTrajectories();
            for( StoneTrajectory trajectory : trajectories ) {
                trajectory.translate(region.getWest(), region.getSouth());
            }
            if (pMaxTrajectories > 0) {
                outTrajectories = buildTrajectoriesCollection(trajectories, elev.getCrs());
            }
        }
    }

    /**
     * Read a raster into a row major array with an external border of novalues.
     *
     * @param nodata the value used for novalues and the border.
     */
    private static double[] readPadded( HMRaster raster, double nodata ) {
        int cols = raster.getCols();
        int rows = raster.getRows();
        int paddedCols = cols + 2;
        double[] values = new double[(rows + 2) * paddedCols];
        Arrays.fill(values, nodata);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double value = raster.getValue(c, r);
                if (!raster.isNovalue(value)) {
                    values[(r + 1) * paddedCols + c + 1] = value;
                }
            }
        }
        return values;
    }

    /**
     * Write the padded counter to an integer coverage, -1 becomes novalue.
     */
    /**
     * @return the spatial step, limited to the cell size.
     */
    private double getStep( double cellSize ) {
        if (!(pStep > 0)) {
            throw new ModelsIllegalargumentException("The step has to be positive.", this, pm);
        }
        if (pStep > cellSize) {
            pm.message("The step " + pStep + " is larger than the cell size, using the cell size " + cellSize + ".");
            return cellSize;
        }
        return pStep;
    }

    /**
     * Build the 3D lines of the trajectories.
     */
    private static SimpleFeatureCollection buildTrajectoriesCollection( List<StoneTrajectory> trajectories,
            CoordinateReferenceSystem crs ) {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("trajectories");
        typeBuilder.setCRS(crs);
        typeBuilder.add("the_geom", LineString.class);
        typeBuilder.add("srcrow", Integer.class);
        typeBuilder.add("srccol", Integer.class);
        typeBuilder.add("boulder", Integer.class);
        typeBuilder.add("maxvel", Double.class);
        typeBuilder.add("maxdz", Double.class);
        SimpleFeatureType type = typeBuilder.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        DefaultFeatureCollection collection = new DefaultFeatureCollection();
        for( StoneTrajectory trajectory : trajectories ) {
            Coordinate[] coordinates = new Coordinate[trajectory.size()];
            for( int i = 0; i < coordinates.length; i++ ) {
                coordinates[i] = new Coordinate(trajectory.getX(i), trajectory.getY(i), trajectory.getZ(i));
            }
            LineString line = GeometryUtilities.gf().createLineString(coordinates);
            builder.addAll(new Object[]{line, trajectory.getSourceRow(), trajectory.getSourceCol(), trajectory.getBoulder(),
                    trajectory.getMaxSpeed(), trajectory.getMaxHeight()});
            collection.add(builder.buildFeature(null));
        }
        return collection;
    }

    private static GridCoverage2D writeCounter( HMRaster template, long[] counter ) throws Exception {
        int cols = template.getCols();
        int rows = template.getRows();
        int icols = cols + 2;
        try (HMRaster out = new HMRaster.HMRasterWritableBuilder().setName("counter").setTemplate(template)
                .setDoInteger(true).setNoValue(HMConstants.intNovalue).build()) {
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    long value = counter[(r + 1) * icols + c + 1];
                    if (value != -1) {
                        out.setValue(c, r, (int) value);
                    }
                }
            }
            return out.buildCoverage();
        }
    }

    /**
     * Write padded maxima to a double coverage, NaN becomes novalue.
     */
    private static GridCoverage2D writeMaxima( HMRaster template, String name, double[] maxima ) throws Exception {
        int cols = template.getCols();
        int rows = template.getRows();
        int icols = cols + 2;
        try (HMRaster out = new HMRaster.HMRasterWritableBuilder().setName(name).setTemplate(template)
                .setNoValue(HMConstants.doubleNovalue).build()) {
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    double value = maxima[(r + 1) * icols + c + 1];
                    if (!Double.isNaN(value)) {
                        out.setValue(c, r, value);
                    }
                }
            }
            return out.buildCoverage();
        }
    }
}
