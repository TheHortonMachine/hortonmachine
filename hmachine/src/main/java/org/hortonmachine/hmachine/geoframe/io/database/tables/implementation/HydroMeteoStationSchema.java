package org.hortonmachine.hmachine.geoframe.io.database.tables.implementation;

import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.GeoAbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.TableField;
import org.locationtech.jts.geom.Point;

/**
 * 
 * 
 * @author Daniele Andreis
 */
public class HydroMeteoStationSchema extends GeoAbstractSchema {

	public HydroMeteoStationSchema() {
		super("station", HydroMeteoStation.class);
	}

	public enum StationType {
		METEO, STREAM_GAUGE;
	}

	
	
	public enum HydroMeteoStation implements TableField {
		GEOM("the_geom", Point.class), ID("id", Integer.class), ELEVATION("elevation", Double.class),
		BASIN_ID("basin_id", Integer.class), TYPE("type", String.class);

		private final String columnName;
		private final Class<?> javaType;

		HydroMeteoStation(String columnName, Class<?> javaType) {
			this.columnName = columnName;
			this.javaType = javaType;
		}

		public String columnName() {
			return this.columnName;
		}

		public Class<?> javaType() {
			return this.javaType;
		}
	}
}