/*
 * $Id: Controller.java dba0d06e37a0 2015-04-14 odavid@colostate.edu $
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
package oms3;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import oms3.util.Threads;

/**
 * Execution Controller.
 *
 * @author od odavid@colostate.edu
 *
 */
class Controller {

    static boolean checkCircular = Boolean.getBoolean("oms.check.circular");
    //
    private static final Logger log = Logger.getLogger("oms3.sim");
    /**
     * Execution event Notification
     */
    Notification ens = new Notification(this);
    /* data set */
    Set<FieldContent> dataSet = new LinkedHashSet<FieldContent>();
    /*  All the Commands that have been added to the controller */
    Map<Object, ComponentAccess> oMap = new LinkedHashMap<Object, ComponentAccess>(32);
    /* The compount where this controller belongs to */
    ComponentAccess ca;
    // optional skipping the integrity checking.
    Validator validator;
    //
    Latch latch = new Latch();
    Runnable[] rc;
    final Object lock = new Object();
    //
    // something internal.
    ComponentException E;
    private static ExecutorService es;

    Controller(Object compound) {
        if (checkCircular) {
            validator = new Validator();
        }
        ca = new ComponentAccess(compound, ens);
    }

    ComponentAccess lookup(Object cmd) {
        if (cmd == null) {
            throw new ComponentException("null component.");
        }
        if (cmd == ca.getComponent()) {
            throw new ComponentException("Cannot add the compound to itself " + cmd.toString());
        }
        ComponentAccess w = oMap.get(cmd);
        if (w == null) {
            oMap.put(cmd, w = new ComponentAccess(cmd, ens));
        }
        return w;
    }

    Notification getNotification() {
        return ens;
    }

    /**
     * Map two output fields.
     *
     * @param out the output field name of this component
     * @param comp the component
     * @param comp_out the component output name;
     */
    void mapOut(String out, Object comp, String comp_out) {
        if (comp == ca.getComponent()) {
            throw new ComponentException("cannot connect 'Out' with itself for " + out);
        }
        ComponentAccess ac_dest = lookup(comp);
        FieldAccess destAccess = (FieldAccess) ac_dest.output(comp_out);
        FieldAccess srcAccess = (FieldAccess) ca.output(out);
        checkFA(destAccess, comp, comp_out);
        checkFA(srcAccess, ca.getComponent(), out);

        FieldContent data = srcAccess.getData();
        data.tagLeaf();
        data.tagOut();

        destAccess.setData(data);
        dataSet.add(data);

        if (log.isLoggable(Level.CONFIG)) {
            log.log(Level.CONFIG, String.format("@Out(%s) -> @Out(%s)", srcAccess, destAccess));
        }
//        ens.fireMapOut(srcAccess, destAccess);
    }

    /**
     * Map two input fields.
     *
     * @param in
     * @param comp
     * @param comp_in
     */
    void mapIn(String in, Object comp, String comp_in) {
        if (comp == ca.getComponent()) {
            throw new ComponentException("cannot connect 'In' with itself for " + in);
        }
        ComponentAccess ac_dest = lookup(comp);
        FieldAccess destAccess = (FieldAccess) ac_dest.input(comp_in);
        checkFA(destAccess, comp, comp_in);
        FieldAccess srcAccess = (FieldAccess) ca.input(in);
        checkFA(srcAccess, ca.getComponent(), in);

        FieldContent data = srcAccess.getData();
        data.tagLeaf();
        data.tagIn();

        destAccess.setData(data);
        dataSet.add(data);
        if (log.isLoggable(Level.CONFIG)) {
            log.config(String.format("@In(%s) -> @In(%s)", srcAccess, destAccess));
        }
    }

    /**
     * Directly map a value to a input field.
     *
     * @param val
     * @param to
     * @param to_in
     */
    void mapInVal(Object val, Object to, String to_in) {
        if (val == null) {
            throw new ComponentException("Null value for " + name(to, to_in));
        }
        if (to == ca.getComponent()) {
            throw new ComponentException("field and component ar ethe same for mapping :" + to_in);
        }
        ComponentAccess ca_to = lookup(to);
        Access to_access = ca_to.input(to_in);
        checkFA(to_access, to, to_in);
        ca_to.setInput(to_in, new FieldValueAccess(to_access, val));
        if (log.isLoggable(Level.CONFIG)) {
            log.config(String.format("Value(%s) -> @In(%s)", val.toString(), to_access.toString()));
        }
    }

    /**
     * Map an input field.
     *
     * @param from
     * @param from_field
     * @param to
     * @param to_in
     */
    void mapInField(Object from, String from_field, Object to, String to_in) {
        if (to == ca.getComponent()) {
            throw new ComponentException("wrong connect:" + from_field);
        }
        ComponentAccess ca_to = lookup(to);
        Access to_access = ca_to.input(to_in);
        checkFA(to_access, to, to_in);
        try {
            FieldInvoker f = Utils.fieldInvoker(from, from_field);
            ca_to.setInput(to_in, new FieldObjectAccess(to_access, f, ens));
            if (log.isLoggable(Level.CONFIG)) {
                log.config(String.format("Field(%s) -> @In(%s)", f.toString(), to_access.toString()));
            }

        } catch (Exception E) {
            throw new ComponentException("No such field '" + from.getClass().getCanonicalName() + "." + from_field + "'");
        }
    }

