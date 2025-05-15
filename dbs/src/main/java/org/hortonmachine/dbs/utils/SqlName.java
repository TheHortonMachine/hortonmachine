package org.hortonmachine.dbs.utils;

/**
 * A name for sql related strings (table names and column names ex.).
 * 
 * This will contain the original name,the fixed one and the one between square brackets.
 */
public class SqlName {

    public String name;

    /// The name fixed by quoting, if necessary.
    ///
    /// This might be needed for strange table names.
    public String fixedName;

    /// The name fixed by double quoting, if necessary.
    ///
    /// This might be needed for strange table names in some cases (ex. create table on postgresql).
    public String fixedDoubleName;

    /// The name fixed by surrounding with square brackets, if necessary.
    ///
    /// This might be needed for example in select queries for strange column names.
    public String bracketName;
    
    /**
     * Make a new instance of sqlname. 
     * 
     * @param name the name to use.
     * @return the instance of SqlName.
     */
    public static SqlName m(String name) {
        return new SqlName(name);
    }

    private SqlName( String name ) {
        this.name = name;
        fixedName = DbsUtilities.fixWithQuotes(name);
        bracketName = DbsUtilities.fixWithBrackets(name);
        fixedDoubleName = DbsUtilities.fixWithDoubleQuotes(name);
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    public String nameForIndex() {
        return this.name.replaceAll("\\s+", "_");
    }
    
}