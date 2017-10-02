/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gears.libs.modules;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

/**
 * Multithreading util class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ThreadedRunnable<E> {

    private ExecutorService fixedThreadPool;
    private ConcurrentLinkedQueue<E> queue = new ConcurrentLinkedQueue<E>();
    private IHMProgressMonitor pm;

    public ThreadedRunnable( int numThreads, IHMProgressMonitor pm ) {
        this.pm = pm;
        fixedThreadPool = Executors.newFixedThreadPool(numThreads);
    }

    public void executeRunnable( Runnable runner ) {
        fixedThreadPool.execute(runner);
    }

    public void waitAndClose() {
        try {
            fixedThreadPool.shutdown();
            fixedThreadPool.awaitTermination(30, TimeUnit.DAYS);
            fixedThreadPool.shutdownNow();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (pm != null) {
            pm.done();
        }
    }

    public void addToQueue( E item ) {
        queue.add(item);
    }

    public ConcurrentLinkedQueue<E> getQueue() {
        return queue;
    }

    public void setWorkLoad( String task, int workLoad ) {
        if (pm != null) {
            if (task == null) {
                task = "Processing...";
            }
            pm.beginTask(task, workLoad);
        }
    }

    public synchronized void worked( int process ) {
        if (pm != null)
            pm.worked(process);
    }
}
