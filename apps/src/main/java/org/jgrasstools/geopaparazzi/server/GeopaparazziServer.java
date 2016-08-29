package org.jgrasstools.geopaparazzi.server;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.spatialite.SpatialiteDb;
import org.jgrasstools.gears.spatialite.SpatialiteGeometryType;
import org.jgrasstools.gears.spatialite.SpatialiteImportUtils;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.geopaparazzi.GeopaparazziWorkspaceUtilities;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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
    private File gpapProjectsFolder;
    private File dataFolder;

    public static void main( String[] args ) {
        String projectsFolder = "/home/hydrologis/Dropbox/geopaparazzi/projects/";
        String dataFolder = "/home/hydrologis/data/";
        GeopaparazziServer server = new GeopaparazziServer(new File(projectsFolder), new File(dataFolder), 8080);
        ServerRunner.executeInstance(server);
    }

    public GeopaparazziServer( File gpapProjectsFolder, File dataFolder ) {
        super(8080);
        this.gpapProjectsFolder = gpapProjectsFolder;
        this.dataFolder = dataFolder;
    }

    public GeopaparazziServer( File gpapProjectsFolder, File dataFolder, int port ) {
        super(port);
        this.gpapProjectsFolder = gpapProjectsFolder;
        this.dataFolder = dataFolder;
    }

    @Override
    public Response serve( IHTTPSession session ) {
        Method method = session.getMethod();
        String uri = session.getUri();
        GeopaparazziServer.LOG.info(method + " '" + uri + "' ");

        // TODO check user
        //
        // resp.setContentType("application/json");
        // String authHeader = req.getHeader("Authorization");
        // String[] userPwd = StageUtils.getUserPwdWithBasicAuthentication(authHeader);
        // if (userPwd == null || !LoginChecker.isLoginOk(userPwd[0], userPwd[1])) {
        // throw new ServletException("No permission!");
        // }

        String msg = "A problem occurred";
        if (uri.substring(1).equalsIgnoreCase("")) {
            // list available projects
            File[] projectsList = getProjectsList();
            String tmpMsg = "<html><body><h1>Geopaparazzi projects on the server</h1>\n";
            for( File file : projectsList ) {
                tmpMsg += "<p><a href=\"project/" + file.getName() + "\"" + FileUtilities.getNameWithoutExtention(file)
                        + "</a></p>";
            }
            tmpMsg += "</body></html>\n";
            return newFixedLengthResponse(tmpMsg);
        } else if (uri.substring(1).toLowerCase().startsWith("project/")) {

        } else if (uri.substring(1).equalsIgnoreCase("stage_gplist_download")) {
            msg = loadProjectsList();
            return newFixedLengthResponse(msg);
        } else if (uri.substring(1).equalsIgnoreCase("stage_gpproject_upload")) {
            msg = getProjectFileFromDevice(session, msg);
        } else if (uri.substring(1).equalsIgnoreCase("stage_gpproject_download")) {
            String projectFileName = "";
            try {
                Map<String, List<String>> parms = session.getParameters();
                List<String> idParams = parms.get("id");
                if (idParams != null && idParams.size() == 1) {
                    projectFileName = idParams.get(0);
                    FileInputStream stream = new FileInputStream(new File(gpapProjectsFolder, projectFileName));
                    String mimeType = "application/octet-stream";
                    return newChunkedResponse(Status.OK, mimeType, stream);
                }
            } catch (FileNotFoundException e) {
                LOG.log(Level.SEVERE, msg, e);
                msg = msg + ". File not found: " + projectFileName;
            } catch (Exception e) {
                LOG.log(Level.SEVERE, msg, e);
            }
        } else if (uri.substring(1).equalsIgnoreCase("sync/layerinfo")) {
            try {
                return newFixedLengthResponse(getAvailableLayers());
            } catch (Exception e) {
                LOG.warning("ERROR: " + e.getMessage());
            }
        } else if (uri.substring(1).equalsIgnoreCase("sync/upload")) {
            // TODO
        } else if (uri.substring(1).equalsIgnoreCase("sync/download")) {
            // get post data json
            try {
                Map<String, String> files = new HashMap<String, String>();
                session.parseBody(files);
                // get the POST body
                String postBody = session.getQueryParameterString();

                JSONObject requestJson = new JSONObject(postBody);
                JSONArray layersArray = requestJson.getJSONArray("layers");
                int length = layersArray.length();
                List<String> fileNamesToMerge = new ArrayList<>();
                for( int i = 0; i < length; i++ ) {
                    fileNamesToMerge.add(layersArray.getString(i));
                }

                File tempDb = File.createTempFile("spatialite4geopaparazzi", ".sqlite");
                try (SpatialiteDb newDb = new SpatialiteDb()) {
                    newDb.open(tempDb.getAbsolutePath());
                    newDb.initSpatialMetadata(null);

                    for( String fileName : fileNamesToMerge ) {
                        File file = new File(dataFolder, fileName);
                        String tableName = SpatialiteImportUtils.createTableFromShp(newDb, file);
                        SpatialiteImportUtils.importShapefile(newDb, file, tableName, -1, new DummyProgressMonitor());
                    }
                }

                FileInputStream stream = new FileInputStream(tempDb);
                String mimeType = "application/octet-stream";
                return newChunkedResponse(Status.OK, mimeType, stream);
            } catch (Exception e) {
                LOG.warning("ERROR: " + e.getMessage());
            }
            return newFixedLengthResponse("");
        }

        return newFixedLengthResponse(msg);
    }

    protected String getProjectFileFromDevice( IHTTPSession session, String msg ) {
        try {
            Map<String, List<String>> parms = session.getParameters();
            List<String> idParams = parms.get("name");
            if (idParams != null && idParams.size() == 1) {

                String projectFileName = idParams.get(0);
                File file = new File(gpapProjectsFolder, projectFileName);
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
        return msg;
    }

    protected String getAvailableLayers() throws Exception {
        File[] shpFiles = dataFolder.listFiles(new FilenameFilter(){
            @Override
            public boolean accept( File dir, String name ) {
                return name.endsWith(".shp");
            }
        });

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for( int i = 0; i < shpFiles.length; i++ ) {
            File file = shpFiles[i];
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\n");
            sb.append("name:\"").append(file.getName()).append("\",\n");
            sb.append("title:\"").append(FileUtilities.getNameWithoutExtention(file)).append("\",\n");
            sb.append("abstract:\"").append(file.getAbsolutePath()).append("\",\n");

            FileDataStore store = FileDataStoreFinder.getDataStore(file);
            SimpleFeatureSource featureSource = store.getFeatureSource();
            GeometryDescriptor geometryDescriptor = featureSource.getSchema().getGeometryDescriptor();

            String geomType = "unknown";
            if (GeometryUtilities.isLine(geometryDescriptor)) {
                geomType = SpatialiteGeometryType.LINESTRING_XY.getDescription();
            } else if (GeometryUtilities.isPoint(geometryDescriptor)) {
                geomType = SpatialiteGeometryType.POINT_XY.getDescription();
            } else if (GeometryUtilities.isPolygon(geometryDescriptor)) {
                geomType = SpatialiteGeometryType.POLYGON_XY.getDescription();
            }
            sb.append("geomtype:\"").append(geomType).append("\",\n");

            CoordinateReferenceSystem crs = featureSource.getSchema().getCoordinateReferenceSystem();
            String codeFromCrs = CrsUtilities.getCodeFromCrs(crs);
            codeFromCrs = codeFromCrs.replaceFirst("EPSG:", "");
            sb.append("srid:").append(codeFromCrs).append(",\n");
            sb.append("permissions:").append("\"read-write\"").append(",\n");
            sb.append("last-modified:").append(new Date().getTime() / 1000).append("\n");
            sb.append("}\n");
        }
        sb.append("]\n");

        return sb.toString();
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
            File[] geopaparazziProjectFiles = GeopaparazziWorkspaceUtilities.getGeopaparazziFiles(gpapProjectsFolder);
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

    private File[] getProjectsList() {
        File[] geopaparazziProjectFiles = GeopaparazziWorkspaceUtilities.getGeopaparazziFiles(gpapProjectsFolder);
        return geopaparazziProjectFiles;
    }

}
