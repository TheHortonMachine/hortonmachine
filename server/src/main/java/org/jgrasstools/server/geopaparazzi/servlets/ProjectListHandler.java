package org.jgrasstools.server.geopaparazzi.servlets;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.jgrasstools.server.geopaparazzi.GeopaparazziWorkspaceUtilities;

public class ProjectListHandler extends AbstractHandler {

    private File gpapProjectsFolder;

    public ProjectListHandler( File gpapProjectsFolder ) {
        this.gpapProjectsFolder = gpapProjectsFolder;
    }

    public void handle( String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response )
            throws IOException, ServletException {
        try {
            response.setContentType("text/json");
            PrintWriter out = response.getWriter();
            String projectsList = GeopaparazziWorkspaceUtilities.loadProjectsList(gpapProjectsFolder);
            out.write(projectsList);

        } catch (Exception e) {
            e.printStackTrace();
        }
        baseRequest.setHandled(true);
    }
}