    /**
     * Map a object to an output field.
     *
     * @param from
     * @param from_out
     * @param to
     * @param to_field
     */
    void mapOutField(Object from, String from_out, Object to, String to_field) {
        if (from == ca.getComponent()) {
            throw new ComponentException("wrong connect:" + to_field);
        }
        ComponentAccess ca_from = lookup(from);
        Access from_access = ca_from.output(from_out);
        checkFA(from_access, from, from_out);

        try {
            FieldInvoker f = Utils.fieldInvoker(to, to_field);
            ca_from.setOutput(from_out, new FieldObjectAccess(from_access, f, ens));

            if (log.isLoggable(Level.CONFIG)) {
                log.config(String.format("@Out(%s) -> field(%s)", from_access, f.toString()));
            }
        } catch (Exception E) {
            throw new ComponentException("No such field '" + to.getClass().getCanonicalName() + "." + to_field + "'");
        }
    }

    /**
     * Connect out to in
     *
     * @param from
     * @param from_out
     * @param to
     * @param to_in
     */
    void connect(Object from, String from_out, Object to, String to_in) {
        // add them to the set of commands
        if (from == to) {
            throw new ComponentException("src == dest.");
        }
        if (to_in == null || from_out == null) {
            throw new ComponentException("Some field arguments are null");
        }

        ComponentAccess ca_from = lookup(from);
        ComponentAccess ca_to = lookup(to);

        Access from_access = ca_from.output(from_out);
        checkFA(from_access, from, from_out);
        Access to_access = ca_to.input(to_in);
        checkFA(to_access, to, to_in);

        if (!canConnect(from_access, to_access)) {
            throw new ComponentException("Type/Access mismatch, Cannot connect: " + from + '.' + to_in + " -> " + to + '.' + from_out);
        }

        // src data object
        FieldContent data = from_access.getData();
        data.tagIn();
        data.tagOut();

        dataSet.add(data);
        to_access.setData(data);                       // connect the two

        if (checkCircular) {
            validator.addConnection(from, to);
            validator.checkCircular();
        }

        if (log.isLoggable(Level.CONFIG)) {
            log.config(String.format("@Out(%s) -> @In(%s)", from_access.toString(), to_access.toString()));
        }

//        ens.fireConnect(from_access, to_access);
    }

    /**
     * Feedback connect out to in
     *
     * @param from
     * @param from_out
     * @param to
     * @param to_in
     */
    void feedback(Object from, String from_out, Object to, String to_in) {
        // add them to the set of commands
        if (from == to) {
            throw new ComponentException("src == dest.");
        }
        if (to_in == null || from_out == null) {
            throw new ComponentException("Some field arguments are null");
        }

        ComponentAccess ca_from = lookup(from);
        ComponentAccess ca_to = lookup(to);

        Access from_access = ca_from.output(from_out);
        checkFA(from_access, from, from_out);
        Access to_access = ca_to.input(to_in);
        checkFA(to_access, to, to_in);

        if (!canConnect(from_access, to_access)) {
            throw new ComponentException("Type/Access mismatch, Cannot connect: " + from + '.' + to_in + " -> " + to + '.' + from_out);
        }

        // src data object
        FieldContent data = from_access.getData();
        data.tagIn();
        data.tagOut();

        //      dataSet.add(data);
        to_access.setData(data);                       // connect the two

        ca_from.setOutput(from_out, new AsyncFieldAccess(from_access));
        ca_to.setInput(to_in, new AsyncFieldAccess(to_access));

        if (checkCircular) {
            //   val.addConnection(from, to);
//            val.checkCircular();
        }

        if (log.isLoggable(Level.CONFIG)) {
            log.config(String.format("feedback @Out(%s) -> @In(%s)", from_access.toString(), to_access.toString()));
        }
//        ens.fireConnect(from_access, to_access);
    }

    private static String name(Object o, String field) {
        return o.toString() + "@" + field;
    }

    private static boolean canConnect(Access me, Access other) {
        // first check for plain type compatibility
        if (other.getField().getType().isAssignableFrom(me.getField().getType())) {
            return true;
        }
        // if this fails, let Conversions kick in.
        return Conversions.canConvert(me.getField().getType(), other.getField().getType());
    }

    private static void checkFA(Object fa, Object o, String field) {
        if (fa == null) {
            throw new ComponentException("No such field '" + o.getClass().getCanonicalName() + "." + field + "'");
        }
    }

    void sanityCheck() {
        if (checkCircular) {
//            val.checkInFieldAccess();
            validator.checkOutFieldAccess();
        }
    }

