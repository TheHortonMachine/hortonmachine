/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.gears.io.exif;

import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;

/**
 * Exif related utilities.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ExifUtil {
    /**
     * Find jpeg {@link ImageReader reader}.
     * 
     * @return the reader or <code>null</code>.
     */
    @SuppressWarnings("nls")
    public static ImageReader findReader() {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("jpeg");
        ImageReader reader = null;
        while( readers.hasNext() ) {
            reader = (ImageReader) readers.next();
            if (reader.getClass().getName().startsWith("com.sun.media")) { // com.sun.imageio?
                break;
            }
        }
        return reader;
    }

    /**
     * Find jpeg {@link ImageWriter writer}.
     * 
     * @return the writer or <code>null</code>.
     */
    @SuppressWarnings("nls")
    public static ImageWriter findWriter() {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter writer = null;
        while( writers.hasNext() ) {
            writer = (ImageWriter) writers.next();
            if (writer.getClass().getName().startsWith("com.sun.imageio")) {
                break;
            }
        }
        return writer;
    }
}
