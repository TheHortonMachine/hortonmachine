package org.hortonmachine.gears.io.stac;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;

public class HMStacUtils {

    static Integer NO_EPSG_DEFINED = -1;
    static DateFormat dateFormatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
    static DateFormat filterTimestampFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
        TimeZone utcTz = TimeZone.getTimeZone("UTC");
        filterTimestampFormatter.setTimeZone(utcTz);
        dateFormatter.setTimeZone(utcTz);
    }
    
    public static String simplify( Object obj ) {
        if (obj instanceof ReferencedEnvelope) {
            ReferencedEnvelope refEnv = (ReferencedEnvelope) obj;
            Envelope env = new Envelope(refEnv);
            return env.toString() + " - " + refEnv.getCoordinateReferenceSystem().getName();
        }
        return null;
    }

    final static List<String> ACCEPTED_TYPES = Arrays.asList("image/tiff;application=geotiff", "image/vnd.stac.geotiff",
            "image/tiff;application=geotiff;profile=cloud-optimized", "image/vnd.stac.geotiff;profile=cloud-optimized", "image/vnd.stac.geotiff;cloud-optimized=true",
            "application/geo+json");

    final static List<String> ACCEPTED_EXTENSIONS = Arrays.asList("tif", "tiff", "gtiff");
}
