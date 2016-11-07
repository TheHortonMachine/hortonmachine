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
package org.jgrasstools.server.jetty.providers.tilesgenerator;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jgrasstools.server.jetty.providers.IProvider;

/**
 * Mapsforge tiles provider servlet.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapsforgeTilesGeneratorServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(MapsforgeTilesGeneratorServlet.class.getName());

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        try {
            ServletContext servletContext = request.getServletContext();

            int xTile = Integer.parseInt(request.getParameter("x"));
            int yTile = Integer.parseInt(request.getParameter("y"));
            int zoom = Integer.parseInt(request.getParameter("z"));
            String generatorName = request.getParameter("id");

            ITilesGenerator[] tilesGenerators = (ITilesGenerator[]) servletContext
                    .getAttribute(IProvider.OFFLINE_MAPSFORGE_TILESGENERATORS);

            ITilesGenerator tilesGenerator = null;
            if (tilesGenerators.length == 1) {
                tilesGenerator = tilesGenerators[0];
            } else {
                for( ITilesGenerator iTilesGenerator : tilesGenerators ) {
                    if (iTilesGenerator.getName().equals(generatorName)) {
                        tilesGenerator = iTilesGenerator;
                        break;
                    }
                }
            }

            ServletOutputStream outputStream = response.getOutputStream();
            tilesGenerator.getTile(xTile, yTile, zoom, outputStream);
            outputStream.flush();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "ERROR", e);
            throw new ServletException(e);
        }
    }

}