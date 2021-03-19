package org.hortonmachine.gforms;

import java.util.List;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.IHmExtrasDb;
import org.hortonmachine.dbs.utils.DbsUtilities;

public class DbFormHandler implements IFormHandler {

    private ADb db;
    private String tableName;

    public DbFormHandler( ADb db, String tableName ) {
        this.db = db;
        this.tableName = tableName;
    }

    @Override
    public boolean isFileBased() {
        return false;
    }

    @Override
    public boolean exists() {
        try {
            return db != null && db.hasTable(tableName);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getForm() throws Exception {
        if (db == null || !(db instanceof IHmExtrasDb))
            return null;
        return ((IHmExtrasDb) db).getFormString(tableName);
    }

    @Override
    public void saveForm( String form ) throws Exception {
        if (db != null && db instanceof IHmExtrasDb)
            ((IHmExtrasDb) db).updateForm(tableName, form);
    }

    @Override
    public String getLabel() {
        if (db == null)
            return null;
        return db.getDatabasePath() + "#" + tableName;
    }

    @Override
    public List<String> getFormKeys() {
        try {
            return DbsUtilities.getTableAlphanumericFields(db, tableName);
        } catch (Exception e) {
            return null;
        }
    }
}
