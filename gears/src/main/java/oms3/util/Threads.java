/*
 * $Id$
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software
 *     in a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 * 
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 * 
 *  3. This notice may not be removed or altered from any source
 *     distribution.
 */
package oms3.util;

import java.util.ArrayList;
import java.util.Iterator;
import oms3.Compound;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import oms3.ComponentAccess;
import oms3.annotations.Finalize;
import oms3.annotations.Initialize;

/**
 * Compound Execution utilities.
 * 
 * @author Olaf David
 * @version $Id$ 
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

    public static ExecutorService e = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() + 1);

    /**
     * Runs a set of Compounds in parallel. there are always numproc + 1 threads active. 
     * @param comp
     * @param numproc
     * @throws java.lang.Exception
     */
     private static void par_ief(CompList<?> t, int numproc) throws Exception {
        if (numproc < 1) {
            throw new IllegalArgumentException("numproc");
        }
        final CountDownLatch latch = new CountDownLatch(t.list().size());
//        final ExecutorService e = Executors.newFixedThreadPool(numproc);
        for (final Compound c : t) {
            e.submit(new Runnable() {

                @Override
                public void run() {
                    try {
                        ComponentAccess.callAnnotated(c, Initialize.class, true);
                        c.execute();
                        ComponentAccess.callAnnotated(c, Finalize.class, true);
                    } catch (Throwable E) {
                        e.shutdownNow();
                    }
                    latch.countDown();
                }
            });
        }
        latch.await();
//        e.shutdown();
    }

    public static void par_e(CompList<?> t) throws Exception {
        par_e(t, Runtime.getRuntime().availableProcessors() + 1);
    }
    
    public static void par_e(CompList<?> t, int numproc) throws Exception {
        if (numproc < 1) {
            throw new IllegalArgumentException("numproc");
        }
        final CountDownLatch latch = new CountDownLatch(t.list().size());
//        final ExecutorService e = Executors.newFixedThreadPool(numproc);
        for (final Compound c : t) {
            e.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        c.execute();
                    } catch (Throwable E) {
                        e.shutdownNow();
                    }
                    latch.countDown();
                }
            });
        }
        latch.await();
//        e.shutdown();
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
