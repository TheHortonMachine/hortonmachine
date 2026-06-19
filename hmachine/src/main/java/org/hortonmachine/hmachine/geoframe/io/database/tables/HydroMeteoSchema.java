package org.hortonmachine.hmachine.geoframe.io.database.tables;

public class HydroMeteoSchema extends AbstractSchema {
	public HydroMeteoSchema() {
		super("hydrometeo_data", HydroMeteoField.class);
	}

	public enum HydroMeteoField implements TableField {
		TS("ts", Long.class), BASIN_ID("basin_id", Integer.class), VAR_ID("avr_id", Integer.class),
		VALUE("value", Double.class), DATA_ORIGIN("data_origin", String.class);

		private final String columnName;
		private final Class<?> javaType;

		HydroMeteoField(String columnName, Class<?> javaType) {
			this.columnName = columnName;
			this.javaType = javaType;
		}

		public String columnName() {
			// TODO Auto-generated method stub
			return columnName;
		}

		public Class<?> javaType() {
			// TODO Auto-generated method stub
			return javaType;
		}

	}

	public enum DataOrigin {
		OBSERVED, INTERPOLATED;
	}

	@Override
	public String createTableSql() {
		// TODO Auto-generated method stub
		return null;
	}
}