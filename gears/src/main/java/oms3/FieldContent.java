/*
 * $Id: FieldContent.java 2fb4d513ea17 2014-06-04 odavid@colostate.edu $
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

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Generic Data Object for exchange
 *
 * @author od
 *
 */
public class FieldContent {

    /* The null object */
    private static final Object NULL = new Object();

    /* No current acess */
    private static final int NONE = 0;
    /* Input access */
    private static final int IN = 1;
    /* Output access */
    private static final int OUT = 2;
    /* leaf */
    private static final int LEAF = 4;
    /* In and out access */
    private static final int IO = (IN | OUT);
    /* In and leaf */
    private static final int LEAF_IN = (IN | LEAF);
    /* Out and leaf */
    private static final int LEAF_OUT = (OUT | LEAF);
    //
    /* Actual access. */
    private byte access = NONE;
    /* The value of the object */
    //
    private Object value;
    private Object shadow; // for now
    //
    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    // Invalidate the value in between iterations.
    // invalidate only if the input can be generated again.
    final void invalidate() {
        if ((access & LEAF_IN) == LEAF_IN) {
            return;
        }
        value = NULL;
    }

    /**
     * Set the value object. Notifies all threads that are waiting on the
     * getValue call
     *
     * @param value the value object.
     */
    void setValue(Object value) {
        lock.lock();
        try {
            this.value = value;
            shadow = value;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Unsynchronized setValue
     *
     * @param value
     */
    void setValue0(Object value) {
        this.value = value;
        shadow = value;
    }

    /**
     * Get the value object. This call blocks the caller until a value is set
     * (!=null).
     *
     * @return the value
     */
    Object getValue() {
        lock.lock();
        try {
            while (value == NULL) {
                condition.await();
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } finally {
            lock.unlock();
            return value;
        }
    }

    /**
     * Unsynchronized getValue
     *
     * @return
     */
    Object getValue0() {
        return value;
    }

    Object getShadow() {
        return shadow;
    }

    void tagIn() {
        access |= IN;
    }

    void tagOut() {
        access |= OUT;
    }

    void tagLeaf() {
        access |= LEAF;
    }

    int access() {
        return access;
    }

    boolean isValid() {
        return access == IO || access == LEAF_IN || access == OUT || access == 7;
    }
}
