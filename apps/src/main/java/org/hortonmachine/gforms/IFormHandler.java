package org.hortonmachine.gforms;

import java.util.List;

public interface IFormHandler {
    
    /**
     * Checks if the source exists. The file or the db table.
     * 
     * @return true if the source exists.
     */
    boolean exists();

    /**
     * @return <code>true</code> if the source is file based.
     */
    boolean isFileBased();
    
    /**
     * Get the form for the given handler. 
     * 
     * If the form doesn't exist (example db without form entry), it return null.
     * 
     * @return the form or <code>null</code> if it does not exist.
     * @throws Exception
     */
    String getForm() throws Exception ;
    
    
    /**
     * Saves the form to the data source.
     * 
     * @param form teh form json string.
     * @throws Exception
     */
    void saveForm(String form) throws Exception ;
    
    /**
     * Get a label for the datasource it backs. 
     * 
     * @return the datasource label.
     */
    String getLabel();
    
    /**
     * Get fixed keys that can be used. 
     * 
     * @return a list of fixed keys if they need to be forced or null. 
     */
    List<String> getFormKeys();

}
