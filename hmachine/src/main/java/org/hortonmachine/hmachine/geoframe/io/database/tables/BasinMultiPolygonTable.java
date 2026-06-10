package org.hortonmachine.hmachine.geoframe.io.database.tables;

import org.locationtech.jts.geom.MultiPolygon;

public enum BasinMultiPolygonTable implements TableFields {

	GEOM("the_geom", MultiPolygon.class), 
	BASIN_ID("basinid", Integer.class), 
	CENTER_X("centrx", Double.class),
	CENTER_Y("centry", Double.class),
	ELEVATION_M("elev_m", Double.class),
	AVG_ELEVATION_M("avgelev_m", Double.class),
	AREA_KM2("area_km2", Double.class), 
	LENGTH_M("length_m", Double.class), 
	SKYVIEW("skyview", Double.class),
	TYPE("type", Integer.class);

	private final String columnName;
	private final Class<?> javaType;

	BasinMultiPolygonTable(String columnName, Class<?> javaType) {
		this.columnName = columnName;
		this.javaType = javaType;
	}

	public String columnName() {
		return columnName;
	}

	public Class<?> javaType() {
		return javaType;
	}
}