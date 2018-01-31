/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.io;

import java.io.IOException;
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

    public void close() throws IOException;

}