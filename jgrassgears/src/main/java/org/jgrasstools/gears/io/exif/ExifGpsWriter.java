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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

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

import com.sun.media.imageio.plugins.tiff.EXIFGPSTagSet;
import com.sun.media.imageio.plugins.tiff.EXIFParentTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.EXIFTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.TIFFDirectory;
import com.sun.media.imageio.plugins.tiff.TIFFField;
import com.sun.media.imageio.plugins.tiff.TIFFImageReadParam;
import com.sun.media.imageio.plugins.tiff.TIFFTag;
import com.sun.media.imageioimpl.plugins.tiff.TIFFIFD;

/**
 * Adapted code from http://code.google.com/p/gmoting/
 */
@Description("Utility class for writing exif tags in jpegs.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Jpeg, Exif, Reading")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class ExifGpsWriter extends JGTModel {
    @Description("The jpeg file.")
    @In
    public String file = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The latitude to add to the exif tags.")
    @Out
    public Double inLat = null;

    @Description("The longitude to add to the exif tags.")
    @Out
    public Double inLon = null;

    private ImageReader jpegReader;

    private ImageWriter jpegWriter;

    private BufferedImage image;

    private File imageFile;

    @Execute
    public void writeGpsExif() throws IOException {
        imageFile = new File(file);
        ImageInputStream is = ImageIO.createImageInputStream(imageFile);

        // Get core JPEG reader.
        Iterator readers = ImageIO.getImageReadersByFormatName("jpeg");

        while( readers.hasNext() ) {
            jpegReader = (ImageReader) readers.next();
            if (jpegReader.getClass().getName().startsWith("com.sun.imageio")) {
                // Break on finding the core provider.
                break;
            }
        }
        if (jpegReader == null) {
            System.out.println("Cannot find core JPEG reader!");
            System.exit(0);
        }

        // Get core JPEG writer.
        Iterator writers = ImageIO.getImageWritersByFormatName("jpeg");

        while( writers.hasNext() ) {
            jpegWriter = (ImageWriter) writers.next();
            if (jpegWriter.getClass().getName().startsWith("com.sun.imageio")) {
                // Break on finding the core provider.
                break;
            }
        }
        if (jpegWriter == null) {
            System.out.println("Cannot find core JPEG writer!");
            System.exit(0);
        }

        jpegReader.setInput(is);
        image = jpegReader.read(0);

        writeExif(null);
    }

    /**
     * Main method to write the gps data to the exif 
     * @param gps - gps position to be added
     */
    private void writeExif( GPSPosition gpos ) {

        IIOMetadata metadata = null;
        try {
            // get metadata object
            metadata = jpegReader.getImageMetadata(0);

            // names says which exif tree to get - 0 for jpeg 1 for the default
            String[] names = metadata.getMetadataFormatNames();
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(names[0]);

            // exif is on the app1 node called unknown
            NodeList nList = root.getElementsByTagName("unknown");
            IIOMetadataNode app1EXIFNode = (IIOMetadataNode) nList.item(0);
            ArrayList md = readExif(app1EXIFNode);
            IIOMetadata exifMetadata = (IIOMetadata) md.get(0);

            // insert the gps data into the exif
            exifMetadata = insertGPSCoords(gpos, exifMetadata);

            // create a new exif node
            IIOMetadataNode app1NodeNew = createNewExifNode(exifMetadata, null, null);

            // copy the user data accross
            app1EXIFNode.setUserObject(app1NodeNew.getUserObject());

            // write to a new image file
            FileImageOutputStream out1 = new FileImageOutputStream(new File("GPS_" + imageFile.getName()));
            jpegWriter.setOutput(out1);
            metadata.setFromTree(names[0], root);

            IIOImage image = new IIOImage(jpegReader.readAsRenderedImage(0, jpegReader.getDefaultReadParam()), null, metadata);

            // write out the new image
            jpegWriter.write(jpegReader.getStreamMetadata(), image, jpegWriter.getDefaultWriteParam());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Private method - Reads the exif metadata for an image
     * @param app1EXIFNode app1 Node of the image (where the exif data is stored)
     * @return the exif metadata
     */
    private ArrayList readExif( IIOMetadataNode app1EXIFNode ) {
        // Set up input skipping EXIF ID 6-byte sequence.
        byte[] app1Params = (byte[]) app1EXIFNode.getUserObject();

        MemoryCacheImageInputStream app1EXIFInput = new MemoryCacheImageInputStream(new ByteArrayInputStream(app1Params, 6,
                app1Params.length - 6));

        // only the tiff reader knows how to interpret the exif metadata
        ImageReader tiffReader = null;
        Iterator readers = ImageIO.getImageReadersByFormatName("tiff");

        while( readers.hasNext() ) {
            tiffReader = (ImageReader) readers.next();
            if (tiffReader.getClass().getName().startsWith("com.sun.media")) {
                // Break on finding the core provider.
                break;
            }
        }
        if (tiffReader == null) {
            System.out.println("Cannot find core TIFF reader!");
        }

        ArrayList out = new ArrayList(1);

        tiffReader.setInput(app1EXIFInput);

        IIOMetadata tiffMetadata = null;

        try {
            tiffMetadata = tiffReader.getImageMetadata(0);
            // IIOMetadata meta = tiffReader.getImageMetadata(0);
            TIFFImageReadParam rParam = (TIFFImageReadParam) tiffReader.getDefaultReadParam();
            rParam.setTIFFDecompressor(null);
        } catch (IOException e) {
            e.printStackTrace();
        };

        tiffReader.dispose();

        out.add(0, tiffMetadata);

        return out;
    }

    /**
     * Private method - creates a copy of the metadata that can be written to
     * @param tiffMetadata - in metadata
     * @return new metadata node that can be written to
     */
    private IIOMetadataNode createNewExifNode( IIOMetadata tiffMetadata, IIOMetadata thumbMeta, BufferedImage thumbnail ) {

        IIOMetadataNode app1Node = null;
        ImageWriter tiffWriter = null;
        try {
            Iterator writers = ImageIO.getImageWritersByFormatName("tiff");
            while( writers.hasNext() ) {
                tiffWriter = (ImageWriter) writers.next();
                if (tiffWriter.getClass().getName().startsWith("com.sun.media")) {
                    // Break on finding the core provider.
                    break;
                }
            }
            if (tiffWriter == null) {
                System.out.println("Cannot find core TIFF writer!");
                System.exit(0);
            }

            ImageWriteParam writeParam = tiffWriter.getDefaultWriteParam();
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionType("EXIF JPEG");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MemoryCacheImageOutputStream app1EXIFOutput = new MemoryCacheImageOutputStream(baos);
            tiffWriter.setOutput(app1EXIFOutput);

            // escribir
            tiffWriter.prepareWriteEmpty(jpegReader.getStreamMetadata(), new ImageTypeSpecifier(image), image.getWidth(),
                    image.getHeight(), tiffMetadata, null, writeParam);

            tiffWriter.endWriteEmpty();

            // Flush data into byte stream.
            app1EXIFOutput.flush();

            // Create APP1 parameter array.
            byte[] app1Parameters = new byte[6 + baos.size()];

            // Add EXIF APP1 ID bytes.
            app1Parameters[0] = (byte) 'E';
            app1Parameters[1] = (byte) 'x';
            app1Parameters[2] = (byte) 'i';
            app1Parameters[3] = (byte) 'f';
            app1Parameters[4] = app1Parameters[5] = (byte) 0;

            // Append TIFF stream to APP1 parameters.
            System.arraycopy(baos.toByteArray(), 0, app1Parameters, 6, baos.size());

            // Create the APP1 EXIF node to be added to native JPEG image metadata.
            app1Node = new IIOMetadataNode("unknown");
            app1Node.setAttribute("MarkerTag", (new Integer(0xE1)).toString());
            app1Node.setUserObject(app1Parameters);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (tiffWriter != null)
                tiffWriter.dispose();
        }

        return app1Node;

    }

    /**
     * Private method - adds gps information to the exif data
     * @param pos a GPSPosition object containing the information to encode
     * @param exif the exif metadata to add the position to
     * 
     */
    private IIOMetadata insertGPSCoords( GPSPosition pos, IIOMetadata exif ) {

        IIOMetadata outExif = null;
        try {
            TIFFDirectory ifd = TIFFDirectory.createFromMetadata(exif);
            TIFFField gpsInfoPointer = null;

            // first get the pointer from the directory if it's not there create a new one
            if (ifd.containsTIFFField(EXIFParentTIFFTagSet.TAG_GPS_INFO_IFD_POINTER)) {
                gpsInfoPointer = ifd.getTIFFField(EXIFParentTIFFTagSet.TAG_GPS_INFO_IFD_POINTER);
                System.out.println("Already has GPS Metadata");
                return exif;
            } else {
                // this assumes that the EXIFParentTIFFTagSet is allowed on the tiff image reader

                // first construct the directory to hold the GPS data
                TIFFDirectory gpsData = pos.createDirectory();

                // Create the pointer with the data
                EXIFParentTIFFTagSet parentSet = EXIFParentTIFFTagSet.getInstance();
                gpsInfoPointer = new TIFFField(parentSet.getTag(EXIFParentTIFFTagSet.TAG_GPS_INFO_IFD_POINTER),
                        TIFFTag.TIFF_LONG, 1, gpsData);
                System.out.println("is pointer =" + gpsInfoPointer.getTag().isIFDPointer() + " data type is ok="
                        + gpsInfoPointer.getTag().isDataTypeOK(TIFFTag.TIFF_LONG));
            }
            ifd.addTIFFField(gpsInfoPointer);
            outExif = ifd.getAsMetadata();

        } catch (IIOInvalidTreeException e) {
            e.printStackTrace();
        }

        return outExif;

    }

}
