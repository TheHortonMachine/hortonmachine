package org.hortonmachine.dbs.utils;

import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.objects.SchemaLevel;

public class TableName {
    private String name;
    public String fixedName;
    public String fixedDoubleName;
    public String bracketName;
    private String schema = "public";
    private ETableType tableType;

    // Constructor
    public TableName(String name, String schema, ETableType tableType) {
        this.name = name;
        this.schema = schema;
        this.tableType = tableType;

        if(SchemaLevel.FALLBACK_SCHEMA.equals(this.schema)) {
            this.schema = null;
        }

        if (this.schema != null) {
            this.fixedName = this.schema + "." + DbsUtilities.fixWithQuotes(this.name);
            this.bracketName = this.schema + "." + DbsUtilities.fixWithBrackets(this.name);
            this.fixedDoubleName = this.schema + "." + DbsUtilities.fixWithDoubleQuotes(this.name);
        } else {
            this.fixedName = DbsUtilities.fixWithQuotes(this.name);
            this.bracketName = DbsUtilities.fixWithBrackets(this.name);
            this.fixedDoubleName = DbsUtilities.fixWithDoubleQuotes(this.name);
        }
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
		return SqlName.m(getFullName());
	}
}

