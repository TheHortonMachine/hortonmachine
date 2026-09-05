/*
 * $Id: Threads.java 6124abb33495 2014-09-10 odavid@colostate.edu $
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
package oms3.util;

import java.util.ArrayList;
import java.util.Iterator;
import oms3.Compound;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import oms3.ComponentAccess;
import oms3.annotations.Finalize;
import oms3.annotations.Initialize;

/**
 * Compound Execution utilities.
 *
 * @author od
 *
 */
public class Threads {

    public static abstract class CompList<T> implements Iterable<Compound> {

        List<T> list;
        public List<Compound> p;

        public CompList(List<T> list) {
            this.list = list;
        }

        public abstract Compound create(T src);

        List<Compound> list() {
            if (p == null) {
                p = new ArrayList<Compound>();
                for (T el : list) {
                    p.add(create(el));
                }
            }
            return p;
        }

        @Override
        public Iterator<Compound> iterator() {
            return list().iterator();
        }
    }

    public static void seq_e(CompList<?> t) {
        for (Compound c : t) {
            c.execute();
        }
    }

    public static void seq_ief(CompList<?> t) throws Exception {
        for (Compound c : t) {
            ComponentAccess.callAnnotated(c, Initialize.class, true);
            c.execute();
            ComponentAccess.callAnnotated(c, Finalize.class, true);
        }
    }

    public static void par_ief(CompList<?> t) throws Exception {
        par_ief(t, Runtime.getRuntime().availableProcessors() + 1);
    }
//    public static ExecutorService e = Executors.newFixedThreadPool(
//            Runtime.getRuntime().availableProcessors() + 1);
//    private static ExecutorService es;
    private static ThreadPoolExecutor es;

    private static synchronized ExecutorService getES() {
        if (es == null) {
            int nthreads = Runtime.getRuntime().availableProcessors() + 1;
            es = new ThreadPoolExecutor(nthreads, nthreads, 500L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
            es.allowCoreThreadTimeOut(true);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    shutdownAndAwaitTermination(es);
                }
            });
        }
        return es;
    }

    /**
     * Runs a set of Compounds in parallel. there are always numproc + 1 threads
     * active.
     *
     * @param comp
     * @param numproc
     * @throws java.lang.Exception
     */
    private static void par_ief(CompList<?> t, int numproc) throws Exception {
        if (numproc < 1) {
            throw new IllegalArgumentException("numproc");
        }
        final CountDownLatch latch = new CountDownLatch(t.list().size());
        final ExecutorService es = getES();

        for (final Compound c : t) {
            es.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        ComponentAccess.callAnnotated(c, Initialize.class, true);
                        c.execute();
                        ComponentAccess.callAnnotated(c, Finalize.class, true);
                    } catch (Throwable E) {
                        shutdownAndAwaitTermination(es);
                    }
                    latch.countDown();
                }
            });
        }
        latch.await();
    }

    public static void par_e(CompList<?> t) throws Exception {
        par_e(t, Runtime.getRuntime().availableProcessors() + 1);
    }

    public static void par_e(CompList<?> t, int numproc) throws Exception {
        if (numproc < 1) {
            throw new IllegalArgumentException("numproc");
        }
        final CountDownLatch latch = new CountDownLatch(t.list().size());
        final ExecutorService es = getES();
        for (final Compound c : t) {
            es.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        c.execute();
                    } catch (Throwable E) {
                        shutdownAndAwaitTermination(es);
                    }
                    latch.countDown();
                }
            });
        }
        latch.await();
    }

    public static void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(50, TimeUnit.MILLISECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(50, TimeUnit.MILLISECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

//    public static final Iterable<Compound> it(final Iterator<Compound> i) {
//        return new Iterable<Compound>() {
//
//            @Override
//            public Iterator<Compound> iterator() {
//                return i;
//            }
//        };
//    }
}
