package org.hortonmachine.hmachine.geoframe.io.database.tables;

public class RawDataSchema extends AbstractSchema {

	public RawDataSchema() {
		super("raw_data", RawField.class);
		// TODO Auto-generated constructor stub
	}

	public enum RawField implements TableField {

		TS("ts", Long.class), STATION_ID("basin_id", Integer.class), VAR_ID("avr_id", Integer.class),
		VALUE("value", Double.class), DATA_ORIGIN("data_origin", String.class);

		private final String columnName;
		private final Class<?> javaType;

		RawField(String columnName, Class<?> javaType) {
			this.columnName = columnName;
			this.javaType = javaType;
		}

		@Override
		public String columnName() {
			// TODO Auto-generated method stub
			return columnName;
		}

		@Override
		public Class<?> javaType() {
			// TODO Auto-generated method stub
			return javaType;
		}

	}

	@Override
	public String createTableSql() {
		// TODO Auto-generated method stub
		return null;
	}
}
