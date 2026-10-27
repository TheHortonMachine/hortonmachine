package org.hortonmachine.gears.io.stac;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

public class HMStacUtils {

    static Integer NO_EPSG_DEFINED = -1;
    static DateFormat dateFormatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
    // with the milliseconds, else the end of a time filter falls before the items of its last second
    static DateFormat filterTimestampFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
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

    /**
     * Check if WGS84 bounds look written in latitude/longitude order instead of the
     * longitude/latitude order required by STAC (GeoJSON): x is within +-90 and y goes beyond it.
     *
     * <p>Only bounds reaching latitudes over +-90 can be detected, as global layers.</p>
     *
     * @param env the bounds to check.
     * @return <code>true</code> if the axes look swapped.
     */
    public static boolean looksLatLon( Envelope env ) {
        if (env == null || env.isNull())
            return false;
        return env.getMinX() >= -90.0001 && env.getMaxX() <= 90.0001 && (env.getMinY() < -90.0001 || env.getMaxY() > 90.0001);
    }

    /**
     * @param geometry the geometry to swap.
     * @return a copy of the geometry with x and y swapped.
     */
    public static Geometry swapXY( Geometry geometry ) {
        Geometry swapped = geometry.copy();
        swapped.apply(new CoordinateSequenceFilter(){
            @Override
            public void filter( CoordinateSequence seq, int i ) {
                double x = seq.getOrdinate(i, 0);
                seq.setOrdinate(i, 0, seq.getOrdinate(i, 1));
                seq.setOrdinate(i, 1, x);
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public boolean isGeometryChanged() {
                return true;
            }
        });
        swapped.geometryChanged();
        return swapped;
    }

    /**
     * Swap the default geometry of an item to longitude/latitude order if it is written in latitude/longitude.
     *
     * @param feature the item feature.
     * @param force if <code>true</code> the geometry is swapped even if it can't be detected
     *          as latitude/longitude, for catalogs known to use that order.
     * @return <code>true</code> if the geometry has been swapped.
     */
    public static boolean fixLatLonGeometry( SimpleFeature feature, boolean force ) {
        if (!(feature.getDefaultGeometry() instanceof Geometry geometry))
            return false;
        if (force || looksLatLon(geometry.getEnvelopeInternal())) {
            feature.setDefaultGeometry(swapXY(geometry));
            return true;
        }
        return false;
    }

    final static List<String> ACCEPTED_TYPES = Arrays.asList("image/tiff;application=geotiff", "image/vnd.stac.geotiff",
            "image/tiff;application=geotiff;profile=cloud-optimized", "image/vnd.stac.geotiff;profile=cloud-optimized", "image/vnd.stac.geotiff;cloud-optimized=true",
            "application/geo+json");

    final static List<String> ACCEPTED_EXTENSIONS = Arrays.asList("tif", "tiff", "gtiff");
}
