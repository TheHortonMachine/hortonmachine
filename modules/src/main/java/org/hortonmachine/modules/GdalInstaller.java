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
import org.hortonmachine.gears.libs.modules.HMConstants;
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

@Description("Gdal installer command.")
@Author(name = "Antonello Andrea", contact = "http://www.hydrologis.com")
@Keywords("gdal, docker")
@Label(HMConstants.GDAL)
@Name("_gdalinstaller")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class GdalInstaller extends GdalDockerModel {

    @Description("Force image removal and new download.")
    @In
    public boolean doForce = false;

    @Execute
    public void process() throws Exception {
        String error = checkDockerInstall();
        if (error == null) {
            try {
                String imageId = hasImage();
                if (imageId != null && doForce) {
                    removeImage(imageId);
                    imageId = hasImage();
                }

                if (imageId == null) {
                    pm.beginTask("Downloading gdal osgeo image. This will take a while depending on your network quality...", -1);
                    pullImage(pm);
                    pm.done();
                }
            } finally {
                closeClient();
            }
        } else {
            pm.errorMessage(error);
        }
    }

    public static void main( String[] args ) throws Exception {
        GdalInstaller i = new GdalInstaller();
        i.process();
    }
}
