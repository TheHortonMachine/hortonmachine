package org.hortonmachine.hmachine.geoframe.io.database.tables.implementation;

import java.util.List;

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

}