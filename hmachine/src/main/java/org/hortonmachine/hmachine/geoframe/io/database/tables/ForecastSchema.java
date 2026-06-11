package org.hortonmachine.hmachine.geoframe.io.database.tables;

public class ForecastSchema extends AbstractSchema {

	public ForecastSchema() {
		super("forecast_meteo_data", ForecastField.class);
	}

	public enum ForecastField implements TableField {
		;

		public String columnName() {
			// TODO Auto-generated method stub
			return null;
		}

		public Class<?> javaType() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	@Override
	public String createTableSql() {
		// TODO Auto-generated method stub
		return null;
	}
}