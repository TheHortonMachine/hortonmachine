package org.hortonmachine.hmachine.geoframe.io.database.tables.implementation;

import java.util.List;

import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.AbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.TableField;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.VarField;

import com.google.errorprone.annotations.Var;

/**
 * Schema for raw environmental data observations.
 *
 * <p>
 * This table stores unprocessed measurements and model-derived values coming
 * from heterogeneous sources such as meteorological stations, gridded datasets,
 * stream gauges, and remote sensing products.
 *
 * <p>
 * Data is stored before any spatial aggregation or assignment to hydrological
 * units (e.g., basin linkage).
 *
 * <p>
 * Each record represents a single observed value of a variable at a given time
 * and source identifier.
 *
 * @author Daniele Andreis
 */
public class RawDataSchema extends AbstractSchema {

	public RawDataSchema() {
		super("raw_data", RawField.class);
	}

	public enum RawField implements TableField {

		TS("ts", Long.class), STATION_ID("station_id", Integer.class), VAR_ID("var_id", Integer.class),
		VALUE("value", Double.class);

		private final String columnName;
		private final Class<?> javaType;

		RawField(String columnName, Class<?> javaType) {
			this.columnName = columnName;
			this.javaType = javaType;
		}

		@Override
		public String columnName() {
			// TODO Auto-generated method stub
			return columnName;
		}

		@Override
		public Class<?> javaType() {
			// TODO Auto-generated method stub
			return javaType;
		}

	}



	@Override
	protected List<TableField> primaryKey() {
		// TODO Auto-generated method stub
		return List.of(RawField.STATION_ID);
	}

	@Override
	protected List<ForeignKey> foreignKeys() {
		// TODO Auto-generated method stub
		return List.of(new ForeignKey(RawField.VAR_ID, GeoFrameSimpleTable.VAR.name(), VarField.VAR_ID));
	}
}
