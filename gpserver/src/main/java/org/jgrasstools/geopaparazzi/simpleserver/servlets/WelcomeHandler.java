package org.jgrasstools.geopaparazzi.simpleserver.servlets;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.jgrasstools.gears.io.geopaparazzi.GeopaparazziUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;

public class WelcomeHandler extends AbstractHandler {

    private File gpapProjectsFolder;

    public WelcomeHandler( File gpapProjectsFolder ) {
        this.gpapProjectsFolder = gpapProjectsFolder;
    }

    public void handle( String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response )
            throws IOException, ServletException {
        try {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();

            File[] projectsList = GeopaparazziUtilities.getGeopaparazziFiles(gpapProjectsFolder);
            String tmpMsg = "<html><body><h1>Geopaparazzi projects on the server</h1>\n";
            tmpMsg += "<p><ol>";
            for( File file : projectsList ) {
                tmpMsg += "<li>" + FileUtilities.getNameWithoutExtention(file) + "</li>";
            }
            tmpMsg += "</ol></p>";
            tmpMsg += "</body></html>\n";
            out.write(tmpMsg);

        } catch (Exception e) {
            e.printStackTrace();
        }
        baseRequest.setHandled(true);
    }
}