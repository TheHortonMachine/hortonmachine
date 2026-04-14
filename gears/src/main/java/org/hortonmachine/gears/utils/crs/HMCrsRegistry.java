package org.hortonmachine.gears.utils.crs;

import java.util.HashMap;
import java.util.Map;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;

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

	
	public CoordinateReferenceSystem getCrs(String epsgCode) throws FactoryException {
		return getCrs(epsgCode, false);
	}
	
	public CoordinateReferenceSystem getCrs(String epsgCode, boolean longitudeFirst) throws FactoryException {
		init();

		String normalized = epsgCode.toUpperCase().replace("EPSG:", "").trim();
		CoordinateReferenceSystem custom = addedCrsObjects.get(normalized);
		if (custom != null) {
			return custom;
		}
		return CRS.decode("EPSG:" + normalized, longitudeFirst);
	}

	public static boolean crsEquals(CoordinateReferenceSystem fcCrs, CoordinateReferenceSystem _crs) {
		return CRS.equalsIgnoreMetadata(fcCrs, _crs);
	}

}
