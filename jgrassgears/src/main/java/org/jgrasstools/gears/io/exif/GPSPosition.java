package org.jgrasstools.gears.io.exif;
import java.util.ArrayList;

import com.sun.media.imageio.plugins.tiff.EXIFGPSTagSet;
import com.sun.media.imageio.plugins.tiff.EXIFParentTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.TIFFDirectory;
import com.sun.media.imageio.plugins.tiff.TIFFField;
import com.sun.media.imageio.plugins.tiff.TIFFTag;
import com.sun.media.imageioimpl.plugins.tiff.TIFFIFD;

/*
 * Created on 21-May-2007
 *
 * @author Alistair Edwardes
 *
 * Department of Geography
 * University of Zurich - Irchel
 * Winterthurerstr. 190 
 * CH-8057 Zurich, Switzerland
 * aje@geo.unizh.ch	
 */
@SuppressWarnings("nls")
public class GPSPosition {

    private String[] latRef = {"", ""};
    private String[] longRef = {"", ""};
    private byte[] altRef = new byte[1];
    private long[][] latitude;
    private long[][] longitude;
    private long[][] altitude;
    private long[][] timeStamp;
    private String[] dateStamp = new String[11];
    private String[] status = {"", ""};
    private String[] dop = {"0"};
    private String[] imgDirectionRef = {"", ""};
    private long[][] imgDirection;
    private String[] datum = {"W", "G", "S", "-", "8", "4", ""};
    private boolean isRMC;

    // only understands GGA or RMC sentences - assumes the header e.g. $GP has
    // been stripped off
    public GPSPosition( String nmeaSentence ) {

        if (nmeaSentence.startsWith("$GP"))
            nmeaSentence = nmeaSentence.substring(3);

        isRMC = nmeaSentence.startsWith("RMC");

        if (isRMC)
            parseRMC(nmeaSentence);
        else
            parseGGA(nmeaSentence);
    }

    // populate fields not covered by RMC with those of GGA and vice-versa
    public void updateFields( String nmea ) {
        // assumes you won't give the same string type twice

        String[] words = nmea.split(",");
        if (isRMC) {
            if (words[6] == "0")
                dop[0] = "999";
            else
                dop[0] = words[8];
            float alt = Float.parseFloat(words[9]) * 10;
            altitude = new long[][]{{(long) alt, 10}};
            // this might not be strict correct - probably need to compare
            // altitude with geoid words[10]
            altRef[0] = EXIFGPSTagSet.ALTITUDE_REF_SEA_LEVEL;
        } else {
            if (words[2] == "A")
                status[0] = EXIFGPSTagSet.STATUS_MEASUREMENT_IN_PROGRESS;
            else
                status[0] = EXIFGPSTagSet.STATUS_MEASUREMENT_INTEROPERABILITY;
            dateStamp = getDate(words[9]);
        }
    }

    
    private void parseRMC( String nmea ) {

        // $GPRMC,075704,A,4723.8391,N,00832.8758,E,002.9,128.7,090207,,,A*70

        String[] words = nmea.split(",");
        // first in time stamp
        timeStamp = getTime(words[1]);
        // then status
        if (words[2] == "A")
            status[0] = EXIFGPSTagSet.STATUS_MEASUREMENT_IN_PROGRESS;
        else
            status[0] = EXIFGPSTagSet.STATUS_MEASUREMENT_INTEROPERABILITY;
        // get latitude
        latitude = getLatitude(words[3]);
        latRef[0] = words[4] == "N" ? EXIFGPSTagSet.LATITUDE_REF_NORTH : EXIFGPSTagSet.LATITUDE_REF_SOUTH;
        longitude = getLongitude(words[5]);
        longRef[0] = words[6] == "E" ? EXIFGPSTagSet.LONGITUDE_REF_EAST : EXIFGPSTagSet.LONGITUDE_REF_WEST;
        dateStamp = getDate(words[9]);
    }

