/*
 * $Id: TableIterator.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
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

import java.util.Iterator;

/** An iterator that allows skipping rows in a table.
 *
 * @author od
 */
public interface TableIterator<T> extends Iterator<T> {

    /** Skip n rows
     * @param n  the number of lines to skip.
     */
    public void skip(int n);
    
    
    /** 
     * Closes the underlying input stream.
     */
    public void close();

}