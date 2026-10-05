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

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.hmachine.modules.geomorphology.gradient.OmsGradient;

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
 * Prepare the input maps of {@link OmsStone} from as little as a DEM.
 *
 * <ul>
 *  <li><b>sources</b>: an existing sources map, or the cells steeper than a threshold, each
 *      throwing the given number of boulders. Optional stop areas (e.g. lakes) become stop cells.</li>
 *  <li><b>friction and restitutions</b>: from a map of lithological classes and a table of
 *      coefficients per class, or uniform values of a default class where (or if) the lithology is
 *      missing.</li>
 * </ul>
 *
 * <p>The built-in table has the 19 lithological classes used in Italy by Alvioli et al. (2021),
 * with codes 1 to 19, and code 0 for unclassified cells (values of the anthropic deposits, as in
 * the original examples):</p>
 * <ul>
 *  <li>Alvioli M. et al. (2021): Rockfall susceptibility and network-ranked susceptibility along
 *      the Italian railway. Engineering Geology, 293, 106301.
 *      https://doi.org/10.1016/j.enggeo.2021.106301</li>
 * </ul>
 */
@Description(OmsStoneInputs.DESCRIPTION)
@Author(name = OmsStoneInputs.AUTHORNAMES, contact = OmsStoneInputs.AUTHORCONTACTS)
@Keywords(OmsStoneInputs.KEYWORDS)
@Label(OmsStoneInputs.LABEL)
@Name(OmsStoneInputs.NAME)
@Status(OmsStoneInputs.STATUS)
@License(OmsStoneInputs.LICENSE)
@Bibliography({OmsStone.BIBLIOGRAPHY_ALVIOLI_ET_AL_2021})
public class OmsStoneInputs extends HMModel {
    @Description(inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev;

    @Description(inSources_DESCRIPTION)
    @In
    public GridCoverage2D inSources;

    @Description(pSourceSlope_DESCRIPTION)
    @In
    public double pSourceSlope = 45.0;

    @Description(pBouldersPerSource_DESCRIPTION)
    @In
    public int pBouldersPerSource = 1;

    @Description(inStopAreas_DESCRIPTION)
    @In
    public GridCoverage2D inStopAreas;

    @Description(inLithology_DESCRIPTION)
    @In
    public GridCoverage2D inLithology;

    @Description(inLithologyTable_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String inLithologyTable;

    @Description(pDefaultLithology_DESCRIPTION)
    @UI("combo:" + LITHOLOGY_NAMES_COMBO)
    @In
    public String pDefaultLithology = UNCLASSIFIED;

    @Description(outSources_DESCRIPTION)
    @Out
    public GridCoverage2D outSources;

    @Description(outNormalRestitution_DESCRIPTION)
    @Out
    public GridCoverage2D outNormalRestitution;

    @Description(outTangentialRestitution_DESCRIPTION)
    @Out
    public GridCoverage2D outTangentialRestitution;

    @Description(outFriction_DESCRIPTION)
    @Out
    public GridCoverage2D outFriction;

    public static final String DESCRIPTION = "Prepare the input maps of the STONE rockfall model from as little as a DEM: sources from the slope, friction and restitutions from a lithological map or default values.";
    public static final String KEYWORDS = "Rockfall, Stone, Hazard, Lithology, Sources";
    public static final String LABEL = HYDROGEOMORPHOLOGY;
    public static final String NAME = "stoneinputs";
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String AUTHORNAMES = "Andrea Antonello";
    public static final String AUTHORCONTACTS = "https://g-ant.eu";
    public static final String inElev_DESCRIPTION = "The map of the digital elevation model (DEM), defining the grid of all the maps.";
    public static final String inSources_DESCRIPTION = "An optional existing map of sources (number of boulders per cell, -1 for stop cells). If missing, the sources are the cells steeper than pSourceSlope.";
    public static final String pSourceSlope_DESCRIPTION = "The slope over which a cell is a source, when no sources map is given [degrees].";
    public static final String pBouldersPerSource_DESCRIPTION = "The number of boulders thrown from each source cell, when no sources map is given.";
    public static final String inStopAreas_DESCRIPTION = "An optional map of areas where boulders stop (e.g. lakes): its valid cells become stop cells (-1) in the sources.";
    public static final String inLithology_DESCRIPTION = "An optional map of lithological class codes, see inLithologyTable. Where (or if) it is missing, the default class is used.";
    public static final String inLithologyTable_DESCRIPTION = "An optional CSV table of the coefficients per lithological class, with lines: code, friction, normal restitution [%], tangential restitution [%]. If missing, the 19 classes of Alvioli et al. (2021) are used, with codes 1 to 19 and 0 for unclassified.";
    public static final String pDefaultLithology_DESCRIPTION = "The class of the built-in table whose coefficients are used where the lithology is missing or its code is not in the table.";
    public static final String outSources_DESCRIPTION = "The map of the sources: number of boulders per cell, -1 for stop cells.";
    public static final String outNormalRestitution_DESCRIPTION = "The map of the normal restitution [%].";
    public static final String outTangentialRestitution_DESCRIPTION = "The map of the tangential restitution [%].";
    public static final String outFriction_DESCRIPTION = "The map of the rolling friction coefficient.";

    /*
     * The built-in classes: code, name, friction, normal and tangential restitution [%].
     */
    public static final String UNCLASSIFIED = "Unclassified";
    private static final Object[][] LITHOLOGY_TABLE = {//
            {0, UNCLASSIFIED, 0.65, 35, 55}, //
            {1, "Anthropic deposits", 0.65, 35, 55}, //
            {2, "Alluvial lacustrine marine eluvial and colluvial deposits", 0.80, 15, 40}, //
            {3, "Coastal deposits", 0.65, 35, 55}, //
            {4, "Landslides", 0.65, 35, 55}, //
            {5, "Glacial deposits", 0.65, 35, 55}, //
            {6, "Loosely packed clastic deposits", 0.35, 45, 55}, //
            {7, "Consolidated clastic deposits", 0.40, 55, 65}, //
            {8, "Marl", 0.40, 55, 65}, //
            {9, "Carbonates-siliciclastic and marl sequence", 0.35, 60, 70}, //
            {10, "Chaotic rocks and melange", 0.35, 45, 55}, //
            {11, "Flysch", 0.40, 55, 65}, //
            {12, "Carbonate rocks", 0.30, 65, 75}, //
            {13, "Evaporites", 0.35, 45, 55}, //
            {14, "Pyroclastic rocks and ignimbrites", 0.40, 55, 65}, //
            {15, "Lava and basalts", 0.30, 65, 75}, //
            {16, "Intrusive igneous rocks", 0.30, 65, 75}, //
            {17, "Schists", 0.35, 60, 70}, //
            {18, "Non-schists", 0.30, 65, 75}, //
            {19, "Lakes and glaciers", 0.95, 10, 10}};
    /** The names of the built-in classes, for the combo (no commas in the names). */
    public static final String LITHOLOGY_NAMES_COMBO = UNCLASSIFIED + ",Anthropic deposits,"
            + "Alluvial lacustrine marine eluvial and colluvial deposits,Coastal deposits,Landslides,Glacial deposits,"
            + "Loosely packed clastic deposits,Consolidated clastic deposits,Marl,Carbonates-siliciclastic and marl sequence,"
            + "Chaotic rocks and melange,Flysch,Carbonate rocks,Evaporites,Pyroclastic rocks and ignimbrites,"
            + "Lava and basalts,Intrusive igneous rocks,Schists,Non-schists,Lakes and glaciers";

    /**
     * The coefficients of a lithological class.
     */
    private static class Coefficients {
        final double friction;
        final int normalRestitution;
        final int tangentialRestitution;

        Coefficients( double friction, int normalRestitution, int tangentialRestitution ) {
            this.friction = friction;
            this.normalRestitution = normalRestitution;
            this.tangentialRestitution = tangentialRestitution;
        }
    }

    @Execute
    public void process() throws Exception {
        checkNull(inElev);
        if (inSources == null && pBouldersPerSource < 1) {
            throw new ModelsIllegalargumentException("The number of boulders per source has to be at least 1.", this, pm);
        }
        Map<Integer, Coefficients> table = inLithologyTable != null && !inLithologyTable.trim().isEmpty()
                ? readTable(new File(inLithologyTable.trim()))
                : builtInTable();
        Coefficients defaultCoefficients = builtInClass(pDefaultLithology);

        try (HMRaster elev = HMRaster.fromGridCoverage(inElev)) {
            int cols = elev.getCols();
            int rows = elev.getRows();

            GridCoverage2D slope = null;
            if (inSources == null) {
                OmsGradient gradient = new OmsGradient();
                gradient.inElev = inElev;
                gradient.pMode = Variables.HORN;
                gradient.doDegrees = true;
                gradient.pm = pm;
                gradient.process();
                slope = gradient.outSlope;
            }

            try (HMRaster sourcesIn = open(inSources, elev, "sources");
                    HMRaster slopeRaster = slope != null ? HMRaster.fromGridCoverage(slope) : null;
                    HMRaster stopAreas = open(inStopAreas, elev, "stop areas");
                    HMRaster lithology = open(inLithology, elev, "lithology");
                    HMRaster sourcesOut = intRaster(elev, "sources");
                    HMRaster normalOut = intRaster(elev, "nrest");
                    HMRaster tangentialOut = intRaster(elev, "trest");
                    HMRaster frictionOut = new HMRaster.HMRasterWritableBuilder().setName("friction").setTemplate(elev)
                            .setNoValue(HMConstants.doubleNovalue).build()) {
                pm.beginTask("Preparing the STONE inputs...", rows);
                for( int r = 0; r < rows; r++ ) {
                    for( int c = 0; c < cols; c++ ) {
                        if (elev.isNovalue(elev.getValue(c, r))) {
                            continue;
                        }

                        int boulders = 0;
                        if (sourcesIn != null) {
                            double value = sourcesIn.getValue(c, r);
                            if (!sourcesIn.isNovalue(value)) {
                                boulders = (int) value;
                            }
                        } else {
                            double value = slopeRaster.getValue(c, r);
                            if (!slopeRaster.isNovalue(value) && value > pSourceSlope) {
                                boulders = pBouldersPerSource;
                            }
                        }
                        if (stopAreas != null && !stopAreas.isNovalue(stopAreas.getValue(c, r))) {
                            boulders = -1;
                        }
                        if (boulders != 0) {
                            sourcesOut.setValue(c, r, boulders);
                        }

                        Coefficients coefficients = defaultCoefficients;
                        if (lithology != null) {
                            double value = lithology.getValue(c, r);
                            if (!lithology.isNovalue(value)) {
                                coefficients = table.getOrDefault((int) Math.round(value), defaultCoefficients);
                            }
                        }
                        normalOut.setValue(c, r, coefficients.normalRestitution);
                        tangentialOut.setValue(c, r, coefficients.tangentialRestitution);
                        frictionOut.setValue(c, r, coefficients.friction);
                    }
                    pm.worked(1);
                }
                pm.done();
                outSources = sourcesOut.buildCoverage();
                outNormalRestitution = normalOut.buildCoverage();
                outTangentialRestitution = tangentialOut.buildCoverage();
                outFriction = frictionOut.buildCoverage();
            }
        }
    }

    /**
     * Open an optional map, checking that it has the grid of the DEM.
     */
    private HMRaster open( GridCoverage2D coverage, HMRaster elev, String name ) {
        if (coverage == null) {
            return null;
        }
        HMRaster raster = HMRaster.fromGridCoverage(coverage);
        if (raster.getCols() != elev.getCols() || raster.getRows() != elev.getRows()) {
            throw new ModelsIllegalargumentException("The " + name + " map needs to have the grid of the DEM.", this, pm);
        }
        return raster;
    }

    private static HMRaster intRaster( HMRaster template, String name ) {
        return new HMRaster.HMRasterWritableBuilder().setName(name).setTemplate(template).setDoInteger(true)
                .setNoValue(HMConstants.intNovalue).build();
    }

    private static Map<Integer, Coefficients> builtInTable() {
        Map<Integer, Coefficients> table = new HashMap<>();
        for( Object[] row : LITHOLOGY_TABLE ) {
            table.put((Integer) row[0], new Coefficients((Double) row[2], (Integer) row[3], (Integer) row[4]));
        }
        return table;
    }

    private Coefficients builtInClass( String name ) {
        for( Object[] row : LITHOLOGY_TABLE ) {
            if (row[1].equals(name)) {
                return new Coefficients((Double) row[2], (Integer) row[3], (Integer) row[4]);
            }
        }
        throw new ModelsIllegalargumentException("Unknown lithological class: " + name, this, pm);
    }

    /**
     * Read a table of lines: code, friction, normal restitution, tangential restitution. Empty
     * lines, lines starting with # and a header line are skipped.
     */
    private Map<Integer, Coefficients> readTable( File file ) throws Exception {
        Map<Integer, Coefficients> table = new HashMap<>();
        List<String> lines = Files.readAllLines(file.toPath());
        for( int i = 0; i < lines.size(); i++ ) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("[,;]");
            if (parts.length < 4) {
                throw new ModelsIllegalargumentException(
                        "Line " + (i + 1) + " of the lithology table doesn't have code, friction, normal and tangential restitution: "
                                + line,
                        this, pm);
            }
            try {
                int code = Integer.parseInt(parts[0].trim());
                table.put(code, new Coefficients(Double.parseDouble(parts[1].trim()),
                        (int) Math.round(Double.parseDouble(parts[2].trim())),
                        (int) Math.round(Double.parseDouble(parts[3].trim()))));
            } catch (NumberFormatException e) {
                if (table.isEmpty()) {
                    // a header
                    continue;
                }
                throw new ModelsIllegalargumentException("Line " + (i + 1) + " of the lithology table is not numeric: " + line,
                        this, pm);
            }
        }
        return table;
    }
}
