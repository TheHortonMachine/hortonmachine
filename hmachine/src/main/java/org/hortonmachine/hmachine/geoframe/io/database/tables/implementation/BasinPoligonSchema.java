package org.hortonmachine.hmachine.geoframe.io.database.tables.implementation;

import java.util.List;

import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.GeoAbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.TableField;
import org.locationtech.jts.geom.MultiPolygon;
/**
 * Schema definition for the "basin" table.
 *
 * <p>This table stores basin geometries as multipolygons and associated summary
 * attributes (typically derived from geomorphological analysis).
 *
 * <p>Each record represents a basin with its geometric representation and
 * aggregated physical properties such as area, elevation...
 */
public final class BasinPoligonSchema extends GeoAbstractSchema {

	public BasinPoligonSchema() {
		super("basin", BasinMultiPolygonField.class);
	}

	public enum BasinMultiPolygonField implements TableField {

		GEOM("the_geom", MultiPolygon.class), BASIN_ID("basinid", Integer.class), CENTER_X("centrx", Double.class),
		CENTER_Y("centry", Double.class), ELEVATION_M("elev_m", Double.class),
		AVG_ELEVATION_M("avgelev_m", Double.class), AREA_KM2("area_km2", Double.class),
		LENGTH_M("length_m", Double.class), SKYVIEW("skyview", Double.class), TYPE("type", Integer.class);

		private final String columnName;
		private final Class<?> javaType;

		BasinMultiPolygonField(String columnName, Class<?> javaType) {
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
}