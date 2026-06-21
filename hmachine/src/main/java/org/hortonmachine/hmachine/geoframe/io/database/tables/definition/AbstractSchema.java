package org.hortonmachine.hmachine.geoframe.io.database.tables.definition;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for database table schema definitions.
 *
 * <p>
 * A schema consists of a table name and a set of fields represented by an enum
 * implementing {@link TableField}. This class provides utility methods for
 * accessing schema metadata and generating common SQL statements.
 * </p>
 *
 * <p>
 * Concrete implementations are responsible for defining the table structure and
 * overriding {@link #createTableSql()} to provide the corresponding CREATE
 * TABLE statement.
 * </p>
 *
 * @author Daniele Andreis
 */
public abstract class AbstractSchema {

	protected final String tableName;

	protected final Class<? extends TableField> fieldClass;

	protected AbstractSchema(String tableName, Class<? extends TableField> fieldClass) {

		this.tableName = tableName;
		this.fieldClass = fieldClass;
	}

	/**
	 * Returns the database table name.
	 *
	 * @return the table name
	 */
	public String tableName() {
		return tableName;
	}

	/**
	 * Returns all fields defined for this schema.
	 *
	 * @return the schema fields
	 */
	public TableField[] fields() {
		return fieldClass.getEnumConstants();
	}

	/**
	 * Builds a SELECT statement including the specified fields.
	 *
	 * @param fields the fields to include in the query
	 * @return a SELECT statement for the configured table
	 */
	public String buildSelect(TableField... fields) {
		String commaFields = Arrays.stream(fields).map(TableField::columnName).collect(Collectors.joining(", "));
		return "SELECT " + commaFields + " FROM " + this.tableName;
	}

	/**
	 * Builds a SELECT statement including all table fields.
	 *
	 * @return a SELECT statement containing all schema fields
	 */
	public String buildSelectAll() {
		TableField[] fields = this.fieldClass.getEnumConstants();
		return this.buildSelect(fields);
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

		sb.append("CREATE TABLE ").append(tableName()).append(" (\n");

		List<TableField> cols = List.of(fields());

		for (int i = 0; i < cols.size(); i++) {
			TableField f = cols.get(i);

			sb.append("    ").append(f.columnName()).append(" ").append(sqlType(f.javaType()));

			sb.append(",\n");
		}

		List<TableField> pk = primaryKey();

		if (!pk.isEmpty()) {
			sb.append("    PRIMARY KEY (");

			for (int i = 0; i < pk.size(); i++) {
				sb.append(pk.get(i).columnName());
				if (i < pk.size() - 1)
					sb.append(", ");
			}

			sb.append("),\n");
		}

		List<ForeignKey> fks = foreignKeys();

		for (int i = 0; i < fks.size(); i++) {

			ForeignKey fk = fks.get(i);

			sb.append("    FOREIGN KEY (").append(fk.column().columnName()).append(") REFERENCES ")
					.append(fk.refTable()).append("(").append(fk.refColumn().columnName()).append(")");

			if (i < fks.size() - 1) {
				sb.append(",");
			}

			sb.append("\n");
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