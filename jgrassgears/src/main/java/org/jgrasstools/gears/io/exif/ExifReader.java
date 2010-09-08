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
package org.jgrasstools.gears.io.exif;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Role;
import oms3.annotations.Status;

import org.jgrasstools.gears.libs.exceptions.ModelsIOException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.w3c.dom.NodeList;

import com.sun.media.imageio.plugins.tiff.EXIFParentTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.EXIFTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.TIFFDirectory;
import com.sun.media.imageio.plugins.tiff.TIFFField;

@Description("Utility class for reading exif tags in jpegs.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Jpeg, Exif, Reading")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class ExifReader extends JGTModel {
    @Role(Role.PARAMETER)
    @Description("The jpeg file.")
    @In
    public String file = null;

    @Role(Role.PARAMETER)
    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Role(Role.PARAMETER)
    @Description("The read exif tags.")
    @Out
    public HashMap<String, ExifTag> outTags = null;

    @Execute
    public void readExif() throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("jpeg");
        ImageReader reader = null;
        while( readers.hasNext() ) {
            reader = (ImageReader) readers.next();
            if (reader.getClass().getName().startsWith("com.sun.media")) {
                // Break on finding the provider.
                break;
            }
        }
        if (reader == null) {
            throw new ModelsIOException("Cannot find reader!", this);
        }

        reader.setInput(new FileImageInputStream(new File(file)));
        IIOMetadata imageMetadata = reader.getImageMetadata(0);

        parseExifMeta(imageMetadata);

    }

    @SuppressWarnings("nls")
    private void parseExifMeta( IIOMetadata exifMeta ) {
        // Specification of "com_sun_media_imageio_plugins_tiff_image_1.0"
        // http://download.java.net/media/jai-imageio/javadoc/1.1/com/sun/media/imageio/plugins/tiff/package-summary.html

        // tags.addColumn("Tag #");
        // tags.addColumn("Name");
        // tags.addColumn("Value(s)");

        outTags = new HashMap<String, ExifTag>();

        IIOMetadataNode root = (IIOMetadataNode) exifMeta.getAsTree("com_sun_media_imageio_plugins_tiff_image_1.0");

        NodeList imageDirectories = root.getElementsByTagName("TIFFIFD");
        for( int i = 0; i < imageDirectories.getLength(); i++ ) {
            IIOMetadataNode directory = (IIOMetadataNode) imageDirectories.item(i);

            NodeList tiffTags = directory.getElementsByTagName("TIFFField");
            for( int j = 0; j < tiffTags.getLength(); j++ ) {
                IIOMetadataNode tag = (IIOMetadataNode) tiffTags.item(j);

                String tagNumber = tag.getAttribute("number");
                String tagName = tag.getAttribute("name");
                String tagValue;

                StringBuilder tmp = new StringBuilder();
                IIOMetadataNode values = (IIOMetadataNode) tag.getFirstChild();

                if ("TIFFUndefined".equals(values.getNodeName())) {
                    tmp.append(values.getAttribute("value"));
                } else {
                    NodeList tiffNumbers = values.getChildNodes();
                    for( int k = 0; k < tiffNumbers.getLength(); k++ ) {
                        tmp.append(((IIOMetadataNode) tiffNumbers.item(k)).getAttribute("value"));
                        tmp.append(",");
                    }
                    tmp.deleteCharAt(tmp.length() - 1);
                }

                tagValue = tmp.toString();

                ExifTag exifTag = new ExifTag(tagName, tagNumber, tagValue);
                outTags.put(tagName, exifTag);
            }
        }
    }
    /**Returns the EXIF information from the given metadata if present.  The
     * metadata is assumed to be in <pre>javax_imageio_jpeg_image_1.0</pre> format.
     * If EXIF information was not present then null is returned.*/
    private byte[] getEXIF( IIOMetadata meta ) {
        // http://java.sun.com/javase/6/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html

        // javax_imageio_jpeg_image_1.0
        // -->markerSequence
        // ---->unknown (attribute: "MarkerTag" val: 225 (for exif))

        IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree("JPEGMetaFormat");

        IIOMetadataNode markerSeq = (IIOMetadataNode) root.getElementsByTagName("markerSequence").item(0);

        NodeList unkowns = markerSeq.getElementsByTagName("unknown");
        for( int i = 0; i < unkowns.getLength(); i++ ) {
            IIOMetadataNode marker = (IIOMetadataNode) unkowns.item(i);
            if ("225".equals(marker.getAttribute("MarkerTag"))) {
                return (byte[]) marker.getUserObject();
            }
        }
        return null;
    }

    /**Uses a TIFFImageReader plugin to parse the given exif data into tiff
     * tags.  The returned IIOMetadata is in whatever format the tiff ImageIO
     * plugin uses.  If there is no tiff plugin, then this method returns null.*/
    private IIOMetadata getTiffMetaFromEXIF( byte[] exif ) {
        java.util.Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("tif");

        ImageReader reader;
        if (!readers.hasNext()) {
            return null;
        } else {
            reader = readers.next();
        }

        // skip the 6 byte exif header
        ImageInputStream wrapper = new MemoryCacheImageInputStream(new java.io.ByteArrayInputStream(exif, 6, exif.length - 6));
        reader.setInput(wrapper, true, false);

        IIOMetadata exifMeta;
        try {
            exifMeta = reader.getImageMetadata(0);
        } catch (Exception e) {
            // shouldn't happen
            throw new Error(e);
        }

        reader.dispose();
        return exifMeta;
    }

}
