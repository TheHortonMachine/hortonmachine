package org.hortonmachine.geopaparazzi;

import java.io.File;
import java.util.logging.Logger;

import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.hortonmachine.geopaparazzi.simpleserver.DisabledLogging;
import org.hortonmachine.geopaparazzi.simpleserver.EmbeddedJspServer;
import org.hortonmachine.geopaparazzi.simpleserver.servlets.ProjectDownloadHandler;
import org.hortonmachine.geopaparazzi.simpleserver.servlets.ProjectListHandler;
import org.hortonmachine.geopaparazzi.simpleserver.servlets.ProjectUploadHandler;
import org.hortonmachine.geopaparazzi.simpleserver.servlets.WelcomeHandler;

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
        if (getContextSecurityHandler() != null) {
        	getContextSecurityHandler().setHandler(contexts);	
        }
        else {
        	_server.setHandler(contexts);
        }
    }

    public static void main( String[] args ) throws Exception {
        org.eclipse.jetty.util.log.Log.setLog(new DisabledLogging());

        File gpapProjectsFolder = new File("/home/hydrologis/Dropbox/geopaparazzi/projects");
        // File dataFolder = new File("/home/hydrologis/data/");

        GeopaparazziServer jspServer = new GeopaparazziServer(8081, gpapProjectsFolder);
        // Uncomment to require authentication
        jspServer.enableBasicAuth("user", "geopap");
        jspServer.start();
    }
}
