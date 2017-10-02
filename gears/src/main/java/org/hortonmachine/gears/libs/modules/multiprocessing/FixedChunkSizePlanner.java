/* 
 * polymap.org
 * Copyright (C) 2017, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.hortonmachine.gears.libs.modules.multiprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class FixedChunkSizePlanner
        extends ExecutionPlanner {

    /**
     * The absolut upper limit of the chunk size.
     */
    public static final int             MAX_CHUNK_SIZE = 10000;
    
    private int                         targetChunkSize = -1;
    
    private List<MultiProcessingTask>   accu;
    
    private List<Future>                submitted = new ArrayList( 1024 );
    
    private Exception                   exc;
    
    
    @Override
    public void submit( MultiProcessingTask task ) {
        // init targetChunkSize
        if (targetChunkSize == -1) {
            if (numberOfTasks <= 0) {
                throw new IllegalStateException( "No setNumberOfTasks() given." );
            }
            int procNum  = Runtime.getRuntime().availableProcessors();
            targetChunkSize = Math.min( numberOfTasks / (procNum*3), MAX_CHUNK_SIZE );
//            System.out.println( "targetChunkSize: " + targetChunkSize );

            accu = new ArrayList( targetChunkSize );
        }
        
        // submit
        accu.add( task );
        if (accu.size() >= targetChunkSize) {
            submitChunk( accu );
            accu = new ArrayList( targetChunkSize );
        }
    }

    
    protected void submitChunk( List<MultiProcessingTask> chunk ) {
       // System.out.println( "submitting chunk: size=" + chunk.size() );
        // work task
        Runnable work = () -> {
            try {
                //System.out.println( Thread.currentThread().getName() + ": starting..." );
                for (MultiProcessingTask task : chunk) {
                    task.calculate();
                }
                //System.out.println( Thread.currentThread().getName() + ": chunk done." );
            }
            catch (Exception e) {
                handleException( e );
            }
        };
        // submit
        boolean success = false;
        for (int waitMillis=10; !success; waitMillis=Math.min( 100, waitMillis*2 ) ) {
           // System.out.println( Thread.currentThread().getName() + ": " + taskCount.availablePermits() );
            success = submitted.add( defaultExecutor.submit( work ) );
        }
    }
    
    
    protected void handleException( Exception e ) {
        exc = exc == null ? e : exc;        
    }

    
    @Override
    public void join() throws Exception {
        if (exc != null) {
            throw exc;
        }
        if (!accu.isEmpty()) {
            submitChunk( accu );
            accu = null;
        }
        for (Future f : submitted) {
            try {
                f.get();
            }
            catch (InterruptedException|CancellationException e) {
                //
            }
        }
    }
}
