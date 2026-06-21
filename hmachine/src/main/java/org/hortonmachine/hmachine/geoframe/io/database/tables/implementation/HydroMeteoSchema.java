package org.hortonmachine.hmachine.geoframe.io.database.tables.implementation;

import java.util.List;

import org.hortonmachine.hmachine.geoframe.io.database.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.AbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.TableField;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinPoligonSchema.BasinMultiPolygonField;

/**
 * 
 * 
 * @author Daniele Andreis
 */
public class HydroMeteoSchema extends AbstractSchema {
	public HydroMeteoSchema() {
		super("hydrometeo_data", HydroMeteoField.class);
	}

	public enum HydroMeteoField implements TableField {
		TS("ts", Long.class), BASIN_ID("basin_id", Integer.class), VAR_ID("avr_id", Integer.class),
		VALUE("value", Double.class), DATA_ORIGIN("data_origin", String.class);

		private final String columnName;
		private final Class<?> javaType;

		HydroMeteoField(String columnName, Class<?> javaType) {
			this.columnName = columnName;
			this.javaType = javaType;
		}

		public String columnName() {
			// TODO Auto-generated method stub
			return columnName;
		}

		public Class<?> javaType() {
			// TODO Auto-generated method stub
			return javaType;
		}

	}

	public enum DataOrigin {
		OBSERVED, INTERPOLATED;
	}

	@Override
	protected List<TableField> primaryKey() {
		// TODO Auto-generated method stub
		return List.of(HydroMeteoField.TS, HydroMeteoField.BASIN_ID, HydroMeteoField.VAR_ID);
	}

	@Override
	protected List<ForeignKey> foreignKeys() {
		// TODO Auto-generated method stub
		return List.of(
				new ForeignKey(HydroMeteoField.BASIN_ID, GeoFrameGeoTable.BASIN.name(),
						BasinMultiPolygonField.BASIN_ID),
				new ForeignKey(HydroMeteoField.VAR_ID, GeoFrameSimpleTable.VAR.name(), VarSchema.VarField.VAR_ID));
	}
}