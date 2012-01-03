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
package oms3;

import java.lang.reflect.Field;

/**
 * Generic Data Object for exchange
 * 
 * @author Olaf David (olaf.david@ars.usda.gov)
 * @version $Id$ 
 */
public class FieldContent {


    public static class FA {

        Field field;
        Object obj;

        FA(Object obj, String name) throws Exception {
            this.obj = obj;
            field = obj.getClass().getField(name);
        }

        Object getFieldValue() throws Exception {
            return field.get(obj);
        }

        void setFieldValue(Object o) throws Exception {
            field.set(obj, o);
        }
    }

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

    // Invalidate the value in between iterations.
    // invalidate only if the input can be generated again.
    final void invalidate() {
        if ((access & LEAF_IN) == LEAF_IN) {
            return;
        }
        value = NULL;
    }

    /**
     * Set the value object. Notifies all threads that are
     * waiting on the getValue call
     * @param value the value object.
     */
    synchronized void setValue(Object value) {
        this.value = value;
        shadow = value;
        notifyAll();
    }

    /**
     * Unsynchronized setValue
     * @param value
     */
    void setValue0(Object value) {
        this.value = value;
        shadow = value;
    }

    /** Get the value object. This call blocks the caller until
     *  a value is set (!=null).
     * 
     * @return the value
     */
    synchronized Object getValue() {
        while (value == NULL) {
            try {
                wait();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
        return value;
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
