package org.hortonmachine.hmachine.geoframe.io.database.tables;

import org.locationtech.jts.geom.LineString;

public enum NetworkCommonFields implements TableFields {

	GEOM("the_geom", LineString.class),
    BASIN_ID("basinid", Integer.class),
    LENGTH_M("length_m", Double.class);

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
