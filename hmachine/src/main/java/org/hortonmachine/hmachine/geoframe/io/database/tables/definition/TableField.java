package org.hortonmachine.hmachine.geoframe.io.database.tables.definition;

/**
 * Represents a field (column) of a database table schema.
 *
 * <p>Implementations typically use enums to define the set of columns
 * belonging to a table, providing metadata such as the column name
 * and the corresponding Java type.</p>
 *
 * <p>This abstraction allows table definitions and SQL generation
 * to be handled in a type-safe and consistent manner.</p>
 *
 * @author Daniele Andreis
 */
public interface TableField {
	/**
	 * Returns the table column name.
	 *
	 * @return the column name
	 */
	public String columnName();

	/**
	 * Returns the Java type associated with the column.
	 *
	 * @return the Java type
	 */
	public Class<?> javaType();

}
