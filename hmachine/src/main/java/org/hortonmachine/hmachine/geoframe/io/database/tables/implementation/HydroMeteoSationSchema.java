package org.hortonmachine.hmachine.geoframe.io.database.tables.implementation;

import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.GeoAbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.TableField;
import org.locationtech.jts.geom.Point;

/**
 * 
 * 
 * @author Daniele Andreis
 */
public class HydroMeteoSationSchema extends GeoAbstractSchema {

	public HydroMeteoSationSchema() {
		super("hydro_meteo_station", HydroMeteoSation.class);
	}

	public enum StationType {
		METEO, STREAM_GAUGE;
	}

	public enum HydroMeteoSation implements TableField {
		GEOM("the_geom", Point.class), ID("id", Integer.class), BASIN_ID("basin_id", Integer.class),
		TYPE("type", String.class);

		private final String columnName;
		private final Class<?> javaType;

		HydroMeteoSation(String columnName, Class<?> javaType) {
			this.columnName = columnName;
			this.javaType = javaType;
		}

		public String columnName() {
			// TODO Auto-generated method stub
			return this.columnName;
		}

		public Class<?> javaType() {
			// TODO Auto-generated method stub
			return this.javaType;
		}

	}
}