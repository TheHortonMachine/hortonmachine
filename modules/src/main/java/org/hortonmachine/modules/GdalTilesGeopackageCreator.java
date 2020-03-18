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

@Description("Gdal geopackage tiles creation command.")
@Author(name = "Antonello Andrea", contact = "http://www.hydrologis.com")
@Keywords("gdal, docker, geopackage")
@Label(HMConstants.GDAL)
@Name("_gdalgeopackagecreator")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class GdalTilesGeopackageCreator extends GdalDockerModel {
    @Description("The raster file to convert.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster = null;

    @Description("The name of the geopackage (if null, the name of the raster is used).")
    @In
    public String inName = null;

    @Description("The name of the new table to create (if null, the geopackage name is used).")
    @In
    public String inTable = null;

    @Description("Create also lower zoomlevel.")
    @In
    public boolean doLowerZoomLevels = false;

    @Execute
    public void process() throws Exception {
        String error = checkDockerInstall();
        if (error == null) {
            try {
                checkNull(inRaster);

                checkFileExists(inRaster);
                File file = new File(inRaster);

                if (inName == null) {
                    inName = FileUtilities.getNameWithoutExtention(file);
                }
                if (inTable == null) {
                    inTable = inName;
                }

                if (!inName.endsWith("gpkg")) {
                    inName += ".gpkg";
                }

                String workspace = file.getParentFile().getAbsolutePath();

                String cmd = "gdal_translate -of GPKG " + file.getName() + " " + inName
                        + " -co  APPEND_SUBDATASET=YES -co RASTER_TABLE=" + inTable + " -co TILING_SCHEME=GoogleMapsCompatible";
                pm.message(cmd);
                startContainer(workspace);
                execCommand(cmd);

                if (doLowerZoomLevels) {
                    cmd = "gdaladdo -r cubic " + inName + " 2 4 8 16 32";
                    pm.message(cmd);

                    execCommand(cmd);
                }
            } finally {
                closeClient();
            }
        } else {
            pm.errorMessage(error);
        }
    }

    public static void main( String[] args ) throws Exception {
        GdalTilesGeopackageCreator i = new GdalTilesGeopackageCreator();
        i.inRaster = "/Users/hydrologis/data/geopackage/mebo2017.tiff";
        i.inName = "mebo_orthos.gpkg";
        i.inTable = "mebo2017";
        i.doLowerZoomLevels = true;
        i.process();
    }
}
