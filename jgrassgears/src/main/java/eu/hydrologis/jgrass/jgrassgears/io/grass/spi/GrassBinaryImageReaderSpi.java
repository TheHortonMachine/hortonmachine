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
package eu.hydrologis.jgrass.jgrassgears.io.grass.spi;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import eu.hydrologis.jgrass.jgrassgears.io.grass.GrassBinaryImageReader;
import eu.hydrologis.jgrass.jgrassgears.io.grass.GrassBinaryImageWriter;

/**
 * The Service Provider Interface for GRASS binary rasters.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 3.0
 * @see GrassBinaryImageReader
 * @see GrassBinaryImageWriter
 * @see GrassBinaryImageReaderSpi
 */
@SuppressWarnings("nls")
public class GrassBinaryImageReaderSpi extends ImageReaderSpi {

    private static final String vendorName = "www.hydrologis.com";
    private static final String[] suffixes = {""};
    private static final String[] formatNames = {"grass", "GRASS", "grassbin",
            "GRASS binary raster"};
    private static final String[] MIMETypes = {"image/grass"};
    private static final String version = "1.0";

    /**
     * the class name of the image reader.
     */
    private static final String readerCN = "eu.hydrologis.jgrass.grassbinary.imageio.io.GrassBinaryImageReader";

    /**
     * the inputTypes that are accepted by the {@link GrassBinaryImageReader}.
     */
    private static final Class< ? >[] inputTypes = new Class[]{File.class, ImageInputStream.class};

    /**
     * the writerSpiName
     */
    private static final String[] wSN = {"eu.hydrologis.jgrass.grassbinary.imageio.io.GrassBinaryImageWriterSpi"};

    /**
     * the flag for stream metadata support.
     */
    private static final boolean supportsStandardStreamMetadataFormat = false;

    private static final String nativeStreamMetadataFormatName = null;
    private static final String nativeStreamMetadataFormatClassName = null;
    private static final String[] extraStreamMetadataFormatNames = null;
    private static final String[] extraStreamMetadataFormatClassNames = null;

    /**
     * the flag for image metadata support.
     */
    private static final boolean supportsStandardImageMetadataFormat = true;

    private static final String nativeImageMetadataFormatName = "eu.hydrologis.jgrass.grassbinary.imageio.metadata.GrassBinaryImageMetadata";
    private static final String nativeImageMetadataFormatClassName = "eu.hydrologis.jgrass.grassbinary.imageio.metadata.GrassBinaryImageMetadataFormat";
    private static final String[] extraImageMetadataFormatNames = {null};
    private static final String[] extraImageMetadataFormatClassNames = {null};

    /**
     * default constructor for the service provider interface.
     */
    public GrassBinaryImageReaderSpi() {
        super(vendorName, version, formatNames, suffixes, MIMETypes, readerCN, inputTypes, wSN,
                supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName,
                nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames,
                extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat,
                nativeImageMetadataFormatName, nativeImageMetadataFormatClassName,
                extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
    }

    public String getDescription( Locale locale ) {
        return "GRASS binary raster image reader service provider interface, version " + version;
    }

    public boolean canDecodeInput( Object source ) throws IOException {
        if (source instanceof File) {
            return true;
        }
        return false;
    }

    public GrassBinaryImageReader createReaderInstance( Object extension ) throws IOException {
        return new GrassBinaryImageReader(this);
    }

}