    private void parseGGA( String nmea ) {

        // $GPGGA,075719,4723.8344,N,00832.8877,E,1,07,01.5,00486.9,M,048.0,M,,*43

        String[] words = nmea.split(",");
        // first in time stamp
        timeStamp = getTime(words[1]);

        // get latitude
        latitude = getLatitude(words[2]);
        latRef[0] = words[3] == "N" ? EXIFGPSTagSet.LATITUDE_REF_NORTH : EXIFGPSTagSet.LATITUDE_REF_SOUTH;
        longitude = getLongitude(words[4]);
        longRef[0] = words[5] == "E" ? EXIFGPSTagSet.LONGITUDE_REF_EAST : EXIFGPSTagSet.LONGITUDE_REF_WEST;
        if (words[6] == "0")
            dop[0] = "999";
        else
            dop[0] = words[8];
        float alt = Float.parseFloat(words[9]) * 10;
        altitude = new long[][]{{(long) alt, 10}};
        // this might not be strict correct - probably need to compare altitude
        // with geoid words[10]
        altRef[0] = EXIFGPSTagSet.ALTITUDE_REF_SEA_LEVEL;
    }

    private long[][] getTime( String time ) {

        long[][] timel = new long[][]{{Long.parseLong(time.substring(0, 2)), 1}, {Long.parseLong(time.substring(2, 4)), 1},
                {Long.parseLong(time.substring(4)), 1}};

        return timel;
    }

    // format is YYYY:MM:DD ?
    private String[] getDate( String date ) {

        String dateStr = "20" + date.substring(4) + ":" + date.substring(2, 4) + ":" + date.substring(0, 2);

        String[] dateArray = new String[11];

        for( int i = 0; i < dateStr.length(); i++ )
            dateArray[i] = dateStr.substring(i, i + 1);
        dateArray[10] = "";

        return dateArray;
    }

    // assumes the the format is HHMM.MMMM
    private long[][] getLatitude( String lat ) {

        float secs = Float.parseFloat("0" + lat.substring(4)) * 60.f;
        long nom = (long) (secs * 1000);

        long[][] latl = new long[][]{{Long.parseLong(lat.substring(0, 2)), 1}, {Long.parseLong(lat.substring(2, 4)), 1},
                {nom, 1000}};

        return latl;
    }

    // assumes the the format is HHHMM.MMMM
    private long[][] getLongitude( String longi ) {

        float secs = Float.parseFloat("0" + longi.substring(5)) * 60.f;
        long nom = (long) (secs * 1000);

        long[][] longl = new long[][]{{Long.parseLong(longi.substring(0, 3)), 1}, {Long.parseLong(longi.substring(3, 5)), 1},
                {nom, 1000}};

        return longl;
    }

    public boolean isGood() {
        /*
         * Based on the dop value From wikipedia
         * 
         * 1 Ideal This is the highest possible confidence level to be used for
         * applications demanding the highest possible precision at all times
         * 2-3 Excellent At this confidence level, positional measurements are
         * considered accurate enough to meet all but the most sensitive
         * applications 4-6 Good Represents a level that marks the minimum
         * appropriate for making business decisions. Positional measurements
         * could be used to make reliable in-route navigation suggestions to the
         * user 7-8 Moderate Positional measurements could be used for
         * calculations, but the fix quality could still be improved. A more
         * open view of the sky is recommended 9-20 Fair Represents a low
         * confidence level. Positional measurements should be discarded or used
         * only to indicate a very rough estimate of the current location 21-50
         * Poor At this level, measurements are inaccurate by as much as half a
         * football field and should be discarded
         */
        return (Float.parseFloat(dop[0]) < 15.f);
    }

