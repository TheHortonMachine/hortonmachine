/*
 * Copyright 2011 Michael Bedward
 * 
 * This file is part of jai-tools.
 *
 * jai-tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * jai-tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public 
 * License along with jai-tools.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package org.jgrasstools.gears.modules.r.mapcalc;

import jaitools.CollectionFactory;
import jaitools.jiffle.runtime.JiffleEvent;
import jaitools.jiffle.runtime.JiffleEventListener;
import jaitools.jiffle.runtime.JiffleExecutorResult;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * An event listener that uses a {@code CountDownLatch} to force the client to
 * wait for the expected number of tasks to be completed.
 * 
 * @author Michael Bedward
 * @since 1.1
 * @version $Id$
 */
public class WaitingListener implements JiffleEventListener {

    private CountDownLatch latch = null;

    private final List<JiffleExecutorResult> results = CollectionFactory.list();
    
    /**
     * Sets the number of task completions and/or failures to wait for.
     * 
     * @param n number of tasks
     */
    public void setNumTasks(int n) {
        if (latch != null && latch.getCount() > 0) {
            throw new IllegalStateException("Method called during wait period");
        }

        latch = new CountDownLatch(n);
    }
    
    /**
     * Waits for tasks to finish.
     */
    public boolean await(long timeOut, TimeUnit units) {
        if (latch == null) {
            throw new RuntimeException("Called await without setting number of tasks");
        }
        
        try {
            boolean isZero = latch.await(timeOut, units);
            if (!isZero) {
                return false;
            }
            return true;
            
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void await() {
        if (latch == null) {
            throw new RuntimeException("Called await without setting number of tasks");
        }
        
        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public List<JiffleExecutorResult> getResults() {
        return results;
    }

    public void onCompletionEvent(JiffleEvent ev) {
        JiffleExecutorResult result = ev.getResult();
        results.add(result);
        latch.countDown();
    }

    public void onFailureEvent(JiffleEvent ev) {
        JiffleExecutorResult result = ev.getResult();
        results.add(result);
        latch.countDown();
    }

}
