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
package org.hortonmachine.modules;

import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_doLatLon;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_inPath;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_pExcludePattern;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_pFromTimestep;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_pGridName;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_pIncludePattern;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdfTemporalAggregator.DESCR_outRaster;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdfTemporalAggregator.DESCR_pAggregation;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdfTemporalAggregator.DESCR_pMinValidFraction;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdfTemporalAggregator.DESCR_pToTimestep;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdfTemporalAggregator.MAX;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdfTemporalAggregator.MEAN;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdfTemporalAggregator.MIN;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdfTemporalAggregator.SUM;

import org.hortonmachine.gears.io.netcdf.OmsNetcdfTemporalAggregator;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OmsNetcdfTemporalAggregator.DESCRIPTION)
@Documentation(OmsNetcdfTemporalAggregator.DOCUMENTATION)
@Author(name = "Andrea Antonello", contact = "https://g-ant.eu")
@Keywords("IO, NetCDF, Raster, Time series, Aggregation, Statistics")
@Label(HMConstants.NETCDF)
@Name("_netcdftemporalaggregator")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class NetcdfTemporalAggregator extends HMModel {
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
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster = null;

    @Execute
    public void process() throws Exception {
        checkNull(inPath, outRaster);

        OmsNetcdfTemporalAggregator aggregator = new OmsNetcdfTemporalAggregator();
        aggregator.pm = pm;
        aggregator.inPath = inPath;
        aggregator.pGridName = pGridName;
        aggregator.pIncludePattern = pIncludePattern;
        aggregator.pExcludePattern = pExcludePattern;
        aggregator.pFromTimestep = pFromTimestep;
        aggregator.pToTimestep = pToTimestep;
        aggregator.pAggregation = pAggregation;
        aggregator.pMinValidFraction = pMinValidFraction;
        aggregator.doLatLon = doLatLon;
        aggregator.process();

        dumpRaster(aggregator.outRaster, outRaster);
    }
}
