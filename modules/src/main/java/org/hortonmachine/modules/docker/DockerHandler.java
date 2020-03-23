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

import java.util.List;

import org.hortonmachine.dbs.utils.OsCheck;
import org.hortonmachine.dbs.utils.OsCheck.OSType;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.StringUtilities;
import org.joda.time.DateTime;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.ResponseItem.ProgressDetail;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;

@SuppressWarnings("deprecation")
public class DockerHandler {
    public static final String WORKSPACE = "/workspace";

    private static final String MSG_NOTRUNNING = "An error occurred with the docker instance. Is docker running on your machine?";
    private static final String MSG_WIN_SOCKET = "If docker is running, make sure set \n\nExpose daemon on tcp://localhost:2375 without TLS\n\nin the docker General docker Settings.";

    public static final String[] keepAliveCmd = {"sh", "-c", "while :; do sleep 1; done"};

    private DockerClient dockerClient;
    private String containerId;

    /**
     * Checks if a certain image is available.
     * 
     * @param imageName the image to check.
     * @return the id of the image or null.
     */
    public String hasImage( String imageName ) {
        List<Image> images = dockerClient.listImagesCmd().withImageNameFilter(imageName).exec();
        if (images.size() > 0) {
            return images.get(0).getId();
        } else {
            return null;
        }
    }

    public void removeImage( String imageId ) {
        dockerClient.removeImageCmd(imageId).exec();
    }

    public void pullImage( String imageName, String tag, IHMProgressMonitor pm ) throws Exception {
        PullImageResultCallback resultCallback = new PullImageResultCallback();
        if (pm != null) {
            resultCallback = new PullImageResultCallback(){
                @Override
                public void onNext( PullResponseItem item ) {
                    String id = item.getId();

                    if (item != null) {
                        ProgressDetail progressDetail = item.getProgressDetail();
                        if (progressDetail != null) {
                            Long currentObj = progressDetail.getCurrent();
                            Long totalObj = progressDetail.getTotal();
                            if (currentObj != null && totalObj != null) {
                                int current = (int) (currentObj / 1000);
                                int total = (int) (totalObj / 1000);
                                String totalUnit = "KB";
                                String currentUnit = "KB";

                                if (total > 1024) {
                                    total = total / 1000;
                                    totalUnit = "MB";
                                }
                                if (current > 1024) {
                                    current = current / 1000;
                                    currentUnit = "MB";
                                }

                                String msg = null;
                                if (current == total) {
                                    msg = "Finished downloading " + id;
                                } else {
                                    msg = "Downloading " + id + " ( " + current + currentUnit + "/" + total + totalUnit + " )";
                                }
                                pm.message(msg);
                            }
                        }
                    }
                    super.onNext(item);
                }
                @Override
                public void onError( Throwable throwable ) {
                    pm.errorMessage("Failed to start pull command:" + throwable.getMessage());
                    super.onError(throwable);
                }
            };
        }
        dockerClient.pullImageCmd(imageName)//
                .withTag(tag)//
                .exec(resultCallback);
        resultCallback.awaitCompletion();
    }

    /**
     * Initialize the docker client. 
     * 
     * @return an error message if there were issues, <code>null</code> if everyhtin gwent smooth.
     */
    public String initDocker() {
        try {
            DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder();
            if (OsCheck.getOperatingSystemType() == OSType.Windows) {
                builder.withDockerHost("tcp://localhost:2375");
            }
            dockerClient = DockerClientBuilder.getInstance(builder).build();

            dockerClient.versionCmd().exec();
        } catch (Exception e) {
            String msg = MSG_NOTRUNNING;
            if (OsCheck.getOperatingSystemType() == OSType.Windows) {
                msg += "\n" + MSG_WIN_SOCKET;
            }
            return msg;
        }

        return null;
    }

    public void startContainer( String imageName, String volumePath ) throws Exception {
        if (hasImage(imageName) == null) {
            throw new ModelsRuntimeException(
                    "The image " + imageName + " could not be found your system. Please run the GdalInstaller command first.",
                    this);
        }

//      List<Container> containers = dockerClient.listContainersCmd()
//              .withShowSize(true)
//              .withShowAll(true)
//              .withStatusFilter("exited").exec()

        String ts = DateTime.now().toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSScompact);

        String name = imageName.replace('/', '_').replace(':', '_');

        CreateContainerResponse container;
        if (volumePath != null) {
            Volume v = new Volume(WORKSPACE);
            container = dockerClient.createContainerCmd(imageName)//
                    .withCmd(keepAliveCmd)//
                    .withName(name + ts)//
                    .withWorkingDir(WORKSPACE)//
                    .withBinds(new Bind(volumePath, v))//
                    .exec();
        } else {
            container = dockerClient.createContainerCmd(imageName)//
                    .withCmd(keepAliveCmd)//
                    .withName(name + ts)//
                    .exec();
        }

        containerId = container.getId();
        dockerClient.startContainerCmd(containerId).exec();

        InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(containerId).exec();

        if (!inspectContainerResponse.getState().getRunning()) {
            throw new ModelsRuntimeException("Container not running.", this);
        }
    }

    public void execCommand( String command ) throws Exception {
        // Exec command inside running container with attached STDOUT and STDERR
        String[] commandArray = StringUtilities.parseCommand(command);

        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)//
                .withAttachStdout(true) //
                .withAttachStderr(true)//
                .withCmd(commandArray)//
                .withUser("root")//
                .exec();
        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(new ExecStartResultCallback(System.out, System.err))
                .awaitCompletion();

//            pm.message(execOutput);

    }

    public void closeClient() throws Exception {
        try {
            if (containerId != null) {
                dockerClient.stopContainerCmd(containerId).withTimeout(2).exec();
                dockerClient.removeContainerCmd(containerId).exec();
            }
        } finally {
            dockerClient.close();
        }
    }

}
