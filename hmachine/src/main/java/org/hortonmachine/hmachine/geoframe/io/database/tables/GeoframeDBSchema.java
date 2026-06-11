package org.hortonmachine.hmachine.geoframe.io.database.tables;

public interface GeoframeDBSchema {
	public String createTableSql();

	String tableName();

	TableField[] fields();
}
