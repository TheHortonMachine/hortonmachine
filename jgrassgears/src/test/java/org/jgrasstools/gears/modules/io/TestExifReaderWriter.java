package org.jgrasstools.gears.modules.io;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrasstools.gears.io.exif.ExifGpsWriter;
import org.jgrasstools.gears.io.exif.ExifReader;
import org.jgrasstools.gears.io.exif.ExifTag;
import org.jgrasstools.gears.io.exif.ExifWriter;
import org.jgrasstools.gears.utils.HMTestCase;
/**
 * Test ExifReader.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestExifReaderWriter extends HMTestCase {

    @SuppressWarnings("nls")
    public void testExifReader() throws Exception {
        // URL testUrl = this.getClass().getClassLoader().getResource("dtm_test.asc");
        // String path = new File(testUrl.toURI()).getAbsolutePath();

        Class< ? > classs = Class.forName("com.sun.media.imageioimpl.plugins.tiff.TIFFImageMetadata");
        
        File img = new File("/home/moovida/Desktop/android_sdcard/geopaparazzi/pictures/IMG_20100619_112532.jpg");
        // File img = new File("/home/moovida/Desktop/rilievo_drava/DCIM/100CANON/IMG_3801.JPG");

        dumpExifData(img);

        ExifGpsWriter writer = new ExifGpsWriter();
        writer.doNorth = true;
        writer.doEast = true;
        writer.pAltitude = 350.0;
        writer.pLat = 46.5;
        writer.pLon = 11.05;
        writer.tTimestamp = "2010-09-12 00:01:00";
        writer.file = img.getAbsolutePath();

        writer.writeGpsExif();

        dumpExifData(img);

    }

    private void dumpExifData( File img ) throws IOException {
        ExifReader exifReader = new ExifReader();
        exifReader.file = img.getAbsolutePath();
        exifReader.readExif();

        HashMap<String, ExifTag> exifTags = exifReader.outTags;

        Set<Entry<String, ExifTag>> entrySet = exifTags.entrySet();
        for( Entry<String, ExifTag> entry : entrySet ) {
            System.out.println(entry.getValue().toString());
        }
    }
}
