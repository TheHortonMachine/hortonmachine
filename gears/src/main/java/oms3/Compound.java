/*
 * $Id: Compound.java 7cba5ba59d73 2018-11-29 francesco.serafin.3@gmail.com $
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
import java.util.Map;
import oms3.annotations.*;

/**
 * Abstract Compound Command.
 *
 * @author od
 *
 */
public class Compound {

    private final Controller controller = new Controller(this);


    /**
     * Initializes all components in this compound. Calls all methods tagged as
     * {{@literal @Initialize}} in all components.
     */
    @Initialize
    public void initializeComponents() {
        controller.callAnnotated(Initialize.class, true);
    }


    /**
     * Executes the the Compound.
     *
     * @throws ComponentException extends RuntimeException 
     */
    @Execute
    public void execute() throws ComponentException {
        check();
        internalExec();
    }


    /**
     * Finalizes all components in this compound. Calls all methods tagged as
     * {{@literal @Finalize}} in all components.
     */
    @Finalize
    public void finalizeComponents() {
        controller.callAnnotated(Finalize.class, true);
    }


    /**
     * Connects two internal components with respect to their fields.
     *
     * from/
     *
     * {@literal @Out -> to/}
     * {@literal @In}
     *
     * @param from command object 1
     * @param from_out output field of cmd1
     * @param to command object 2
     * @param to_in input field of cmd2
     */
    public void out2infb(Object from, String from_out, Object to, String to_in) {
        controller.connect(from, from_out, to, to_in);
    }


    /**
     * Connects two internal components with respect to their fields.
     *
     * from/
     *
     * {@literal @Out -> to/}
     * {@literal @In}
     *
     * @param from command object 1
     * @param from_out output field of cmd1
     * @param to command object 2
     * @param to_in input field of cmd2
     */
    public void out2in(Object from, String from_out, Object to, String to_in) {
        controller.connect(from, from_out, to, to_in);
    }


    /**
     * Connects field1 of cmd1 with the same named fields in cmds
     *
     * @param from component1
     * @param from_out field
     * @param tos other components
     */
    public void out2in(Object from, String from_out, Object... tos) {
        for (Object co : tos) {
            out2in(from, from_out, co, from_out);
        }
    }


    /**
     * Feedback connection between two components.
     *
     * @param from the src component
     * @param from_out output field
     * @param to dest component
     * @param to_in in field
     */
    public void feedback(Object from, String from_out, Object to, String to_in) {
        controller.feedback(from, from_out, to, to_in);
    }


    /**
     * Feedback connection between two components.
     *
     * @param from the src component
     * @param from_out output field
     * @param tos dest components
     */
    public void feedback(Object from, String from_out, Object... tos) {
        for (Object co : tos) {
            feedback(from, from_out, co, from_out);
        }
    }


    /**
     * Maps a Compound Input field to a internal simple input field.
     *
     * @param in Compound input field.
     * @param to internal Component
     * @param to_in Input field of the internal component
     */
    public void in2in(String in, Object to, String to_in) {
        controller.mapIn(in, to, to_in);
    }


    /**
     * Maps a compound input to an internal simple input field. Both fields have
     * the same name.
     *
     * @param in the name of the field
     * @param to the commands to map to
     */
    public void in2in(String in, Object... to) {
        String[] vars = in.trim().split("\\s+");
        for (String var : vars) {
            for (Object comp : to) {
                in2in(var, comp, var);
            }
        }
    }


    /**
     * Maps a field to an In and Out field
     *
     * @param o the object
     * @param field the field name
     * @param comp the component
     * @param inout the field tagged with In and Out
     */
    public void field2inout(Object o, String field, Object comp, String inout) {
        controller.mapInField(o, field, comp, inout);
        controller.mapOutField(comp, inout, o, field);
    }


    /**
     * Maps a field to an In and Out field
     *
     * @param o the object
     * @param field the field name
     * @param comp the component
     */
    public void field2inout(Object o, String field, Object comp) {
        field2inout(o, field, comp, field);
    }


