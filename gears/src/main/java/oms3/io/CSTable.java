/*
 * $Id: CSTable.java 7cba5ba59d73 2018-11-29 francesco.serafin.3@gmail.com $
 * 
 * This file is part of the Object Modeling System (OMS),
 * 2007-2012, Olaf David and others, Colorado State University.
 *
 * OMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 2.1.
 *
 * OMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OMS.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
 */
package oms3.io;

import java.util.Map;

/** Comma separated table.
 *
 * @author od
 */
public interface CSTable {

    /** Get the name of the table
     * 
     * @return the name
     */
    String getName();

    /** Get the annotations for the table
     * 
     * @return the annotations for the table.
     */
    Map<String, String> getInfo();

    /**
     * Get the info for a column.
     * 
     * @param column the column index
     * @return a map with KVP for column meta data.
     */
    Map<String, String> getColumnInfo(int column);

    /**
     *  Get the column name.
     * @param column the column index
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
