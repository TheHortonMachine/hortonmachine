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
import java.util.HashMap;
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
public class TilesGeneratorServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(TilesGeneratorServlet.class.getName());

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    @Override
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        try {
            ServletContext servletContext = request.getServletContext();

            int xTile = Integer.parseInt(request.getParameter(ITilesObject.X));
            int yTile = Integer.parseInt(request.getParameter(ITilesObject.Y));
            int zoom = Integer.parseInt(request.getParameter(ITilesObject.Z));
            String generatorName = request.getParameter(ITilesObject.ID);

            HashMap<String, ITilesGenerator> tilesGenerators = (HashMap<String, ITilesGenerator>) servletContext
                    .getAttribute(IProvider.OFFLINE_TILESGENERATORS);

            ITilesGenerator tilesGenerator = tilesGenerators.get(generatorName);
            ServletOutputStream outputStream = response.getOutputStream();
            tilesGenerator.getTile(xTile, yTile, zoom, outputStream);
            outputStream.flush();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "ERROR", e);
            throw new ServletException(e);
        }
    }

}