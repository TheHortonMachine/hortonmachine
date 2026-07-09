package org.hortonmachine.hmachine.geoframe.io.database.tables.implementation;

import java.util.List;

import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.SimpleAbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.TableField;

/**
 * Schema definition for the {@code topology} table.
 *
 * <p>
 * This table stores the topological relationships between basins, linking each
 * upstream basin to its corresponding downstream basin.
 * </p>
 *
 * <p>
 * Column definitions are centralized in the {@link TopologyField} enumeration
 * to ensure consistency between SQL generation and Java code.
 * </p>
 *
 * @author Daniele Andreis
 */
public class TopologySchema extends SimpleAbstractSchema {
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
			return columnName;
		}

		public Class<?> javaType() {
			return javaType;
		}

	}

	@Override
	protected List<TableField> primaryKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<ForeignKey> foreignKeys() {
		// TODO Auto-generated method stub
		return null;
	}
}