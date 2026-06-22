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

	
}