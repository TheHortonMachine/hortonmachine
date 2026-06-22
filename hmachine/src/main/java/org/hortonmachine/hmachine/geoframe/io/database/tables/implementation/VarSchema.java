package org.hortonmachine.hmachine.geoframe.io.database.tables.implementation;

import java.util.List;

import org.hortonmachine.hmachine.geoframe.io.database.importer.CreateTableUtils;
import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.AbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.TableField;

/**
 * Schema definition for the "environmental_variables" reference table.
 *
 * <p>
 * This table acts as a lookup/dictionary for environmental variables used
 * across the database. Its primary purpose is to provide stable identifiers
 * (var_id) that can be referenced as foreign keys in other tables.
 *
 * <p>
 * Each record represents a single environmental variable with an optional unit
 * and description.
 * 
 * <p>
 * The table is automatically populated with a set of default variables by
 * {@link CreateTableUtils}:
 *
 * <ul>
 * <li>Precipitation (var_id = 2)</li>
 * <li>Temperature (var_id = 4)</li>
 * <li>Radiation (var_id = ??)</li>
 * <li>Discharge (var_id = ??)</li>
 * <li>Evapotraspiration (var_id = 1)</li>
 * </ul>
 *
 * <p>
 * The unit associated with a variable should be specified when importing data.
 * For example, precipitation may be expressed in {@code mm/day} or
 * {@code mm/h}.
 *
 * <p>
 * Additional environmental variables can be added to the table as needed.
 *
 * @author Daniele Andreis
 */
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
			return columnName;
		}

		public Class<?> javaType() {
			return javaType;
		}

	}

	@Override
	protected List<TableField> primaryKey() {
		// TODO Auto-generated method stub
		return List.of(VarField.VAR_ID);
	}

	@Override
	protected List<ForeignKey> foreignKeys() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Represents an environmental variable stored in the
	 * {@code environmental_variables} reference table.
	 *
	 * @param varId       unique identifier of the variable
	 * @param name        variable name
	 * @param unit        measurement unit (e.g. mm/day, °C, W/m²)
	 * @param description human-readable description of the variable
	 */
	public record EnvironmentalVariable(Integer varId, String name, String unit, String description) {
	}

	public enum TimeResolution {
		HOURLY, DAILY, MONTHLY, YEARLY
	}

}