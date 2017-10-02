package org.hortonmachine.gears.modules;
//package org.hortonmachine.gears.modules;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map.Entry;
//import java.util.Set;
//
//import org.hortonmachine.gears.io.exif.ExifGpsWriter;
//import org.hortonmachine.gears.io.exif.ExifReader;
//import org.hortonmachine.gears.io.exif.ExifTag;
//import org.hortonmachine.gears.utils.HMTestCase;
///**
// * Test ExifReader.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestExifReaderWriter extends HMTestCase {
//
//    @SuppressWarnings("nls")
//    public void testExifReader() throws Exception {
//        
//        File img = new File("/home/moovida/Desktop/android_sdcard/geopaparazzi/pictures/IMG_20100619_112532.jpg");
//
//        dumpExifData(img);
//
//        ExifGpsWriter writer = new ExifGpsWriter();
//        writer.doNorth = true;
//        writer.doEast = true;
//        writer.pAltitude = 350.0;
//        writer.pLat = 46.5;
//        writer.pLon = 11.05;
//        writer.tTimestamp = "2010-09-12 00:01:00";
//        writer.file = img.getAbsolutePath();
//
//        writer.writeGpsExif();
//
//        dumpExifData(img);
//
//    }
//
//    private void dumpExifData( File img ) throws IOException {
//        ExifReader exifReader = new ExifReader();
//        exifReader.file = img.getAbsolutePath();
//        exifReader.readExif();
//
//        HashMap<String, ExifTag> exifTags = exifReader.outTags;
//
//        Set<Entry<String, ExifTag>> entrySet = exifTags.entrySet();
//        for( Entry<String, ExifTag> entry : entrySet ) {
//            System.out.println(entry.getValue().toString());
//        }
//    }
//}
