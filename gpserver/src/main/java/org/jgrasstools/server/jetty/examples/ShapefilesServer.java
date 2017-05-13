package org.jgrasstools.server.jetty.examples;

import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jgrasstools.server.jetty.EmbeddedJspServer;
import org.jgrasstools.server.jetty.providers.IProvider;
import org.jgrasstools.server.jetty.providers.data.NwwDataProvider;
import org.jgrasstools.server.jetty.providers.data.ShapeFileDataProvider;
import org.jgrasstools.server.jetty.providers.tiles.ATilesProvider;
import org.jgrasstools.server.jetty.providers.tiles.OsmOLProvider;
import org.jgrasstools.server.jetty.providers.tiles.TilesCollection;
import org.jgrasstools.server.jetty.utils.DisabledLogging;

public class ShapefilesServer extends EmbeddedJspServer {

    public ShapefilesServer( Integer port, String webappFolder ) {
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
                            "/home/hydrologis/data/naturalearth/ne_10m_roads/ne_10m_roads.shp",
                            "continent='Europe'", null),
                    new ShapeFileDataProvider(
                            "/home/hydrologis/data/naturalearth/ne_10m_populated_places/ne_10m_populated_places.shp",
                            "SOV_A3='POL'", "NAME")
                    //
            };
            servletContext.setAttribute(IProvider.DATAPROVIDERS, providers);
            
            TilesCollection tc = new TilesCollection();
            ATilesProvider[] tilesProviders = new ATilesProvider[]{
                    new OsmOLProvider("OpenStreetMap"),
                    tc.getAerialAltoAdigeProvider(),
                    tc.getCtpTrentinoProvider(),
                    tc.getRoadsProvider(),
                    tc.getAerialWithLabelsProvider()
            };
            servletContext.setAttribute(IProvider.TILESPROVIDERS, tilesProviders);
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main( String[] args ) throws Exception {
        org.eclipse.jetty.util.log.Log.setLog(new DisabledLogging());

        String webFolder = "/home/hydrologis/development/jgrasstools-git/server/src/main/webapp";
        ShapefilesServer jspServer = new ShapefilesServer(null, webFolder);
        jspServer.start();
    }
}
