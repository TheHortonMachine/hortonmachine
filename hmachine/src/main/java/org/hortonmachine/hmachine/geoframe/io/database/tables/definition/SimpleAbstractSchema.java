package org.hortonmachine.hmachine.geoframe.io.database.tables.definition;

import java.util.List;

/**
 * Base schema for simple table. .
 * 
 * @author Daniele Andreis
 */
public abstract class SimpleAbstractSchema extends AbstractSchema implements GeoframeSimpleTableSchema {

	protected SimpleAbstractSchema(String tableName, Class<? extends TableField> fieldClass) {
		super(tableName, fieldClass);
	}

	/**
	 * Returns the SQL statement required to create the table.
	 *
	 * <p>
	 * Subclasses should override this method to provide a complete CREATE TABLE
	 * statement.
	 * </p>
	 * 
	 * @todo:Maybe to change in abstract??
	 * @return the CREATE TABLE SQL statement
	 */
	public String createTableSql() {
		StringBuilder sb = new StringBuilder();

		sb.append("CREATE TABLE  IF NOT EXISTS ").append(tableName()).append(" (\n");

		List<TableField> cols = List.of(fields());

		for (int i = 0; i < cols.size(); i++) {
			TableField f = cols.get(i);

			sb.append("    ").append(f.columnName()).append(" ").append(sqlType(f.javaType()));
			if (i < cols.size() - 1) {
				sb.append(",\n");
			}
		}

		List<TableField> pk = primaryKey();

		if (pk != null && !pk.isEmpty()) {
			sb.append(", \n    PRIMARY KEY (");

			for (int i = 0; i < pk.size(); i++) {
				sb.append(pk.get(i).columnName());
				if (i < pk.size() - 1)
					sb.append(", ");
			}

			sb.append(")");
		}

		List<ForeignKey> fks = foreignKeys();
		if (fks != null && !fks.isEmpty()) {

			for (int i = 0; i < fks.size(); i++) {

				ForeignKey fk = fks.get(i);

				sb.append(" , \n  FOREIGN KEY (").append(fk.column().columnName()).append(") REFERENCES ")
						.append(fk.refTable()).append("(").append(fk.refColumn().columnName()).append(")");

			//	if (i < fks.size() - 1) {
			//		sb.append(",");
			//	}

				sb.append("\n");
			}
		}

		sb.append(");\n");

		return sb.toString();
	}

	/**
	 * 
	 * @return a list of primary key
	 */
	protected abstract List<TableField> primaryKey();

	/**
	 * 
	 * @return a list of foreign key key
	 */
	protected abstract List<ForeignKey> foreignKeys();

	/*
	 * mapper to sql
	 */
	protected String sqlType(Class<?> type) {

		if (type == Integer.class)
			return "INTEGER";
		if (type == Long.class)
			return "BIGINT";
		if (type == Double.class)
			return "DOUBLE PRECISION";
		if (type == String.class)
			return "TEXT";

		throw new IllegalArgumentException("Unsupported type: " + type);
	}

	public record ForeignKey(TableField column, String refTable, TableField refColumn) {
	}

}
