/*
 * $Id:$
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software
 *     in a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 * 
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 * 
 *  3. This notice may not be removed or altered from any source
 *     distribution.
 */
package oms3.io;

import java.util.Map;

/** Comma separated table.
 *
 * @author Olaf David
 */
public interface CSTable {

    /** Get the name of the table
     * 
     * @return the name
     */
    String getName();

    /** Get the annotations for the table
     * 
     * @return the annotations for the propertyset.
     */
    Map<String, String> getInfo();

    /**
     * Get the info for a column.
     * 
     * @param column
     * @return a map with KVP for column meta data.
     */
    Map<String, String> getColumnInfo(int column);

    /**
     *  Get the column name.
     * @param column
     * @return the column name.
     */
    String getColumnName(int column);

    /** get the number of columns in the table
     * 
     * @return the number of columns.
     */
    int getColumnCount();

    /**
     * get the row iterator.
     * @return row iterator
     */
    Iterable<String[]> rows();

    /**
     * get the row iterator and skip the first rows.
     * @param skipRow the # rows to skip
     * @return row iterator
     */
    Iterable<String[]> rows(final int skipRow);
    
    
}
