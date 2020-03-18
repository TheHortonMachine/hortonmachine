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
package org.hortonmachine.modules.docker;

import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

/**
 * @author hydrologis
 *
 */
public class GdalDockerModel extends HMModel {

    private static final String TAG = "latest";
    private static final String OSGEO_GDAL = "osgeo/gdal";
    private static final String OSGEO_GDAL_WITHTAG = OSGEO_GDAL + ":" + TAG;
    protected DockerHandler dockerHandler = new DockerHandler();

    protected void startContainer( String volumePath ) throws Exception {
        dockerHandler.startContainer(OSGEO_GDAL, volumePath);
    }

    /**
     * Checks if docker client is initialized and if not, inits it.
     * 
     * If it can't init it: it returns an error string, else null.
     */
    protected String checkDockerInstall() {
        String error = dockerHandler.initDocker();
        if (error !=null) {
            return error;
        }
        return null;
    }

    protected String hasImage() {
        return dockerHandler.hasImage(OSGEO_GDAL_WITHTAG);
    }

    protected void pullImage( IHMProgressMonitor pm ) throws Exception {
        dockerHandler.pullImage(OSGEO_GDAL, TAG, pm);
    }

    protected void removeImage( String id ) {
        dockerHandler.removeImage(id);
    }

    public void execCommand( String command ) throws Exception {
        dockerHandler.execCommand(command);
    }
    public void closeClient() throws Exception {
        dockerHandler.closeClient();
    }

}
