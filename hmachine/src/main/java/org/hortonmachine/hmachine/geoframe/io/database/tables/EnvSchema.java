package org.hortonmachine.hmachine.geoframe.io.database.tables;

public class EnvSchema extends AbstractSchema {
	public EnvSchema() {
		super("environmental_variables", EnvField.class);
	}

	public enum EnvField implements TableField {
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