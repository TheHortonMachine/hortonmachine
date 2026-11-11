package org.hortonmachine.gears.utils.crs;

import java.util.HashMap;
import java.util.Map;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.hortonmachine.dbs.utils.CrsId;
import static org.hortonmachine.gears.utils.crs.CustomCrsHandler.*;

public enum HMCrsRegistry {
	INSTANCE;

	/** If the longitude first axis order was set, see {@link #initLongitudeFirst()}. */
	private static boolean longitudeFirstSet = false;

	static {
		initLongitudeFirst();
	}

	/**
	 * The hints that make the GeoTools readers decode the CRS of their data with the longitude
	 * first, as <code>format.getReader(file, HMCrsRegistry.longitudeFirstHints())</code>,
	 * independently of the global order of {@link #initLongitudeFirst()}.
	 *
	 * @return new hints, which the caller can change.
	 */
	public static Hints longitudeFirstHints() {
		return new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
	}

	/**
	 * Make GeoTools put the longitude (easting) first in the CRSs it decodes without an explicit
	 * axis order, as EPSG:4326, which HortonMachine assumes everywhere: x is the longitude.
	 *
	 * <p>The applications, the server, the command line and the tests call it first thing, since
	 * the system property alone is read only when the GeoTools factories are created: set later,
	 * as by a class loaded after some CRS was decoded, it was ignored. The system default hint set
	 * here also reaches the factories already created. Calling it again does nothing.</p>
	 */
	public static synchronized void initLongitudeFirst() {
		if (longitudeFirstSet) {
			return;
		}
		System.setProperty("org.geotools.referencing.forceXY", "true");
		Hints.putSystemDefault(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
		longitudeFirstSet = true;
	}

	boolean initialized = false;

	private HMCrsRegistry() {
	}


	private final Map<String, CoordinateReferenceSystem> addedCrsObjects = new HashMap<>();

	public boolean hasCrs(String epsgCode) {
		init();
		return addedCrsWkts.containsKey(epsgCode);
	}

	public String getCrsWkt(String epsgCode) {
		init();
		return addedCrsWkts.get(epsgCode);
	}

	public void addCrs(String epsgCode, String wkt) {
		if (initialized) {
			throw new IllegalStateException("Cannot add CRS after initialization");
		}
		addedCrsWkts.put(epsgCode, wkt);
	}

	public void init() {
		if (!initialized) {
			addedCrsWkts.forEach((epsg, wkt) -> {
				try {
					CoordinateReferenceSystem crs = CRS.parseWKT(wkt);
					addedCrsObjects.put(epsg, crs);
				} catch (Exception e) {
					throw new RuntimeException("Failed to parse CRS with EPSG code " + epsg, e);
				}
			});
		}
		initialized = true;
	}

	
	/**
	 * Get a {@link CoordinateReferenceSystem} from a code string with the longitude (easting)
	 * first, the axis order HortonMachine works with, as
	 * {@link #getCrs(String, boolean) getCrs(code, true)}.
	 *
	 * @param crsCode authority-qualified code (e.g. {@code "EPSG:4326"}, {@code "ESRI:102700"}) or a
	 *                bare integer treated as EPSG (e.g. {@code "4326"}).
	 * @see #getCrs(String, boolean)
	 */
	public CoordinateReferenceSystem getCrs( String crsCode ) throws FactoryException {
		return getCrs(crsCode, true);
	}

	/**
	 * Get a {@link CoordinateReferenceSystem} from a code string with explicit axis order control.
	 *
	 * <p>Accepts authority-qualified strings such as {@code "EPSG:4326"} or {@code "ESRI:102700"},
	 * as well as bare integers which default to EPSG. Custom WKTs registered via {@link #addCrs}
	 * are checked first (EPSG codes only).
	 *
	 * <p><b>longitudeFirst semantics</b>:
	 * <ul>
	 *   <li>{@code false} — authority-declared axis order. For EPSG geographic CRS this is
	 *       <b>(latitude, longitude)</b>; for EPSG projected CRS typically <b>(easting, northing)</b>.
	 *       Use this when strict EPSG compliance is required, as for the coordinates of OGC
	 *       requests. Unlike {@link org.geotools.referencing.CRS#decode(String, boolean)}, it holds
	 *       also with the global longitude first order of {@link #initLongitudeFirst()}, since the
	 *       CRS is decoded from the OGC URN form of the code. Such CRSs are not equal, for
	 *       {@link #crsEquals}, to their longitude first versions: keep them to the edges, as the
	 *       requests, not in the data.</li>
	 *   <li>{@code true} — force longitude (or easting) as the first axis. For EPSG:4326 this
	 *       gives <b>(longitude, latitude)</b>, which is the convention used by most mapping
	 *       libraries, WGS 84 bounding boxes, and GeoJSON.</li>
	 * </ul>
	 *
	 * @param crsCode       authority-qualified code (e.g. {@code "EPSG:4326"}, {@code "ESRI:102700"})
	 *                      or a bare integer treated as EPSG (e.g. {@code "4326"}).
	 * @param longitudeFirst {@code true} to put longitude/easting first; {@code false} for the
	 *                       authority-declared order (latitude first for geographic CRS).
	 */
	public CoordinateReferenceSystem getCrs( String crsCode, boolean longitudeFirst ) throws FactoryException {
		init();
		CrsId crsId = CrsId.of(crsCode);
		if (crsId.isEpsg()) {
			CoordinateReferenceSystem custom = addedCrsObjects.get(String.valueOf(crsId.code));
			if (custom != null) {
				return custom;
			}
		}
		if (longitudeFirst) {
			return CRS.decode(crsId.toAuthorityCode(), true);
		}
		// CRS.decode(code, false) follows the global longitude first order, the OGC URN form of
		// the code always gives the authority order, also for the codes of the EPSG extensions
		return CRS.decode("urn:ogc:def:crs:" + crsId.authority + "::" + crsId.code);
	}

	public static boolean crsEquals(CoordinateReferenceSystem fcCrs, CoordinateReferenceSystem _crs) {
		return CRS.equalsIgnoreMetadata(fcCrs, _crs);
	}
	
	public static String getCodeFromCrs( CoordinateReferenceSystem crs ) throws Exception {
        return CrsUtilities.getCodeFromCrs(crs);
    }

    public static Integer getSrid( CoordinateReferenceSystem crs ) throws FactoryException {
        return CrsUtilities.getSrid(crs);
    }
}
