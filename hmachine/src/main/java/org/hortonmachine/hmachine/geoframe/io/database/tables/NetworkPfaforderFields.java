package org.hortonmachine.hmachine.geoframe.io.database.tables;

public enum NetworkPfaforderFields implements TableFields {

    PFAFORDER("pfaforder", Double.class);

    private final String columnName;
    private final Class<?> javaType;

    NetworkPfaforderFields(String columnName, Class<?> javaType) {
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