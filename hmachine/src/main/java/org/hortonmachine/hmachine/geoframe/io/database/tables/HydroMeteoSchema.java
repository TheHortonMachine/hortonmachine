package org.hortonmachine.hmachine.geoframe.io.database.tables;

public class HydroMeteoSchema extends AbstractSchema {
	public HydroMeteoSchema() {
		super("hydrometeo_data", HydroMeteoField.class);
	}

	public enum HydroMeteoField implements TableField {
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