package org.hortonmachine.dbs.utils;

import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.objects.SchemaLevel;

public class TableName extends SqlName {
    private final ETableType tableType;

    // Constructor
    public TableName( String name, String schema, ETableType tableType ) {
        super(SchemaLevel.FALLBACK_SCHEMA.equals(schema) ? null : schema, name);
        this.tableType = tableType;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        if (hasSchema()) {
            return schema + "." + name;
        }
        return name;
    }

    public boolean hasSchema() {
        return schema != null;
    }

    public String getSchema() {
        return schema;
    }

    public ETableType getTableType() {
        return tableType;
    }
    
    public SqlName toSqlName() {
        return SqlName.qualified(schema, name);
    }
    
    @Override
    public String toString() {
        return getFullName();
    }
}
