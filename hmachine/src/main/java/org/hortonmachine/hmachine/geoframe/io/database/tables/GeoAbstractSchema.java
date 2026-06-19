package org.hortonmachine.hmachine.geoframe.io.database.tables;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;

public abstract class GeoAbstractSchema extends AbstractSchema implements GeoTableSchema {

	protected GeoAbstractSchema(String tableName, Class<? extends TableField> fieldClass) {
		super(tableName, fieldClass);
	}

	@Override
	public SimpleFeatureBuilder getSFBuilder(CoordinateReferenceSystem crs) {
		TableField[] columns = fields();
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName(this.tableName);
		b.setCRS(crs);
		for (TableField col : columns) {
			b.add(col.columnName(), col.javaType());
		}
		SimpleFeatureType type = b.buildFeatureType();
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
		return builder;
	}

}
