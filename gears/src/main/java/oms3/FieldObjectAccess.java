/*
 * $Id: FieldObjectAccess.java 2fb4d513ea17 2014-06-04 odavid@colostate.edu $
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

import java.lang.reflect.Field;
import oms3.Notification.DataflowEvent;

/** Field Access.
 * 
 * @author od  
 *   
 */
class FieldObjectAccess implements Access {
    
    Notification ens;
    FieldInvoker data;
    Access fa;

    FieldObjectAccess(Access fa, FieldInvoker data, Notification ens) {
        this.fa = fa;
        this.data = data;
        this.ens = ens;
    }

    /**
     * Checks if this object is in a valid state.
     * @return
     */
    @Override
    public boolean isValid() {
        return data != null;
    }

    /** 
     * a field is receiving a new value (in)
     * 
     * @throws java.lang.Exception
     */
    @Override
    public void in() throws Exception {
        if (data == null) {
            throw new ComponentException("Not connected: " + toString());
        }
        Object val = data.getValue();
        // fire only if there is a listener
        if (ens.shouldFire()) {
            DataflowEvent e = new DataflowEvent(ens.getController(), this, val);
            ens.fireIn(e);
            // the value might be altered
            val = e.getValue();
        }

        fa.setFieldValue(val);
    }

    /** 
     * a field is sending a new value (out)
     * 
     * @throws java.lang.Exception
     */
    @Override
    public void out() throws Exception {
        Object val = fa.getFieldValue();
        if (ens.shouldFire()) {
            DataflowEvent e = new DataflowEvent(ens.getController(), this, val);
//            DataflowEvent e = new DataflowEvent(ens.getController(), this, access.toObject());
            ens.fireOut(e);
            // the value might be altered
            val = e.getValue();
        }
        // if data==null this unconsumed @Out, its OK but we do not want to set it.
        if (data != null) {
            data.setValue(val);
        }
        fa.out();
    }

    /** Get the command belonging to this Object
     *
     * @return the command object
     */
    @Override
    public Object getComponent() {
        return fa.getComponent();
    }

    /**
     * Get the Field
     * @return the field object.
     */
    @Override
    public Field getField() {
        return fa.getField();
    }

    @Override
    public String toString() {
        return "FieldObjectAccess [" + fa.toString() + " - " + data;
    }

     @Override
    public Object getFieldValue() throws Exception {
        return fa.getFieldValue();
    }

    @Override
    public void setFieldValue(Object o) throws Exception {
        fa.setFieldValue(o);
    }

    @Override
    public FieldContent getData() {
        return fa.getData();
    }

    @Override
    public void setData(FieldContent data) {
        fa.setData(data);
    }
}
