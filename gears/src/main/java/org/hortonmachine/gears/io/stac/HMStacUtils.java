package org.hortonmachine.gears.io.stac;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;

public class HMStacUtils {

    static DateFormat dateFormatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
    static DateFormat fileNameDateFormatter = new SimpleDateFormat("yyyyMMdd_HHmmss");

    
    public static String simplify( Object obj ) {
        if (obj instanceof ReferencedEnvelope) {
            ReferencedEnvelope refEnv = (ReferencedEnvelope) obj;
            Envelope env = new Envelope(refEnv);
            return env.toString() + " - " + refEnv.getCoordinateReferenceSystem().getName();
        }
        return null;
    }
}
