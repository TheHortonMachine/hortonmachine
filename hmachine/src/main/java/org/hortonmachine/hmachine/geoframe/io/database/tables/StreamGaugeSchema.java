package org.hortonmachine.hmachine.geoframe.io.database.tables;

import org.locationtech.jts.geom.Point;

/**
 * @deprecated change wit HydroMeteoStation?
 */
public class StreamGaugeSchema extends AbstractSchema {

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

	@Override
	public String createTableSql() {
		// TODO Auto-generated method stub
		return null;
	}
}