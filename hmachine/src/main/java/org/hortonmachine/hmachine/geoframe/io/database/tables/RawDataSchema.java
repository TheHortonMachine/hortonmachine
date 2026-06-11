package org.hortonmachine.hmachine.geoframe.io.database.tables;

public class RawDataSchema extends AbstractSchema {

	public RawDataSchema() {
		super("raw_data", RawField.class);
		// TODO Auto-generated constructor stub
	}

	public enum RawField implements TableField {
		;

		@Override
		public String columnName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
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
