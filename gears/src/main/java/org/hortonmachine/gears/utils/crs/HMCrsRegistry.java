package org.hortonmachine.gears.utils.crs;

import java.util.HashMap;
import java.util.Map;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.hortonmachine.dbs.utils.CrsId;

public enum HMCrsRegistry {
	INSTANCE;

	boolean initialized = false;

	private HMCrsRegistry() {
	}

	String wkt6933 = """
			PROJCS["WGS 84 / NSIDC EASE-Grid 2.0 Global",
			    GEOGCS["WGS 84",
			        DATUM["WGS_1984",
			            SPHEROID["WGS 84",6378137,298.257223563,
			                AUTHORITY["EPSG","7030"]],
			            AUTHORITY["EPSG","6326"]],
			        PRIMEM["Greenwich",0,
			            AUTHORITY["EPSG","8901"]],
			        UNIT["degree",0.0174532925199433,
			            AUTHORITY["EPSG","9122"]],
			        AUTHORITY["EPSG","4326"]],
			    PROJECTION["Cylindrical_Equal_Area"],
			    PARAMETER["standard_parallel_1",30],
			    PARAMETER["central_meridian",0],
			    PARAMETER["false_easting",0],
			    PARAMETER["false_northing",0],
			    UNIT["metre",1,
			        AUTHORITY["EPSG","9001"]],
			    AXIS["Easting",EAST],
			    AXIS["Northing",NORTH],
			    AUTHORITY["EPSG","6933"]]
			""";

	public final Map<String, String> addedCrsWkts = new HashMap<>() {
		{
			put("6933", wkt6933);
		}
	};
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
	 * Get a {@link CoordinateReferenceSystem} from a code string using the authority-declared axis
	 * order ({@code longitudeFirst = false}).
	 *
	 * <p>For EPSG geographic CRS (e.g. {@code "EPSG:4326"}) the authority-declared order is
	 * <b>(latitude, longitude)</b>. For EPSG projected CRS (e.g. {@code "EPSG:32632"} UTM) it is
	 * <b>(easting, northing)</b>. Call {@link #getCrs(String, boolean) getCrs(code, true)} when you
	 * need longitude/easting first for a geographic CRS.
	 *
	 * @param crsCode authority-qualified code (e.g. {@code "EPSG:4326"}, {@code "ESRI:102700"}) or a
	 *                bare integer treated as EPSG (e.g. {@code "4326"}).
	 * @see #getCrs(String, boolean)
	 */
	public CoordinateReferenceSystem getCrs( String crsCode ) throws FactoryException {
		return getCrs(crsCode, false);
	}

	/**
	 * Get a {@link CoordinateReferenceSystem} from a code string with explicit axis order control.
	 *
	 * <p>Accepts authority-qualified strings such as {@code "EPSG:4326"} or {@code "ESRI:102700"},
	 * as well as bare integers which default to EPSG. Custom WKTs registered via {@link #addCrs}
	 * are checked first (EPSG codes only).
	 *
	 * <p><b>longitudeFirst semantics</b> (mirrors {@link org.geotools.referencing.CRS#decode(String, boolean)}):
	 * <ul>
	 *   <li>{@code false} — authority-declared axis order. For EPSG geographic CRS this is
	 *       <b>(latitude, longitude)</b>; for EPSG projected CRS typically <b>(easting, northing)</b>.
	 *       Use this when strict EPSG compliance is required.</li>
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
		return CRS.decode(crsId.toAuthorityCode(), longitudeFirst);
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