    /**
     * Maps a object's field to an In field
     *
     * @param o the object
     * @param field the field name
     * @param to the component
     * @param to_in the In field.
     */
    public void field2in(Object o, String field, Object to, String to_in) {
        controller.mapInField(o, field, to, to_in);
    }


    /**
     * Maps an object's field to a component's In field with the same name
     *
     * @param o the object
     * @param field the field name
     * @param to the component.
     */
    public void field2in(Object o, String field, Object to) {
        field = field.trim();
        if (field.indexOf(' ') > 0) {           // maybe multiple field names given
            String[] fields = field.split("\\s+");
            for (String f : fields) {
                field2in(o, f, to, f);
            }
        } else {
            field2in(o, field, to, field);
        }
    }


    /**
     * Maps a component's Out field to an object field.
     *
     * @param from the component
     * @param from_out the component's out field
     * @param o the object
     * @param field the object's field
     */
    public void out2field(Object from, String from_out, Object o, String field) {
        controller.mapOutField(from, from_out, o, field);
    }


    /**
     * Maps a component Out field to an object's field. Both field have the same
     * name.
     *
     * @param from the component
     * @param from_out the component's Out field.
     * @param o the object
     */
    public void out2field(Object from, String from_out, Object o) {
        out2field(from, from_out, o, from_out);
    }


    /**
     * Maps a Compound Output field to a internal simple output field.
     *
     * @param out Compound output field.
     * @param to internal Component
     * @param to_out output field of the internal component
     */
    public void out2out(String out, Object to, String to_out) {
        controller.mapOut(out, to, to_out);
    }


    /**
     * Map output maps a compound output to an internal simple output field.
     * Both fields have the same name.
     *
     * @param out the name of the field
     * @param to the component source.
     */
    public void out2out(String out, Object to) {
        out2out(out, to, out);
    }


    /**
     * Connects all of the out fields of the "from" object to the all the in
     * fields of a "to" object if both fields have the same name.
     *
     * @param from component with out fields
     * @param tos components with in fields
     */
    public void allout2in(Object from, Object... tos) {
        ComponentAccess fromCA = controller.lookup(from);
        for (Object to : tos) {
            ComponentAccess toCA = controller.lookup(to);
            for (Access fromAccess : fromCA.outputs()) {
                String fieldName = fromAccess.getField().getName();
                Access toAccess = toCA.input(fieldName);
                // !toAccess.isValid() makes sure that the field is not already
                // connected thus preventing destruction of other connections
                if (toAccess != null && !toAccess.isValid()) {
                    out2in(from, fieldName, to, fieldName);
                }
            }
        }
    }


    /**
     * Connects all of the in fields of this object to the all the in fields of
     * a "to" object if both fields have the same name.
     *
     * @param tos to components with in fields
     */
    public void allin2in(Object... tos) {
        ComponentAccess fromCA = controller.ca;
        for (Object to : tos) {
            ComponentAccess toCA = controller.lookup(to);
            for (Access fromAccess : fromCA.inputs()) {
                String fieldName = fromAccess.getField().getName();
                Access toAccess = toCA.input(fieldName);
                // !toAccess.isValid() makes sure that the field is not already
                // connected thus preventing destruction of other connections
                if (toAccess != null && !toAccess.isValid()) {
                    in2in(fieldName, to, fieldName);
                }
            }
        }
    }


    /**
     * Connects all of the fields of the "from" object to the all the in/out
     * fields of a "to" object if both fields have the same name.
     *
     * @param from container
     * @param tos components with in fields
     */
    public void all2inout(Object from, Object... tos) {
        Field[] fromFields = from.getClass().getDeclaredFields();
        Map<String, Field> fromFieldMap = Utils.getFieldMap(fromFields);
        for (Object to : tos) {
            ComponentAccess toCA = controller.lookup(to);

            // Connect all fields to ins
            all2in(from, to, fromFields, toCA);

            // Connect all outs to fields
            out2all(to, from, toCA, fromFieldMap);
        }
    }


