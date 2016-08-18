package org.jgrasstools.geopaparazzi.server;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jaitools.numeric.Statistic;
import org.jgrasstools.geopaparazzi.GeopaparazziWorkspaceUtilities;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.util.ServerRunner;

/**
 * A simple simple Geopaparazzi Project server.
 */
public class GeopaparazziServer extends NanoHTTPD {

    /**
     * logger to log to.
     */
    private static final Logger LOG = Logger.getLogger(GeopaparazziServer.class.getName());
    private File projectsFolder;

    public static void main( String[] args ) {
        String pathname = "path to projects folder";
        GeopaparazziServer server = new GeopaparazziServer(new File(pathname));
        ServerRunner.executeInstance(server);
    }

    public GeopaparazziServer( File projectsFolder ) {
        super(8080);
        this.projectsFolder = projectsFolder;
    }

    @Override
    public Response serve( IHTTPSession session ) {
        Method method = session.getMethod();
        String uri = session.getUri();
        GeopaparazziServer.LOG.info(method + " '" + uri + "' ");

        String msg = "A problem occurred";
        if (uri.substring(1).equalsIgnoreCase("stage_gplist_download")) {
            msg = loadProjectsList();
            return newFixedLengthResponse(msg);
        } else if (uri.substring(1).equalsIgnoreCase("stage_gpproject_upload")) {
            try {
                Map<String, List<String>> parms = session.getParameters();
                List<String> idParams = parms.get("name");
                if (idParams != null && idParams.size() == 1) {

                    String projectFileName = idParams.get(0);
                    File file = new File(projectsFolder, projectFileName);
                    if (file.exists()) {
                        msg = msg + ". File already exists on the server: " + projectFileName;
                        throw new ResponseException(Status.BAD_REQUEST, msg);
                    } else {
                        long size = ((HTTPSession) session).getBodySize();
                        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                        int REQUEST_BUFFER_LEN = 512;
                        // Read all the body and write it to request_data_output
                        byte[] buf = new byte[REQUEST_BUFFER_LEN];
                        int rlen = 0;
                        while( rlen >= 0 && size > 0 ) {
                            rlen = session.getInputStream().read(buf, 0, (int) Math.min(size, REQUEST_BUFFER_LEN));
                            size -= rlen;
                            if (rlen > 0) {
                                randomAccessFile.write(buf, 0, rlen);
                            }
                        }
                        safeClose(randomAccessFile);
                    }
                }
            } catch (IOException e1) {
                LOG.log(Level.SEVERE, msg, e1);
            } catch (Exception e1) {
                LOG.log(Level.SEVERE, msg, e1);
            }
        } else if (uri.substring(1).equalsIgnoreCase("stage_gpproject_download")) {
            Map<String, List<String>> parms = session.getParameters();
            List<String> idParams = parms.get("id");
            if (idParams != null && idParams.size() == 1) {
                String projectFileName = idParams.get(0);
                try {
                    FileInputStream stream = new FileInputStream(new File(projectsFolder, projectFileName));
                    String mimeType = "application/octet-stream";
                    return newChunkedResponse(Status.OK, mimeType, stream);
                } catch (FileNotFoundException e) {
                    LOG.log(Level.SEVERE, msg, e);
                    msg = msg + ". File not found: " + projectFileName;
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, msg, e);
                }
            }
        }

        return newFixedLengthResponse(msg);
    }

    private static final void safeClose( Object closeable ) {
        try {
            if (closeable != null) {
                if (closeable instanceof Closeable) {
                    ((Closeable) closeable).close();
                } else if (closeable instanceof Socket) {
                    ((Socket) closeable).close();
                } else if (closeable instanceof ServerSocket) {
                    ((ServerSocket) closeable).close();
                } else {
                    throw new IllegalArgumentException("Unknown object to close");
                }
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Could not close", e);
        }
    }

    private String loadProjectsList() {
        // resp.setContentType("application/json");
        // String authHeader = req.getHeader("Authorization");
        // String[] userPwd = StageUtils.getUserPwdWithBasicAuthentication(authHeader);
        // if (userPwd == null || !LoginChecker.isLoginOk(userPwd[0], userPwd[1])) {
        // throw new ServletException("No permission!");
        // }

        GeopaparazziServer.LOG.info("Project query incoming");

        try {
            File[] geopaparazziProjectFiles = GeopaparazziWorkspaceUtilities.getGeopaparazziFiles(projectsFolder);
            List<HashMap<String, String>> projectMetadataList = GeopaparazziWorkspaceUtilities
                    .readProjectMetadata(geopaparazziProjectFiles);

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"projects\": [");

            for( int i = 0; i < projectMetadataList.size(); i++ ) {
                HashMap<String, String> metadataMap = projectMetadataList.get(i);
                long fileSize = geopaparazziProjectFiles[i].length();
                if (i > 0)
                    sb.append(",");
                sb.append("{");
                sb.append("    \"id\": \"" + geopaparazziProjectFiles[i].getName() + "\",");
                sb.append("    \"title\": \"" + metadataMap.get("description") + "\",");
                sb.append("    \"date\": \"" + metadataMap.get("creationts") + "\",");
                sb.append("    \"author\": \"" + metadataMap.get("creationuser") + "\",");
                sb.append("    \"name\": \"" + metadataMap.get("name") + "\",");
                sb.append("    \"size\": \"" + fileSize + "\"");
                sb.append("}");
            }

            sb.append("]");
            sb.append("}");

            return sb.toString();
        } catch (Exception e) {
            return "An error occurred: " + e.getMessage();
        }
    }

}
