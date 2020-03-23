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
import java.io.File;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.modules.docker.GdalDockerModel;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description("A raster to tiff converter.")
@Author(name = "Antonello Andrea", contact = "http://www.hydrologis.com")
@Keywords("gdal, docker, raster, tiff")
@Label(HMConstants.GDAL)
@Name("_gdalraster2tiffconverter")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class GdalRaster2TiffConverter extends GdalDockerModel {
    @Description("The raster file to convert.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster = null;

    @Description("Force creation of tiled a tiff.")
    @In
    public boolean doTiles = true;

    @Description("Use jpeg compression")
    @In
    public boolean doJpegCompression = false;

    @Description("Additional options. The format is COMPRESS=DEFLATE or TFW=YES")
    @In
    public String pOptions = "";

    // -co options https://gdal.org/drivers/raster/gtiff.html#raster-gtiff
    // gdal_translate -of GTiff -co "TILED=YES" -co COMPRESS=JPEG -co PHOTOMETRIC=YCBCR $1 $2

    @Execute
    public void process() throws Exception {
        String error = checkDockerInstall();
        if (error == null) {
            try {
                checkNull(inRaster);

                checkFileExists(inRaster);
                File file = new File(inRaster);

                String inName = file.getName();

                String outName = FileUtilities.getNameWithoutExtention(file) + ".tiff";

                String workspace = file.getParentFile().getAbsolutePath();

                String cmd = "gdal_translate -of GTiff";
                if (doTiles) {
                    cmd += " -co TILED=YES";
                }
                if (doJpegCompression) {
                    cmd += " -co COMPRESS=JPEG";
                }

                if (pOptions != null) {
                    String[] split = pOptions.split("\\s+");
                    for( String option : split ) {
                        cmd += " -co " + option;
                    }
                }

                cmd += " " + inName + " " + outName;
                pm.message(cmd);
                startContainer(workspace);
                execCommand(cmd);
            } finally {
                closeClient();
            }
        } else {
            pm.errorMessage(error);
        }
    }

    public static void main( String[] args ) throws Exception {
        GdalRaster2TiffConverter i = new GdalRaster2TiffConverter();
        i.inRaster = "/Users/hydrologis/data/DTM_calvello/dtm_all.asc";
        i.doTiles = true;
        i.pOptions = "COMPRESS=DEFLATE TFW=YES";
        i.process();
    }
}
