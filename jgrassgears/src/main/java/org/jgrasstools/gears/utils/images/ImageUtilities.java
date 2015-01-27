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
package org.jgrasstools.gears.utils.images;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * An utility class image handling. 
 *
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.9
 */
public class ImageUtilities {

    /**
     * Scale an image to a given size maintaining the ratio.
     * 
     * @param image the image to scale.
     * @param newSize the size of the new image (it will be used for the longer side).
     * @return the scaled image.
     * @throws Exception
     */
    public static BufferedImage scaleImage( BufferedImage image, int newSize ) throws Exception {

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int width;
        int height;
        if (imageWidth > imageHeight) {
            width = newSize;
            height = imageHeight * width / imageWidth;
        } else {
            height = newSize;
            width = height * imageWidth / imageHeight;
        }

        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();

        return resizedImage;
    }

}
