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
import java.awt.image.RenderedImage;
import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.parameter.Parameter;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;

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

    /**
     * Read an image from a coverage reader.
     * 
     * @param reader the reader.
     * @param cols the expected cols.
     * @param rows the expected rows.
     * @param w west bound.
     * @param e east  bound.
     * @param s south bound.
     * @param n north bound.
     * @return the image or <code>null</code> if unable to read it.
     * @throws IOException
     */
    public static BufferedImage imageFromReader( AbstractGridCoverage2DReader reader, int cols, int rows, double w, double e,
            double s, double n ) throws IOException {
        GeneralParameterValue[] readParams = new GeneralParameterValue[1];
        Parameter<GridGeometry2D> readGG = new Parameter<GridGeometry2D>(AbstractGridFormat.READ_GRIDGEOMETRY2D);
        GridEnvelope2D gridEnvelope = new GridEnvelope2D(0, 0, cols, rows);
        DirectPosition2D minDp = new DirectPosition2D(w, s);
        DirectPosition2D maxDp = new DirectPosition2D(e, n);
        Envelope env = new Envelope2D(minDp, maxDp);
        readGG.setValue(new GridGeometry2D(gridEnvelope, env));
        readParams[0] = readGG;

        GridCoverage2D gridCoverage2D = reader.read(readParams);
        RenderedImage image = gridCoverage2D.getRenderedImage();
        if (image instanceof BufferedImage) {
            BufferedImage bImage = (BufferedImage) image;
            return bImage;
        }
        return null;
    }

}
