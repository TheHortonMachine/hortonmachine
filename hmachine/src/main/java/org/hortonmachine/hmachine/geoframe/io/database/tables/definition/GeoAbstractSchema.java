package org.hortonmachine.hmachine.geoframe.io.database.tables.definition;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;

/**
 * Base schema for geospatial tables linked to GeoTools feature models.
 *
 * <p>
 * This class builds a {@link SimpleFeatureBuilder} from the declared
 * {@link TableField} enum, mapping each column name and Java type into a
 * {@link SimpleFeatureType}.
 * 
 * @author Daniele Andreis
 */
public abstract class GeoAbstractSchema extends AbstractSchema implements GeoframeGeoTableSchema {

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
