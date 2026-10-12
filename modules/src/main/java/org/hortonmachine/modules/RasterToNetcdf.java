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

import static org.hortonmachine.gears.io.netcdf.OmsGridCoverage2NetcdfConverter.AUTHORCONTACTS;
import static org.hortonmachine.gears.io.netcdf.OmsGridCoverage2NetcdfConverter.AUTHORNAMES;
import static org.hortonmachine.gears.io.netcdf.OmsGridCoverage2NetcdfConverter.DESCRIPTION;
import static org.hortonmachine.gears.io.netcdf.OmsGridCoverage2NetcdfConverter.DESCR_pLongName;
import static org.hortonmachine.gears.io.netcdf.OmsGridCoverage2NetcdfConverter.DESCR_pUnits;
import static org.hortonmachine.gears.io.netcdf.OmsGridCoverage2NetcdfConverter.DESCR_pVariableName;
import static org.hortonmachine.gears.io.netcdf.OmsGridCoverage2NetcdfConverter.DOCUMENTATION;
import static org.hortonmachine.gears.io.netcdf.OmsGridCoverage2NetcdfConverter.KEYWORDS;
import static org.hortonmachine.gears.io.netcdf.OmsGridCoverage2NetcdfConverter.LABEL;
import static org.hortonmachine.gears.io.netcdf.OmsGridCoverage2NetcdfConverter.LICENSE;
import static org.hortonmachine.gears.io.netcdf.OmsGridCoverage2NetcdfConverter.NAME;
import static org.hortonmachine.gears.io.netcdf.OmsGridCoverage2NetcdfConverter.STATUS;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hortonmachine.gears.io.netcdf.OmsGridCoverage2NetcdfConverter;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
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

@Description(DESCRIPTION)
@Documentation(DOCUMENTATION)
@Author(name = AUTHORNAMES, contact = AUTHORCONTACTS)
@Keywords(KEYWORDS)
@Label(LABEL)
@Name("_" + NAME)
@Status(STATUS)
@License(LICENSE)
public class RasterToNetcdf extends HMModel {

    @Description("The raster to write, for a file with a single raster.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description("Alternative to the raster: the folder with the rasters of a time series, all on the same grid, taken in the order of their file names.")
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inFolder;

    @Description("The UTC timestamps of the rasters, as yyyy-MM-dd HH:mm, one per line in the order of the rasters. Optional for a single raster, which then has no time.")
    @UI(HMConstants.MULTILINE_UI_HINT)
    @In
    public String pTimestamps;

    @Description(DESCR_pVariableName)
    @In
    public String pVariableName = "data";

    @Description(DESCR_pLongName)
    @In
    public String pLongName;

    @Description(DESCR_pUnits)
    @In
    public String pUnits;

    @Description("The NetCDF file to write.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNetcdf;

    @Execute
    public void process() throws Exception {
        checkNull(outNetcdf);

        List<String> timestamps = new ArrayList<>();
        if (pTimestamps != null) {
            for( String line : pTimestamps.split("\\R") ) {
                if (!line.isBlank()) {
                    timestamps.add(line.trim());
                }
            }
        }

        List<File> rasterFiles = new ArrayList<>();
        if (inRaster != null) {
            rasterFiles.add(new File(inRaster));
            if (timestamps.size() > 1) {
                throw new ModelsIllegalargumentException("A single raster can have only one timestamp.", this);
            }
        } else if (inFolder != null) {
            File[] files = new File(inFolder).listFiles(( dir, name ) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".tif") || lower.endsWith(".tiff") || lower.endsWith(".asc");
            });
            if (files == null || files.length == 0) {
                throw new ModelsIllegalargumentException("No rasters found in the folder: " + inFolder, this);
            }
            Arrays.sort(files);
            rasterFiles.addAll(Arrays.asList(files));
            if (timestamps.size() != rasterFiles.size()) {
                throw new ModelsIllegalargumentException("The folder has " + rasterFiles.size() + " rasters, but "
                        + timestamps.size() + " timestamps were given: a time series needs one per raster.", this);
            }
        } else {
            throw new ModelsIllegalargumentException("Either a raster or a folder of rasters is needed.", this);
        }

        OmsGridCoverage2NetcdfConverter converter = new OmsGridCoverage2NetcdfConverter();
        converter.pm = pm;
        converter.outPath = outNetcdf;
        converter.pVariableName = pVariableName;
        converter.pLongName = pLongName;
        converter.pUnits = pUnits;
        try {
            pm.beginTask("Writing " + rasterFiles.size() + " rasters...", rasterFiles.size());
            for( int i = 0; i < rasterFiles.size(); i++ ) {
                converter.inRaster = getRaster(rasterFiles.get(i).getAbsolutePath());
                converter.pTimestamp = timestamps.isEmpty() ? null : timestamps.get(i);
                converter.process();
                pm.worked(1);
            }
            pm.done();
        } finally {
            converter.close();
        }
    }
}
