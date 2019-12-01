/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.gears.utils.images;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Hashtable;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.processing.Operations;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.parameter.Parameter;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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

    public static BufferedImage rotateImageByDegrees( BufferedImage img, double angle ) {
        double rads = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
        int w = img.getWidth();
        int h = img.getHeight();
        int newWidth = (int) Math.floor(w * cos + h * sin);
        int newHeight = (int) Math.floor(h * cos + w * sin);

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate((newWidth - w) / 2, (newHeight - h) / 2);

        int x = w / 2;
        int y = h / 2;

        at.rotate(rads, x, y);
        g2d.setTransform(at);
        g2d.drawImage(img, 0, 0, null);
        g2d.setColor(Color.RED);
        g2d.drawRect(0, 0, newWidth - 1, newHeight - 1);
        g2d.dispose();

        return rotated;
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
            double s, double n, CoordinateReferenceSystem resampleCrs ) throws IOException {
        CoordinateReferenceSystem sourceCrs = reader.getCoordinateReferenceSystem();
        GeneralParameterValue[] readParams = new GeneralParameterValue[1];
        Parameter<GridGeometry2D> readGG = new Parameter<GridGeometry2D>(AbstractGridFormat.READ_GRIDGEOMETRY2D);
        GridEnvelope2D gridEnvelope = new GridEnvelope2D(0, 0, cols, rows);
        DirectPosition2D minDp = new DirectPosition2D(sourceCrs, w, s);
        DirectPosition2D maxDp = new DirectPosition2D(sourceCrs, e, n);
        Envelope env = new Envelope2D(minDp, maxDp);
        readGG.setValue(new GridGeometry2D(gridEnvelope, env));
        readParams[0] = readGG;

        GridCoverage2D gridCoverage2D = reader.read(readParams);
        if (gridCoverage2D == null) {
            return null;
        }
        if (resampleCrs != null) {
            gridCoverage2D = (GridCoverage2D) Operations.DEFAULT.resample(gridCoverage2D, resampleCrs);
        }
        RenderedImage image = gridCoverage2D.getRenderedImage();
        return rendereImage2BufferedImage(image);
    }

    /**
     * Make a color of the image transparent.
     * 
     * @param bufferedImageToProcess the image to extract the color from.
     * @param colorToMakeTransparent the color to make transparent.
     * @return the new image.
     */
    public static BufferedImage makeColorTransparent( BufferedImage bufferedImageToProcess, final Color colorToMakeTransparent ) {
        ImageFilter filter = new RGBImageFilter(){
            public int markerRGB = colorToMakeTransparent.getRGB() | 0xFF000000;
            public final int filterRGB( int x, int y, int rgb ) {
                if ((rgb | 0xFF000000) == markerRGB) {
                    // Mark the alpha bits as zero - transparent
                    return 0x00FFFFFF & rgb;
                } else {
                    return rgb;
                }
            }
        };
        ImageProducer ip = new FilteredImageSource(bufferedImageToProcess.getSource(), filter);
        Image image = Toolkit.getDefaultToolkit().createImage(ip);
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bufferedImage.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return bufferedImage;
    }

    /**
     * Checks if an image is all of one color.
     * 
     * @param image the image to check.
     * @return <code>true</code> if the image is all of the same color.
     */
    public static boolean isAllOneColor( BufferedImage image ) {
        int width = image.getWidth();
        int height = image.getHeight();
        int previousRgb = 0;
        boolean isFirst = true;
        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                int rgb = image.getRGB(x, y);
                if (isFirst) {
                    isFirst = false;
                } else {
                    if (rgb != previousRgb) {
                        return false;
                    }
                }
                previousRgb = rgb;
            }
        }
        return true;
    }

    public static boolean areEqual( BufferedImage img1, BufferedImage img2 ) {
        if (img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight()) {
            for( int x = 0; x < img1.getWidth(); x++ ) {
                for( int y = 0; y < img1.getHeight(); y++ ) {
                    if (img1.getRGB(x, y) != img2.getRGB(x, y))
                        return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    public static BufferedImage rendereImage2BufferedImage( RenderedImage renderedImage ) {
        if (renderedImage instanceof BufferedImage) {
            return (BufferedImage) renderedImage;
        }
        ColorModel cm = renderedImage.getColorModel();
        int width = renderedImage.getWidth();
        int height = renderedImage.getHeight();
        WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        Hashtable<String, Object> properties = new Hashtable<>();
        String[] keys = renderedImage.getPropertyNames();
        if (keys != null) {
            for( int i = 0; i < keys.length; i++ ) {
                properties.put(keys[i], renderedImage.getProperty(keys[i]));
            }
        }
        BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
        renderedImage.copyData(raster);
        return result;
    }
}
