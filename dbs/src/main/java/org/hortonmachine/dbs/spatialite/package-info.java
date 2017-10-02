/**
 * <h2>Some comments about spatialite and multithreading</h2>
 * 
 * (thanks to Sandro Furieri for this)
 * 
 * <p>When you launch multiple <b>processes</b>, each process
 * has its own inviolable memory space on which no other process
 * will be able to put its fingers on (apart of the old windows 95
 * and 98, that had a memory management model that was a hybrid 
 * between a colander and a toilet bowl, but that is a different
 * story).</p>
 * 
 * <p>With <b>threads</b> instead the opposite applies. All threads 
 * that are children of the same process share a single memory space
 * that they all own. Often that comes in handy, but in some cases
 * it can be hell.</p>
 * 
 * <p>Since SQLite does <b>NOT</> work in client-server mode 
 * and you are working directly with the SQL engine, it is mandatory
 * that every thread has its own very private memory space
 * in order to avoid violation of the allocation/freeing mechanics
 * by different threads, which would lead to either inconsistent
 * results or segmentation faults.</>
 * 
 * <p>Therefore:
 * <ul>
 *      <li>using one db connection and many threads to interact in read-write
 *      mode is a configuration that <b>HAS TO BE AVOIDED</b></li>
 *      <li>if instead one connection for every thread is used, then one 
 *      needs to make sure that one thread at the time connects
 *      to write, while multiple connections are allowed in read-only mode or
 *      else Exceptions will be raised due to the locking system.
 *      </li>
 *      <li>the fact that all the connection threads come from the same process
 *      in this case doesn't matter. In SQLite one connection blocks the other
 *      and it is impossible to make multiple parallel writing operations.</li>
 *      <li>multithreading for SQLite doesn't make any sense (at least not in 
 *      terms of performance), since everything needs to be semaphorized, which
 *      results in a <b>queued single thread model</b>.</li>
 * </ul>
 * </p>
 * 
 * 
 */
package org.hortonmachine.dbs.spatialite;