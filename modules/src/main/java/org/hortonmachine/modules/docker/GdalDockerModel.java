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
import org.hortonmachine.gears.utils.StringUtilities;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ExecCreation;

public class GdalDockerModel extends HMModel {

    private DockerClient docker;

    protected String startContainer() throws Exception {
        docker = DefaultDockerClient.fromEnv().build();

        // Pull an image
        // docker.pull("osgeo/gdal");

        // Create container with exposed ports
        final ContainerConfig containerConfig = ContainerConfig.builder().image("osgeo/gdal")
                .cmd("sh", "-c", "while :; do sleep 1; done").build();

        final ContainerCreation creation = docker.createContainer(containerConfig);
        final String id = creation.id();
        // Start container
        docker.startContainer(id);
        return id;
    }

    public void execCommand( String containerId, String command ) throws Exception {
        try {
            // Exec command inside running container with attached STDOUT and STDERR
            String[] commandArray = StringUtilities.parseCommand(command);

            final ExecCreation execCreation = docker.execCreate(containerId, commandArray,
                    DockerClient.ExecCreateParam.attachStdout(), DockerClient.ExecCreateParam.attachStderr());
            final LogStream output = docker.execStart(execCreation.id());
            final String execOutput = output.readFully();
            pm.message(execOutput);

        } finally {
            // Kill container
            docker.killContainer(containerId);
            // Remove container
            docker.removeContainer(containerId);
        }
    }

    protected void closeClient() {
        // Close the docker client
        docker.close();
    }

}