    public TIFFDirectory createDirectory() {

        EXIFGPSTagSet gpsTags = EXIFGPSTagSet.getInstance();

        ArrayList tags = new ArrayList();
        tags.add(gpsTags);
        TIFFDirectory directory = new TIFFIFD(tags, EXIFParentTIFFTagSet.getInstance().getTag(
                EXIFParentTIFFTagSet.TAG_GPS_INFO_IFD_POINTER));
        // TIFFDirectory directory = new TIFFDirectory(new
        // TIFFTagSet[]{gpsTags},EXIFParentTIFFTagSet.getInstance().getTag(EXIFParentTIFFTagSet.TAG_GPS_INFO_IFD_POINTER));

        // create the new fields

        // version field
        TIFFField field = new TIFFField(gpsTags.getTag(EXIFGPSTagSet.TAG_GPS_VERSION_ID), TIFFTag.TIFF_BYTE, 4,
                EXIFGPSTagSet.GPS_VERSION_2_2);
        directory.addTIFFField(field);
        // lat reference
        field = new TIFFField(gpsTags.getTag(EXIFGPSTagSet.TAG_GPS_LATITUDE_REF), TIFFTag.TIFF_ASCII, 2, latRef);
        directory.addTIFFField(field);
        // latitude
        field = new TIFFField(gpsTags.getTag(EXIFGPSTagSet.TAG_GPS_LATITUDE), TIFFTag.TIFF_RATIONAL, 3, latitude);
        directory.addTIFFField(field);
        // long reference
        field = new TIFFField(gpsTags.getTag(EXIFGPSTagSet.TAG_GPS_LONGITUDE_REF), TIFFTag.TIFF_ASCII, 2, longRef);
        directory.addTIFFField(field);
        // longitude
        field = new TIFFField(gpsTags.getTag(EXIFGPSTagSet.TAG_GPS_LONGITUDE), TIFFTag.TIFF_RATIONAL, 3, longitude);
        directory.addTIFFField(field);
        // time stamp
        field = new TIFFField(gpsTags.getTag(EXIFGPSTagSet.TAG_GPS_TIME_STAMP), TIFFTag.TIFF_RATIONAL, 3, timeStamp);
        directory.addTIFFField(field);
        // status
        field = new TIFFField(gpsTags.getTag(EXIFGPSTagSet.TAG_GPS_STATUS), TIFFTag.TIFF_ASCII, 2, status);
        directory.addTIFFField(field);
        // date stamp
        field = new TIFFField(gpsTags.getTag(EXIFGPSTagSet.TAG_GPS_DATE_STAMP), TIFFTag.TIFF_ASCII, 11, dateStamp);
        directory.addTIFFField(field);
        // datum
        field = new TIFFField(gpsTags.getTag(EXIFGPSTagSet.TAG_GPS_MAP_DATUM), TIFFTag.TIFF_ASCII, 6, datum);
        directory.addTIFFField(field);
        // altitude reference
        field = new TIFFField(gpsTags.getTag(EXIFGPSTagSet.TAG_GPS_ALTITUDE_REF), TIFFTag.TIFF_BYTE, 1, altRef);
        directory.addTIFFField(field);
        field = new TIFFField(gpsTags.getTag(EXIFGPSTagSet.TAG_GPS_ALTITUDE), TIFFTag.TIFF_RATIONAL, 1, altitude);
        directory.addTIFFField(field);
        // add the direction
        imgDirectionRef[0] = EXIFGPSTagSet.DIRECTION_REF_TRUE;
        field = new TIFFField(gpsTags.getTag(EXIFGPSTagSet.TAG_GPS_IMG_DIRECTION_REF), TIFFTag.TIFF_ASCII, 2, imgDirectionRef);
        directory.addTIFFField(field);
        if (imgDirection == null)
            imgDirection = new long[][]{{0, 100}};
        field = new TIFFField(gpsTags.getTag(EXIFGPSTagSet.TAG_GPS_IMG_DIRECTION), TIFFTag.TIFF_RATIONAL, 1, imgDirection);
        directory.addTIFFField(field);

        return directory;
    }

    public boolean equals( GPSPosition b ) {
        return (this.timeStamp[0][0] == b.timeStamp[0][0] && this.timeStamp[0][1] == b.timeStamp[0][1] && this.timeStamp[0][2] == b.timeStamp[0][2]);
    }

    // this is going to need an interpolate function gpos-gpos->gpos

    /**
     * @param args
     */
    public static void main( String[] args ) {

        String nmea = "$GPGGA,075704,4723.8391,N,00832.8758,E,1,07,01.5,00486.6,M,048.0,M,,*4A";
        GPSPosition gpos = new GPSPosition(nmea);
        nmea = "$GPRMC,075704,A,4723.8391,N,00832.8758,E,002.9,128.7,090207,,,A*70";
        gpos.updateFields(nmea);
        TIFFDirectory dir = gpos.createDirectory();

        System.out.println();
    }

}
