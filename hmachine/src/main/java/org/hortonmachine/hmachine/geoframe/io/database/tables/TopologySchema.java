package org.hortonmachine.hmachine.geoframe.io.database.tables;

public class TopologySchema extends AbstractSchema {
	public TopologySchema() {
		super("topology", TopologyField.class);
		// TODO Auto-generated constructor stub
	}

	public enum TopologyField implements TableField {
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