    private static synchronized ExecutorService getExecutorService() {
        if (es == null) {
            es = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 500L,
                    TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    Threads.shutdownAndAwaitTermination(es);
                    if (log.isLoggable(Level.INFO)) {
                        log.info("Executor shutdown.");
                    }
                }
            });
        }
        return es;
    }

    /**
     * Count down latch.
     */
    static private class Latch {

        private int count;
        private final Lock lock = new ReentrantLock();
        private Condition condition = lock.newCondition();

        void load(int count) {
            this.count = count;
        }

        void open() {
            lock.lock();
            try {
                count = 0;
                condition.signal();
            } finally {
                lock.unlock();
            }
        }

        void countDown() {
            lock.lock();
            try {
                if (--count <= 0) {
                    condition.signal();
                }
            } finally {
                lock.unlock();
            }
        }

        void await() throws InterruptedException {
            lock.lock();
            try {
                while (count > 0) {
                    condition.await();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    //
    protected void internalExec() throws ComponentException {
        Collection<ComponentAccess> comps = oMap.values();

        if (comps.isEmpty()) {
            return;        // compound inputs to internals
        }
        try {
            for (Access a : ca.inputs()) {   // map the inputs
                a.out();
            }
        } catch (Exception Ex) {
            throw new ComponentException(Ex, ca.getComponent());
        }

        for (FieldContent dataRef : dataSet) {
            dataRef.invalidate();
        }

        latch.load(comps.size());
        ens.fireStart(ca);
        final ExecutorService executor = getExecutorService();
        if (rc == null) {
            rc = new Runnable[comps.size()];
            int i = 0;
            for (final ComponentAccess co : comps) {
                rc[i++] = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            co.exec();
                            latch.countDown();
                        } catch (ComponentException ce) {
                            synchronized (lock) {
                                if (E == null) {
                                    E = ce;
                                }
                            }
                            latch.open();
                            Threads.shutdownAndAwaitTermination(executor); // ???
                        }
                    }
                };
            }
        }
        if (E == null) {
            for (Runnable r : rc) {
                executor.submit(r);
            }
        }

        try {
            latch.await();
        } catch (InterruptedException IE) {
            // nothing to do here.
        }

        // some of the components left an
        // exception.
        if (E != null) {
            ens.fireException(E);
            throw E;
        }

        try {
            ens.fireFinnish(ca);
            // map the outputs.
//            System.out.println("Comp " + ca.getComponent() + ": " + ca.outputs());
            for (Access a : ca.outputs()) {
                a.in();
            }
        } catch (Exception Ex) {
            throw new ComponentException(Ex, ca.getComponent());
        }
    }

    /**
     * Call an annotated method.
     *
     * @param ann
     */
    void callAnnotated(Class<? extends Annotation> ann, boolean lazy) {
        for (ComponentAccess p : oMap.values()) {
            p.callAnnotatedMethod(ann, lazy);
        }
    }

    /////////////////////////
    /**
     * Validator. The Validator checks for unresolved field access connections,
     * circular references, etc.
     */
    private class Validator {

        // node -> [node1, node2, ...]
        Map<Object, List<Object>> graph = new HashMap<Object, List<Object>>();

        void addConnection(Object from, Object to) {
            List<Object> tos = graph.get(from);
            if (tos == null) {
                tos = new ArrayList<Object>();
                graph.put(from, tos);
            }
            tos.add(to);
        }

        private void check(Object probe, Object current) {
            List<Object> nl = graph.get(current);
            if (nl == null) {
                return;
            }
            for (Object o : nl) {
                if (o == probe) {
                    throw new ComponentException("Circular reference to: " + probe);
                } else {
                    check(probe, o);
                }
            }
        }

        /**
         * Checks the whole graph for circular references that would lead to a
         * deadlock.
         */
        void checkCircular() {
            if (graph.isEmpty()) {
                return;
            }
            for (Object probe : graph.keySet()) {
                check(probe, probe);
            }
        }

        void checkInFieldAccess() {
            for (Access in : ca.inputs()) {
                if (!in.getData().isValid()) {
                    throw new ComponentException("Invalid Access " + in + " -> " + in.getData().access());
                }
            }
            for (ComponentAccess w : oMap.values()) {
                for (Access in : w.inputs()) {
                    if (!in.isValid()) {
                        throw new ComponentException("Missing Connect for: " + in);
                    }
                    if (!in.getData().isValid()) {
                        throw new ComponentException("Invalid Access " + in + " -> " + in.getData().access());
                    }
                }
            }
        }

        void checkOutFieldAccess() {
            if (log.isLoggable(Level.WARNING)) {
                for (ComponentAccess w : oMap.values()) {
                    for (Access out : w.outputs()) {
                        if (!out.isValid()) {
                            log.warning("Empty @Out connect for: " + out);
                        }
                    }
                }
            }
        }
    }
}
