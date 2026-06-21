package org.hortonmachine.hmachine.geoframe.io.database.tables.implementation;

import java.util.List;

import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.GeoAbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.TableField;
import org.locationtech.jts.geom.LineString;
/**
 * 
 * 
 * @author Daniele Andreis
 */
public final class NetworkSchema extends GeoAbstractSchema {
	public NetworkSchema() {
		super("net", NetworkCommonFields.class);
	}

	public enum NetworkCommonFields implements TableField {

		GEOM("the_geom", LineString.class), BASIN_ID("basinid", Integer.class), LENGTH_M("length_m", Double.class),
		HACK("hack", Double.class), PFAFORDER("pfaforder", Double.class);

		private final String columnName;
		private final Class<?> javaType;

		NetworkCommonFields(String columnName, Class<?> javaType) {
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