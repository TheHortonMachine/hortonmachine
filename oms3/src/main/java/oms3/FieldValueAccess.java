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
//import oms3.gen.Access;

/** Field Access.
 * 
 * @author Olaf David (olaf.david@ars.usda.gov)
 * @version $Id$ 
 */
class FieldValueAccess implements Access {

    Object data;
    Access fa;

//    Access access;
   
    FieldValueAccess(Access fa, Object data) {
        this.fa = fa;
        this.data = data;
    }

    /**
     * Checks if this object is in a valid state.
     * @return
     */
    @Override
    public boolean isValid() {
        return data != null && fa.isValid();
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
        Object val = data;
        // fire only if there is a listener
//        if (ens.shouldFire()) {
//            DataflowEvent e = new DataflowEvent(ens.getController(), this, val);
////            DataflowEvent e = new DataflowEvent(ens.getController(), this, access.toObject());
//            ens.fireIn(e);
//            // the value might be altered
//            val = e.getValue();
//        }

//        access.pass((Access) val);
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
//        Object val = access;

//        if (ens.shouldFire()) {
//            DataflowEvent e = new DataflowEvent(ens.getController(), this, val);
////            DataflowEvent e = new DataflowEvent(ens.getController(), this, access.toObject());
//            ens.fireOut(e);
//            // the value might be altered
//            val = e.getValue();
//        }
        // if data==null this unconsumed @Out, its OK but we do not want to set it.
            data = val;
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
