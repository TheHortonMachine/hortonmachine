package org.hortonmachine.hmachine.geoframe.io.database.tables;

public class HydroMeteoSationSchema extends AbstractSchema {

	public HydroMeteoSationSchema() {
		super("hydro_meteo_station", HydroMeteoSation.class);
	}

	public enum HydroMeteoSation implements TableField {
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