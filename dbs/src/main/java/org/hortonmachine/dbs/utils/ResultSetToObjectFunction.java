package org.hortonmachine.dbs.utils;

import org.hortonmachine.dbs.compat.IHMResultSet;

/**
 * A functional interface to convert a resultset to object.
 * 
 *  <p>This is necessary to make the android part compatible.</p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@FunctionalInterface
public interface ResultSetToObjectFunction {
 
    /**
     * Get the object from the resultset.
     * 
     * @param resultSet the resultset to use.
     * @param index the index of the object to get.
     * @return the object or <code>null</code>.
     */
    Object getObject(IHMResultSet resultSet, int index);
 
}