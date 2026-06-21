package org.hortonmachine.hmachine.geoframe.io.database.tables;

public class VarSchema extends AbstractSchema {
	public VarSchema() {
		super("environmental_variables", VarField.class);
	}

	public enum VarField implements TableField {
		VAR_ID("var_id", Integer.class), NAME("name", String.class), UNIT("unit", String.class),
		DESCRIPTION("description", String.class);

		private final String columnName;
		private final Class<?> javaType;

		VarField(String columnName, Class<?> javaType) {
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

	@Override
	public String createTableSql() {
		// TODO Auto-generated method stub
		return null;
	}

}