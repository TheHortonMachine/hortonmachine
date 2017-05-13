/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.geopaparazzi.simpleserver.servlets;
import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.jgrasstools.geopaparazzi.simpleserver.GeopaparazziServerUtilities;

/**
 * A project download handler.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class ProjectDownloadHandler extends AbstractHandler {

    private File gpapProjectsFolder;

    public ProjectDownloadHandler( File gpapProjectsFolder ) {
        this.gpapProjectsFolder = gpapProjectsFolder;
    }

    public void handle( String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response )
            throws IOException, ServletException {
        try {
            GeopaparazziServerUtilities.handleProjectDownload(gpapProjectsFolder, request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        baseRequest.setHandled(true);
    }

}