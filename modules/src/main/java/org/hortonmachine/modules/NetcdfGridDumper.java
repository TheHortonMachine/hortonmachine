/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_doLongitudeShift;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_inPath;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_outFolder;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_pFalseEastingCorrection;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_pFalseNorthingCorrection;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_pFromTimestep;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_pGridName;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_pNorthPoleLatitudeCorrection;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_pNorthPoleLongitudeCorrection;
import static org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter.DESCR_pToTimestep;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.netcdf.OmsNetcdf2GridCoverageConverter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.files.FileUtilities;

import oms3.annotations.Author;
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

@Description("Dump NetCDF grids to geotools compatible rasters convering them to lat/lon epsg:4326.")
@Author(name = "Antonello Andrea", contact = "http://www.hydrologis.com")
@Keywords("netdcf")
@Label(HMConstants.NETCDF)
@Name("_netcdfgriddumper")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class NetcdfGridDumper extends HMModel {
    @Description(DESCR_inPath)
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inPath = null;

    @Description(DESCR_pGridName)
    @In
    public String pGridName = null;

    @Description(DESCR_pFromTimestep)
    @In
    public Integer pFromTimestep = 0;

    @Description(DESCR_pToTimestep)
    @In
    public Integer pToTimestep;

    @Description(DESCR_pFalseEastingCorrection)
    @In
    public Double pFalseEastingCorrection = null;

    @Description(DESCR_pFalseNorthingCorrection)
    @In
    public Double pFalseNorthingCorrection = null;

    @Description(DESCR_pNorthPoleLongitudeCorrection)
    @In
    public Double pNorthPoleLongitudeCorrection = null;

    @Description(DESCR_pNorthPoleLatitudeCorrection)
    @In
    public Double pNorthPoleLatitudeCorrection = null;
    
    @Description(DESCR_outFolder)
    @UI(HMConstants.FOLDEROUT_UI_HINT)
    @Out
    public String outFolder;

    @Execute
    public void process() throws Exception {

        OmsNetcdf2GridCoverageConverter converter = new OmsNetcdf2GridCoverageConverter();
        converter.inPath = inPath;
        converter.pGridName = pGridName;
        converter.pFromTimestep = pFromTimestep;
        converter.pToTimestep = pToTimestep;
        converter.pFalseEastingCorrection = pFalseEastingCorrection;
        converter.pFalseNorthingCorrection = pFalseNorthingCorrection;
        converter.pNorthPoleLatitudeCorrection = pNorthPoleLatitudeCorrection;
        converter.pNorthPoleLongitudeCorrection = pNorthPoleLongitudeCorrection;

        converter.initProcess();

        while( converter.doProcess ) {
            converter.process();
            GridCoverage2D outRaster = converter.outRaster;
            Date date = converter.currentDate;
            SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String tsString = f.format(date);
            File folderFile = new File(outFolder);
            File inFile = new File(inPath);
            String name = FileUtilities.getNameWithoutExtention(inFile);
            name = name + "__" + tsString + ".tif";
            File outFile = new File(folderFile, name);
            dumpRaster(outRaster, outFile.getAbsolutePath());
        }

    }
}
