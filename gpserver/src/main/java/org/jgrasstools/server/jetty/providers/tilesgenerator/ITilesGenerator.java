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

import java.io.OutputStream;

/**
 * Tiles generator interface.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface ITilesGenerator extends ITilesObject {
    public String getUrl();

    /**
     * Gets the bytes of the image.
     * 
     * @param xtile the x tile.
     * @param yTile the y tile.
     * @param zoom the zoom level.
     * @param outputStream the stream to write to. If you need a byte[]:
     *    <code>
     *    ByteArrayOutputStream baos = new ByteArrayOutputStream();
     *    ImageIO.write( originalImage, "jpg", baos );
     *    baos.flush();
     *    byte[] imageInByte = baos.toByteArray();
     *    baos.close();
     *    </code>
     * @throws Exception 
     */
    public void getTile( int xtile, int yTile, int zoom, OutputStream outputStream ) throws Exception;
}
