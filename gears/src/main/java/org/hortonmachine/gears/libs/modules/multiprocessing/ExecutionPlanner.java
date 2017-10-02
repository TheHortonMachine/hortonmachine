/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) Falko Bräutigam, polymap.de 
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
package org.hortonmachine.gears.libs.modules.multiprocessing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * An {@link ExecutionPlanner} provides the logic to distribute the processing of
 * many small {@link MultiProcessingTask}s over a limited number of execution
 * threads. This is crucial for efficient parallel execution.
 * 
 * @author Falko Bräutigam
 */
public abstract class ExecutionPlanner {

    static {
        ThreadFactory threadFactory = new ThreadFactory() {
            volatile int threadNumber = 0;
            
            @Override
            public Thread newThread( Runnable r ) {
                Thread t = new Thread( r, "process-worker-" + threadNumber++ );
                t.setDaemon( false );
                //t.setPriority( DEFAULT_THREAD_PRIORITY );
                return t;
            }
        };
        
        int procNum = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor( procNum, procNum, 60L, TimeUnit.SECONDS,
                // with BlockingExecutorService on top we can have unbound queue
                new LinkedTransferQueue(),
               // new LinkedBlockingDeque( procNum ),
               // new SynchronousQueue(),
               // new ArrayBlockingQueue( procNum ) );
                threadFactory );
        
        defaultExecutor = new BlockingExecutorService( threadPool, procNum );
    }
    
    
    /**
     * The default {@link ExecutorService} to be used by all planners.
     * <p/>
     * Set this to change the default. The {@link ExecutorService} must not have an
     * unbound queue or an unlimited number of threads. In other words, the executor
     * has to refuse submits when system resources are running out.
     */
    public static final BlockingExecutorService defaultExecutor; 
    
    /**
     * Set this to change the default planner for all modules.
     */
    public static Supplier<ExecutionPlanner> defaultPlannerFactory = () ->
//            new FixedChunkSizePlanner(); // use this for parallelization
            new InThreadExecutionPlanner(); // use this for no parallelization
    
    /**
     * Creates a new general purpose, default {@link ExecutionPlanner}. This is
     * returned by {@link MultiProcessing#createDefaultPlanner()} by default.
     */
    public static ExecutionPlanner createDefaultPlanner() {
        return defaultPlannerFactory.get();
    }
    
    
    // instance *******************************************
    
    protected int           numberOfTasks = -1;
    
    /**
     * Hints this planner about the total number of task until next {@link #join()}. 
     */
    public ExecutionPlanner setNumberOfTasks( int num ) {
        this.numberOfTasks = num;
        return this;
    }
    
    
    /**
     * Submits the given task for execution.
     * <p/>
     *
     * @param task
     */
    public abstract void submit( MultiProcessingTask task );
    
    
    /**
     * Blocks the calling thread until all sumitted task have completed execution.
     * 
     * @throws Exception If a previously submitted task has thrown an exception then
     *         it is re-thrown by the join() method.
     */
    public abstract void join() throws Exception;
    
}
