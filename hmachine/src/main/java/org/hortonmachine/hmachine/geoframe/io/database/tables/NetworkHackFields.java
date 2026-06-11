package org.hortonmachine.hmachine.geoframe.io.database.tables;

public enum NetworkHackFields implements TableField {

    HACK("hack", Double.class);

    private final String columnName;
    private final Class<?> javaType;

    NetworkHackFields(String columnName, Class<?> javaType) {
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