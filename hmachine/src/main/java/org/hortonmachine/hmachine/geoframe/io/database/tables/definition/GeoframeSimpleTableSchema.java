package org.hortonmachine.hmachine.geoframe.io.database.tables.definition;

/**
 * Contract for database schema definitions used in Geoframe.
 *
 * <p>
 * Defines the minimal structure required to describe a database table: its
 * name, its fields, and the SQL needed to create it.
 *
 * <p>
 * Implementations typically map enums of {@link TableField} to relational table
 * definitions.
 * 
 * @author Daniele Andreis
 */
public interface GeoframeSimpleTableSchema {
	public String createTableSql();
}
