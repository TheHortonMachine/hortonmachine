package org.hortonmachine.hmachine.geoframe.io.database.tables;

public class TopologySchema extends AbstractSchema {
	public TopologySchema() {
		super("topology", TopologyField.class);
		// TODO Auto-generated constructor stub
	}

	public enum TopologyField implements TableField {
		UPPSTREAM_BASIN("upstream_basin_id", Integer.class), DOWNSTREAM_BASIN("downstream_basin_id", Integer.class);

		private final String columnName;
		private final Class<?> javaType;

		TopologyField(String columnName, Class<?> javaType) {
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
		return "CREATE TABLE " + this.tableName + " ( " + TopologyField.UPPSTREAM_BASIN.columnName() + " INTEGER, "
				+ TopologyField.DOWNSTREAM_BASIN.columnName() + " INTEGER " + ");";
	}
}