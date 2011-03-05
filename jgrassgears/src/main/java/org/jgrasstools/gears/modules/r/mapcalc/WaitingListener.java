/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.gears.modules.r.mapcalc;

import jaitools.jiffle.runtime.JiffleEvent;
import jaitools.jiffle.runtime.JiffleEventListener;
import jaitools.jiffle.runtime.JiffleExecutorResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * An event listener with a wait function.
 * 
 * @author Michael Bedward
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class WaitingListener implements JiffleEventListener {

    private CountDownLatch latch;

    private final Map<Integer, JiffleExecutorResult> results = new ConcurrentHashMap<Integer, JiffleExecutorResult>();

    /**
     * Sets the number of job completions and/or failures to wait for.
     * 
     * @param n number of jobs
     */
    public synchronized void setNumJobs( int n ) {
        if (latch != null && latch.getCount() > 0) {
            throw new IllegalStateException("Method called during wait period");
        }

        latch = new CountDownLatch(n);
    }

    /**
     * Waits for jobs to finish.
     */
    public void await() {
        try {
            latch.await();
        } catch (InterruptedException ignored) {
            ignored.printStackTrace();
        }
    }

    public synchronized JiffleExecutorResult getResult( int jobID ) {
        return results.get(jobID);
    }

    public void onCompletionEvent( JiffleEvent ev ) {
        latch.countDown();
        JiffleExecutorResult result = ev.getResult();
        results.put(result.getTaskID(), result);
    }

    public void onFailureEvent( JiffleEvent ev ) {
        latch.countDown();
        JiffleExecutorResult result = ev.getResult();
        results.put(result.getTaskID(), result);
    }

}
