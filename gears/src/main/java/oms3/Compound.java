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

import oms3.annotations.*;


/**
 * Abstract Compound Command.
 * 
 * @author od
 * @version $Id$ 
 */
public  class Compound {

    private Controller controller = new Controller(this);

    @Initialize
    /** Initializes all components in this compound.
     * Calls all methods tagged as {{@Initialize}} in all
     * components.
     */
    public void initializeComponents() {
        controller.callAnnotated(Initialize.class, true);
    }

    @Finalize
    /** Finalizes all components in this compound.
     * Calls all methods tagged as {{@Finalize}} in all
     * components.
     */
    public void finalizeComponents() {
        controller.callAnnotated(Finalize.class, true);
    }


    @Execute
    /** Executes the the Compound.
     * @throws ComponentException
     */
    public void execute() throws ComponentException {
        check();
        internalExec();
    }

    /** Shutting down the execution service
     * 
     */
    public static void shutdown() {
        Controller.shutdown();
    }

    public static void reload() {
        Controller.reload();
    }


    /** Connects two internal components with respect to their fields.
     *
     * from/@Out -> to/@In
     *
     * @param from command object 1
     * @param from_out output field of cmd1
     * @param to command object 2
     * @param to_in input field of cmd2
     */
    public void out2infb(Object from, String from_out, Object to, String to_in) {
        controller.connect(from, from_out, to, to_in);
    }

    /** Connects two internal components with respect to their fields.
     *
     * from/@Out -> to/@In
     *
     * @param from command object 1
     * @param from_out output field of cmd1
     * @param to command opbject 2
     * @param to_in input field of cmd2
     */
    public void out2in(Object from, String from_out, Object to, String to_in) {
        controller.connect(from, from_out, to, to_in);
    }


    /** Connects field1 of cmd1 with the same named fields in cmds
     * @param from component1
     * @param from_out field
     * @param tos other components
     */
    public void out2in(Object from, String from_out, Object... tos) {
        for (Object co : tos) {
            out2in(from, from_out, co, from_out);
        }
    }

    /** Feedback connection between two components.
     *
     * @param from the src component
     * @param from_out output field
     * @param to       dest component
     * @param to_in    in field
     */
    public void feedback(Object from, String from_out, Object to, String to_in) {
        controller.feedback(from, from_out, to, to_in);
    }

    /** Feedback connection between two components.
     *
     * @param from
     * @param from_out
     * @param tos
     */
    public void feedback(Object from, String from_out, Object... tos) {
        for (Object co : tos) {
            feedback(from, from_out, co, from_out);
        }
    }

    /** Maps a Compound Input field to a internal simple input field.
     *
     * @param in Compound input field.
     * @param to   internal Component
     * @param to_in  Input field of the internal component
     */
    public void in2in(String in, Object to, String to_in) {
        controller.mapIn(in, to, to_in);
    }

    /** Maps a compound input to an internal simple input field. Both
     * fields have the same name.
     * @param in the name of the field
     * @param to the commands to map to
     */
    public void in2in(String in, Object... to) {
        for (Object cmd : to) {
            in2in(in, cmd, in);
        }
    }

    /** Maps a field to an In and Out field
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

    /** Maps a field to an In and Out field
     *
     * @param o the object
     * @param field the field name
     * @param comp the component
     */
    public void field2inout(Object o, String field, Object comp) {
        field2inout(o, field, comp, field);
    }

    /** Maps a object's field to an In field
     *
     * @param o the object
     * @param field the field name
     * @param to the component
     * @param to_in the In field.
     */
    public void field2in(Object o, String field, Object to, String to_in) {
        controller.mapInField(o, field, to, to_in);
    }

    /** Maps an object's field to a component's In field with the same name
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


    /** Maps a component's Out field to an object field.
     *
     * @param from the component
     * @param from_out the component's out field
     * @param o the object
     * @param field the object's field
     */
    public void out2field(Object from, String from_out, Object o, String field) {
        controller.mapOutField(from, from_out, o, field);
    }

    /** Maps a component Out field to an object's field. Both field have the
     *  same name.
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
     * @param out Compount output field.
     * @param to   internal Component
     * @param to_out  output field of the internal component
     */
    public void out2out(String out, Object to, String to_out) {
        controller.mapOut(out, to, to_out);
    }

    /**
     * Map output
     * maps a compound output to an internal simple output field. Both
     * fields have the same name.
     *
     * @param out tha name of the field
     * @param to the component source.
     */
    public void out2out(String out, Object to) {
        out2out(out, to, out);
    }

    public void val2in(boolean val, Object to, String field) {
        controller.mapInVal(new Boolean(val), to, field);
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



    // deprecated methods starting here.
    
    @Deprecated
    public void connect(Object from, String from_out, Object to, String to_in) {
        controller.connect(from, from_out, to, to_in);
    }

    @Deprecated
    public void connect(Object from, String from_out, Object... tos) {
        for (Object co : tos) {
            connect(from, from_out, co, from_out);
        }
    }

    @Deprecated
    public void mapIn(String in, Object to, String to_in) {
        controller.mapIn(in, to, to_in);
    }

    @Deprecated
    public void mapIn(String in, Object... to) {
        for (Object cmd : to) {
            mapIn(in, cmd, in);
        }
    }

    @Deprecated
    public void mapInField(Object o, String out, Object from, String from_out) {
        controller.mapInField(o, out, from, from_out);
    }

    @Deprecated
    public void mapInField(Object o, String out, Object from) {
        controller.mapInField(o, out, from, out);
    }
    
    @Deprecated
    public void mapOutField(Object o, String out, Object from, String from_out) {
        controller.mapOutField(o, out, from, from_out);
    }

    @Deprecated
    public void mapOutField(Object o, String out, Object from) {
        controller.mapOutField(o, out, from, out);
    }

    @Deprecated
    public void mapOut(String out, Object from, String from_out) {
        controller.mapOut(out, from, from_out);
    }

    @Deprecated
    public void mapOut(String out, Object from) {
        mapOut(out, from, out);
    }


    /**
     * Check for valid internals within the compound
     */
    protected  void check() {
        controller.sanityCheck();
    }

    /**
     * Internal execution.
     * 
     * @throws ComponentException
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
     * @param l the Listener to remove
     */
    public void removeListerer(Notification.Listener l) {
        controller.getNotification().removeListener(l);
    }
}
