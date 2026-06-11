package org.hortonmachine.hmachine.geoframe.io.database.tables;

import java.util.Date;

import org.hortonmachine.gears.utils.time.ETimeUtilities;

public class ClaibrationSchema extends AbstractSchema {
	private final static String PREFIX = "calibration_";

	public ClaibrationSchema() {
		super(PREFIX + ETimeUtilities.INSTANCE.TIMESTAMPFORMATTER_UTC.format(new Date())
				+ "_water_budget_simulation_discharge", CalibrationField.class);
	}

	public enum CalibrationField implements TableField {
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
	public String createTableSql() {
		// TODO Auto-generated method stub
		return null;
	}

}