package org.hortonmachine.hmachine.geoframe.io.database.tables.implementation;

import java.util.Date;
import java.util.List;

import org.hortonmachine.gears.utils.time.ETimeUtilities;
import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.AbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.TableField;

/**
 * 
 * 
 * @author Daniele Andreis
 */
public class SimulationSchema extends AbstractSchema {
	private final static String PREFIX = "simulation_";

	public SimulationSchema() {
		super(PREFIX + ETimeUtilities.INSTANCE.TIMESTAMPFORMATTER_UTC.format(new Date())
				+ "_water_budget_simulation_discharge", SimulationField.class);
		// TODO Auto-generated constructor stub
	}

	public enum SimulationField implements TableField {
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