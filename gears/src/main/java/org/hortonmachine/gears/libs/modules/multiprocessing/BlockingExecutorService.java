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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Submits up to a maximum count tasks to a delegate {@link ExecutorService}. Blocks
 * submitting thread if it thries to submit more than the maximum number of tasks.
 *
 * @author Falko Bräutigam
 */
public class BlockingExecutorService
        implements ExecutorService {

    private ExecutorService         delegate;
    
    private Semaphore               taskCount;

    /**
     * 
     * 
     * @param delegate
     * @param maxTaskCount The maximum number of tasks to submit to the delegate.
     */
    public BlockingExecutorService( ExecutorService delegate, int maxTaskCount ) {
        this.delegate = delegate;
        this.taskCount = new Semaphore( maxTaskCount );
    }

    protected void beforeSubmit() {
        try {
            taskCount.acquire();
        }
        catch (InterruptedException e) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void execute( Runnable command ) {
        beforeSubmit();
        delegate.execute( () -> {
            try {
                command.run();
            }
            finally {
                taskCount.release();
            }
        });
    }

    @Override
    public <T> Future<T> submit( Callable<T> task ) {
        beforeSubmit();
        return delegate.submit( () -> {
            try {
                return task.call();
            }
            finally {
                taskCount.release();
            }
        });
    }

    @Override
    public <T> Future<T> submit( Runnable task, T result ) {
        beforeSubmit();
        return delegate.submit( () -> {
            try {
                task.run();
            }
            finally {
                taskCount.release();
            }
        }, result );
    }

    @Override
    public Future<?> submit( Runnable task ) {
        beforeSubmit();
        return delegate.submit( () -> {
            try {
                task.run();
            }
            finally {
                taskCount.release();
            }
        });
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination( long timeout, TimeUnit unit ) throws InterruptedException {
        return delegate.awaitTermination( timeout, unit );
    }

    @Override
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> tasks ) throws InterruptedException {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit )
            throws InterruptedException {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public <T> T invokeAny( Collection<? extends Callable<T>> tasks ) throws InterruptedException, ExecutionException {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public <T> T invokeAny( Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit )
            throws InterruptedException, ExecutionException, TimeoutException {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }
    
}