    /**
     * Connects all of the fields of the "from" object to the all the in fields
     * of a "to" object if both fields have the same name.
     *
     * @param from container
     * @param tos components with in fields
     */
    public void all2in(Object from, Object... tos) {
        Field[] fromFields = from.getClass().getDeclaredFields();
        for (Object to : tos) {
            ComponentAccess toCA = controller.lookup(to);
            all2in(from, to, fromFields, toCA);
        }
    }


    /**
     * Connects all of the out fields of this object to the all the out fields
     * of a "to" object if both fields have the same name.
     *
     * @param tos to components with in fields
     */
    public void allout2out(Object... tos) {
        ComponentAccess fromCA = controller.ca;
        for (Object to : tos) {
            ComponentAccess toCA = controller.lookup(to);
            for (Access fromAccess : fromCA.outputs()) {
                String fieldName = fromAccess.getField().getName();
                Access toAccess = toCA.output(fieldName);
                // !toAccess.isValid() makes sure that the field is not already
                // connected thus preventing destruction of other connections
                if (toAccess != null && !toAccess.isValid()) {
                    out2out(fieldName, to, fieldName);
                }
            }
        }
    }


    /**
     * Connects all of the out fields of the "from" object to the all the fields
     * of a "to" object if both fields have the same name.
     *
     * @param from component with out fields
     * @param tos containers
     */
    public void out2all(Object from, Object... tos) {
        ComponentAccess fromCA = controller.lookup(from);
        for (Object to : tos) {
            Field[] toFields = to.getClass().getDeclaredFields();
            Map<String, Field> toFieldMap = Utils.getFieldMap(toFields);
            out2all(from, to, fromCA, toFieldMap);
        }
    }


    private void all2in(Object from, Object to, Field[] fromFields, ComponentAccess toCA) {
        for (Field fromField : fromFields) {
            String fieldName = fromField.getName();
            Access toAccess = toCA.input(fieldName);
            // !toAccess.isValid() makes sure that the field is not already
            // connected thus preventing destruction of other connections
            if (toAccess != null && !toAccess.isValid()) {
                field2in(from, fieldName, to, fieldName);
            }
        }
    }


    private void out2all(Object from, Object to, ComponentAccess fromCA, Map<String, Field> toFieldMap) {
        for (Access fromAccess : fromCA.outputs()) {
            String fieldName = fromAccess.getField().getName();
            Field toField = toFieldMap.get(fieldName);
            // An output can go to multiple fields without being destructive.
            if (toField != null) {
                out2field(from, fieldName, to, fieldName);
            }
        }
    }


    public void val2in(boolean val, Object to, String field) {
        controller.mapInVal(val, to, field);
    }


    public void val2in(char val, Object to, String field) {
        controller.mapInVal(new Character(val), to, field);
    }


    public void val2in(byte val, Object to, String field) {
        controller.mapInVal(new Byte(val), to, field);
    }


    public void val2in(short val, Object to, String field) {
        controller.mapInVal(new Short(val), to, field);
    }


    public void val2in(int val, Object to, String field) {
        controller.mapInVal(new Integer(val), to, field);
    }


    public void val2in(long val, Object to, String field) {
        controller.mapInVal(new Long(val), to, field);
    }


    public void val2in(float val, Object to, String field) {
        controller.mapInVal(new Float(val), to, field);
    }


    public void val2in(double val, Object to, String field) {
        controller.mapInVal(new Double(val), to, field);
    }


    public void val2in(Object val, Object to, String field) {
        controller.mapInVal(val, to, field);
    }


    /**
     * Check for valid internals within the compound
     */
    protected void check() {
        controller.sanityCheck();
    }


    /**
     * Internal execution.
     *
     * @throws ComponentException component RuntimeException
     */
    protected void internalExec() throws ComponentException {
        controller.internalExec();
    }


    /**
     * Add a ExecutionListener that tracks execution
     *
     * @param l the Listener to add
     */
    public void addListener(Notification.Listener l) {
        controller.getNotification().addListener(l);
    }


    /**
     * Remove a ExecutionListener that tracks execution
     *
     * @param l the Listener to remove
     */
    public void removeListerer(Notification.Listener l) {
        controller.getNotification().removeListener(l);
    }
}
