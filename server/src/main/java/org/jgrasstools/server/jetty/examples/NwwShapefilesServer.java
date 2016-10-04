package org.jgrasstools.server.jetty.examples;

import org.apache.log4j.BasicConfigurator;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jgrasstools.server.jetty.EmbeddedJspServer;
import org.jgrasstools.server.jetty.map.NwwDataProvider;
import org.jgrasstools.server.jetty.map.ShapeFileDataProvider;
import org.jgrasstools.server.jetty.utils.DisabledLogging;

public class NwwShapefilesServer extends EmbeddedJspServer {

    public NwwShapefilesServer( Integer port, String webappFolder ) {
        super(port, webappFolder);
    }

    @Override
    protected void configureWebAppContext( WebAppContext webapp ) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void configureServletHandler( ServletHandler servletHandler ) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void doPreStart() {

        Context servletContext = webapp.getServletContext();

        try {
            NwwDataProvider[] providers = new NwwDataProvider[]{ //
                    new ShapeFileDataProvider(
                            "/home/hydrologis/development/jgrasstools-git/server/src/main/resources/naturalearth/ne_10m_roads.shp",
                            "continent='Europe'", null),
                    new ShapeFileDataProvider(
                            "/home/hydrologis/development/jgrasstools-git/server/src/main/resources/naturalearth/ne_10m_populated_places.shp",
                            "SOV_A3='ITA'", "NAME")
                    //
            };
            servletContext.setAttribute(NwwDataProvider.PROVIDERS, providers);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main( String[] args ) throws Exception {
        BasicConfigurator.configure();
        org.eclipse.jetty.util.log.Log.setLog(new DisabledLogging());

        String webFolder = "/home/hydrologis/development/jgrasstools-git/server/src/main/webapp";
        NwwShapefilesServer jspServer = new NwwShapefilesServer(null, webFolder);
        jspServer.start();
    }
}
