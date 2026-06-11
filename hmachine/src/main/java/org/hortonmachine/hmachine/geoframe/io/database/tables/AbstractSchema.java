package org.hortonmachine.hmachine.geoframe.io.database.tables;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class AbstractSchema implements GeoframeDBSchema {

	protected final String tableName;
	protected final Class<? extends TableField> fieldClass;

	protected AbstractSchema(String tableName, Class<? extends TableField> fieldClass) {

		this.tableName = tableName;
		this.fieldClass = fieldClass;
	}

	@Override
	public String tableName() {
		return tableName;
	}

	@Override
	public TableField[] fields() {
		return fieldClass.getEnumConstants();
	}

	public String buildSelect(TableField... fields) {
		String commaFields = Arrays.stream(fields).map(TableField::columnName).collect(Collectors.joining(", "));
		return "SELECT " + commaFields + " FROM " + this.tableName;
	}

	public String buildSelectAll() {
		TableField[] fields = this.fieldClass.getEnumConstants();
		return this.buildSelect(fields);
	}

}