package org.jgrasstools.server.geopaparazzi;

import java.io.File;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jgrasstools.server.geopaparazzi.servlets.ProjectDownloadHandler;
import org.jgrasstools.server.geopaparazzi.servlets.ProjectListHandler;
import org.jgrasstools.server.geopaparazzi.servlets.ProjectUploadHandler;
import org.jgrasstools.server.geopaparazzi.servlets.WelcomeHandler;
import org.jgrasstools.server.jetty.EmbeddedJspServer;
import org.jgrasstools.server.jetty.utils.DisabledLogging;

public class GeopaparazziServer extends EmbeddedJspServer {
    private static final Logger LOG = Logger.getLogger(GeopaparazziServer.class.getName());

    public static String PROJECTS_FOLDER_KEY = "PROJECTS_FOLDER_KEY";
    public static String DATA_FOLDER_KEY = "DATA_FOLDER_KEY";

    private File gpapProjectsFolder;

    public GeopaparazziServer( Integer port, File gpapProjectsFolder ) throws Exception {
        super(port, null);
        this.gpapProjectsFolder = gpapProjectsFolder;
    }

    @Override
    public void stop() {
        // in case stop stuff here
        try {
            super.stop();
        } catch (Exception e) {
            LOG.severe("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void configureWebAppContext( WebAppContext webapp ) {
    }

    @Override
    protected void configureServletHandler( ServletHandler servletHandler ) {
    }

    @Override
    protected void doPreStart() {
        ContextHandler welcomeContext = new ContextHandler("/");
        welcomeContext.setContextPath("/");
        welcomeContext.setHandler(new WelcomeHandler(gpapProjectsFolder));

        ContextHandler projectsListContext = new ContextHandler("/stage_gplist_download");
        projectsListContext.setHandler(new ProjectListHandler(gpapProjectsFolder));

        ContextHandler projectDownloadContext = new ContextHandler("/stage_gpproject_download");
        projectDownloadContext.setHandler(new ProjectDownloadHandler(gpapProjectsFolder));

        ContextHandler projectUploadContext = new ContextHandler("/stage_gpproject_upload");
        projectUploadContext.setHandler(new ProjectUploadHandler(gpapProjectsFolder));

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[]{welcomeContext, projectDownloadContext, projectUploadContext, projectsListContext});

        _server.setHandler(contexts);

    }

    // TODO add back when spatialite layers are supported
    // part contexts
    // } else if (uri.substring(1).equalsIgnoreCase("sync/layerinfo")) {
    // try {
    // return newFixedLengthResponse(getAvailableLayers());
    // } catch (Exception e) {
    // LOG.warning("ERROR: " + e.getMessage());
    // }
    // } else if (uri.substring(1).equalsIgnoreCase("sync/upload")) {
    // // TODO
    // } else if (uri.substring(1).equalsIgnoreCase("sync/download")) {
    // // get post data json
    // try {
    // Map<String, String> files = new HashMap<String, String>();
    // session.parseBody(files);
    // // get the POST body
    // String postBody = session.getQueryParameterString();
    //
    // JSONObject requestJson = new JSONObject(postBody);
    // JSONArray layersArray = requestJson.getJSONArray("layers");
    // int length = layersArray.length();
    // List<String> fileNamesToMerge = new ArrayList<>();
    // for( int i = 0; i < length; i++ ) {
    // fileNamesToMerge.add(layersArray.getString(i));
    // }
    //
    // File tempDb = File.createTempFile("spatialite4geopaparazzi", ".sqlite");
    // try (ASpatialDb newDb = new SpatialiteDb()) {
    // newDb.open(tempDb.getAbsolutePath());
    // newDb.initSpatialMetadata(null);
    //
    // for( String fileName : fileNamesToMerge ) {
    // File file = new File(dataFolder, fileName);
    // String tableName = SpatialiteImportUtils.createTableFromShp(newDb, file);
    // SpatialiteImportUtils.importShapefile(newDb, file, tableName, -1, new
    // DummyProgressMonitor());
    // }
    // }
    //
    // FileInputStream stream = new FileInputStream(tempDb);
    // String mimeType = "application/octet-stream";
    // return newChunkedResponse(Status.OK, mimeType, stream);
    // } catch (Exception e) {
    // LOG.warning("ERROR: " + e.getMessage());
    // }
    // return newFixedLengthResponse("");
    // }

    // PART impl
    // protected String getAvailableLayers() throws Exception {
    // File[] shpFiles = dataFolder.listFiles(new FilenameFilter(){
    // @Override
    // public boolean accept( File dir, String name ) {
    // return name.endsWith(".shp");
    // }
    // });
    //
    // StringBuilder sb = new StringBuilder();
    // sb.append("[");
    //
    // for( int i = 0; i < shpFiles.length; i++ ) {
    // File file = shpFiles[i];
    // if (i > 0) {
    // sb.append(",");
    // }
    // sb.append("{\n");
    // sb.append("name:\"").append(file.getName()).append("\",\n");
    // sb.append("title:\"").append(FileUtilities.getNameWithoutExtention(file)).append("\",\n");
    // sb.append("abstract:\"").append(file.getAbsolutePath()).append("\",\n");
    //
    // FileDataStore store = FileDataStoreFinder.getDataStore(file);
    // SimpleFeatureSource featureSource = store.getFeatureSource();
    // GeometryDescriptor geometryDescriptor = featureSource.getSchema().getGeometryDescriptor();
    //
    // String geomType = "unknown";
    // if (GeometryUtilities.isLine(geometryDescriptor)) {
    // geomType = SpatialiteGeometryType.LINESTRING_XY.getDescription();
    // } else if (GeometryUtilities.isPoint(geometryDescriptor)) {
    // geomType = SpatialiteGeometryType.POINT_XY.getDescription();
    // } else if (GeometryUtilities.isPolygon(geometryDescriptor)) {
    // geomType = SpatialiteGeometryType.POLYGON_XY.getDescription();
    // }
    // sb.append("geomtype:\"").append(geomType).append("\",\n");
    //
    // CoordinateReferenceSystem crs = featureSource.getSchema().getCoordinateReferenceSystem();
    // String codeFromCrs = CrsUtilities.getCodeFromCrs(crs);
    // codeFromCrs = codeFromCrs.replaceFirst("EPSG:", "");
    // sb.append("srid:").append(codeFromCrs).append(",\n");
    // sb.append("permissions:").append("\"read-write\"").append(",\n");
    // sb.append("last-modified:").append(new Date().getTime() / 1000).append("\n");
    // sb.append("}\n");
    // }
    // sb.append("]\n");
    //
    // return sb.toString();
    // }

    public static void main( String[] args ) throws Exception {
        org.eclipse.jetty.util.log.Log.setLog(new DisabledLogging());

        File gpapProjectsFolder = new File("/home/hydrologis/Dropbox/geopaparazzi/projects");
        // File dataFolder = new File("/home/hydrologis/data/");

        GeopaparazziServer jspServer = new GeopaparazziServer(8081, gpapProjectsFolder);
        jspServer.start();
    }
}
