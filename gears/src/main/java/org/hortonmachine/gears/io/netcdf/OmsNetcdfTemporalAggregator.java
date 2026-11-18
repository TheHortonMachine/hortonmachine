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
package org.hortonmachine.gears.io.netcdf;

import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_doLatLon;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_inPath;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_pExcludePattern;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_pFromTimestep;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_pGridName;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_pIncludePattern;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.HMRasterWritableBuilder;
import org.hortonmachine.gears.utils.RegionMap;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OmsNetcdfTemporalAggregator.DESCRIPTION)
@Documentation(OmsNetcdfTemporalAggregator.DOCUMENTATION)
@Author(name = "Andrea Antonello", contact = "https://g-ant.eu")
@Keywords("IO, NetCDF, Raster, Time series, Aggregation, Statistics")
@Label(HMConstants.NETCDF)
@Name("netcdftemporalaggregator")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class OmsNetcdfTemporalAggregator extends HMModel {
    @Description(DESCR_inPath)
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inPath = null;

    @Description(DESCR_pGridName)
    @In
    public String pGridName = null;

    @Description(DESCR_pIncludePattern)
    @In
    public String pIncludePattern = null;

    @Description(DESCR_pExcludePattern)
    @In
    public String pExcludePattern = null;

    @Description(DESCR_pFromTimestep)
    @In
    public Integer pFromTimestep = 0;

    @Description(DESCR_pToTimestep)
    @In
    public Integer pToTimestep = null;

    @Description(DESCR_pAggregation)
    @UI("combo:" + MEAN + "," + SUM + "," + MIN + "," + MAX)
    @In
    public String pAggregation = MEAN;

    @Description(DESCR_pMinValidFraction)
    @In
    public Double pMinValidFraction = 0.0;

    @Description(DESCR_doLatLon)
    @In
    public boolean doLatLon = false;

    @Description(DESCR_outRaster)
    @Out
    public GridCoverage2D outRaster = null;

    public static final String MEAN = "mean";
    public static final String SUM = "sum";
    public static final String MIN = "min";
    public static final String MAX = "max";

    public static final String DESCRIPTION = "Aggregates the timesteps of a NetCDF grid into a single raster: their mean, sum, minimum or maximum, cell by cell.";
    public static final String DESCR_pToTimestep = "The index of the timestep at which to stop, excluded. If not set, all the timesteps from the first are used.";
    public static final String DESCR_pAggregation = "The aggregation of the values of every cell: mean, sum, min or max.";
    public static final String DESCR_pMinValidFraction = "The fraction of the timesteps, from 0 to 1, that need a valid value in a cell for it to be aggregated, else the cell is novalue. With 0 a single valid value is enough.";
    public static final String DESCR_outRaster = "The aggregated raster.";

    public static final String DOCUMENTATION = """
            The timesteps are read one at a time with the conversion of NetcdfGridDumper, so the output has the same grid: projected grids of known crs keep it, the others are resampled to lat/long WGS84, as described there. The timesteps are never all in memory: every cell keeps only its running sum, minimum or maximum and its number of valid values, so a whole year of daily grids can be aggregated in the memory of two rasters.

            The novalues of a timestep, as the fill values of the file, are skipped: the mean is the one of the valid values of the cell, the sum is their sum. A cell is aggregated only if it has a valid value in at least the given fraction of the timesteps, rounded up, and at least one; else it is novalue.

            As an example, the yearly mean of a Daymet file of daily maximum temperatures, which has 365 timesteps, is the mean with all the timesteps; a minimum valid fraction of 0.9 leaves out the cells that miss more than 36 days.
            """;

    @Execute
    public void process() throws Exception {
        checkNull(inPath, pAggregation);
        if (!pAggregation.equals(MEAN) && !pAggregation.equals(SUM) && !pAggregation.equals(MIN) && !pAggregation.equals(MAX)) {
            throw new ModelsIllegalargumentException("The aggregation has to be one of: mean, sum, min, max.", this);
        }
        double minValidFraction = pMinValidFraction == null ? 0 : pMinValidFraction;
        if (minValidFraction < 0 || minValidFraction > 1) {
            throw new ModelsIllegalargumentException("The minimum valid fraction has to be between 0 and 1.", this);
        }

        OmsNetcdf2GridCoverageConverter converter = new OmsNetcdf2GridCoverageConverter();
        converter.pm = pm;
        converter.inPath = inPath;
        converter.pGridName = pGridName;
        converter.pIncludePattern = pIncludePattern;
        converter.pExcludePattern = pExcludePattern;
        converter.pFromTimestep = pFromTimestep;
        converter.pToTimestep = pToTimestep == null ? OmsNetcdf2GridCoverageConverter.ALL_TIMESTEPS : pToTimestep;
        converter.doLatLon = doLatLon;

        boolean isMean = pAggregation.equals(MEAN);
        boolean isSum = pAggregation.equals(SUM);
        boolean isMin = pAggregation.equals(MIN);

        double[] accumulated = null;
        int[] validCounts = null;
        double[] values = null;
        RegionMap region = null;
        CoordinateReferenceSystem crs = null;
        int timesteps = 0;
        try {
            converter.initProcess();
            while( converter.doProcess ) {
                if (pm.isCanceled()) {
                    return;
                }
                converter.process();
                try (HMRaster raster = HMRaster.fromGridCoverage(converter.outRaster)) {
                    int cols = raster.getCols();
                    int rows = raster.getRows();
                    if (accumulated == null) {
                        region = raster.getRegionMap();
                        crs = raster.getCrs();
                        accumulated = new double[cols * rows];
                        validCounts = new int[cols * rows];
                        values = new double[cols * rows];
                    }
                    values = raster.getValues(0, 0, cols, rows, values);
                    for( int i = 0; i < values.length; i++ ) {
                        double value = values[i];
                        if (raster.isNovalue(value)) {
                            continue;
                        }
                        if (validCounts[i] == 0) {
                            accumulated[i] = value;
                        } else if (isMean || isSum) {
                            accumulated[i] += value;
                        } else if (isMin) {
                            accumulated[i] = Math.min(accumulated[i], value);
                        } else {
                            accumulated[i] = Math.max(accumulated[i], value);
                        }
                        validCounts[i]++;
                    }
                }
                timesteps++;
            }
        } finally {
            converter.close();
        }
        if (accumulated == null || validCounts == null || region == null) {
            throw new ModelsIllegalargumentException("No timestep has been read.", this);
        }

        int minValid = Math.max(1, (int) Math.ceil(minValidFraction * timesteps - 1E-9));
        pm.message("Aggregating " + timesteps + " timesteps with " + pAggregation + ", cells need at least " + minValid
                + " valid values.");
        double novalue = HMConstants.doubleNovalue;
        int cols = region.getCols();
        int rows = region.getRows();
        HMRaster aggregated = new HMRasterWritableBuilder().setName(pAggregation).setRegion(region).setCrs(crs)
                .setNoValue(novalue).setInitialValue(novalue).build();
        for( int row = 0; row < rows; row++ ) {
            for( int col = 0; col < cols; col++ ) {
                int i = row * cols + col;
                int count = validCounts[i];
                if (count >= minValid) {
                    double value = isMean ? accumulated[i] / count : accumulated[i];
                    aggregated.setValue(col, row, value);
                }
            }
        }
        outRaster = aggregated.buildCoverage();
    }

}
