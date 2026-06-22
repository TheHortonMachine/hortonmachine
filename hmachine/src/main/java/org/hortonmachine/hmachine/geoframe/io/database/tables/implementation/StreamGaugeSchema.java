package org.hortonmachine.hmachine.geoframe.io.database.tables.implementation;

import java.util.List;

import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.AbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.GeoAbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.TableField;
import org.locationtech.jts.geom.Point;

/**
 * @deprecated change wit HydroMeteoStation?
 *
 * 
 * 
 * @author Daniele Andreis
 *
 *
 */
public class StreamGaugeSchema extends GeoAbstractSchema {

	protected StreamGaugeSchema() {
		super("stream_gauge", StreamGaugeField.class);
		// TODO Auto-generated constructor stub
	}

	public enum StreamGaugeField implements TableField {

		GEOM("the_geom", Point.class), STREAM_GAUGE_ID("stream_gauge_id", String.class),
		BASIN_ID("basin_id", Integer.class);

		private final String columnName;
		private final Class<?> javaType;

		StreamGaugeField(String columnName, Class<?> javaType) {
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