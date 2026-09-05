/*
 * $Id: ComponentAccess.java 7cba5ba59d73 2018-11-29 francesco.serafin.3@gmail.com $
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

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;
import oms3.annotations.Range;
import oms3.annotations.Role;
import oms3.util.Annotations;

/**
 * Component Access.
 *
 * This class manages reflective access to components internals for the purpose
 * of their integration into a model.
 *
 * @author od (odavid@colostate.edu)
 *
 */
public class ComponentAccess {

    private static final Logger log = Logger.getLogger("oms3.sim");
    /**
     * target component
     */
    Object comp;
    
    // in and out fields
    // name->fieldaccess
    Map<String, Access> ins = new LinkedHashMap<>();
    Map<String, Access> outs = new LinkedHashMap<>();
    
    // execution notification.
    Notification ens;
    /**
     * Execute method.
     */
    final MethodInvoker exec;

    public ComponentAccess(Object cmd) {
        this(cmd, null);
    }

    ComponentAccess(Object comp, Notification ens) {
        this.comp = comp;
        this.ens = ens;

        Method execute = getMethodOfInterest(comp, Execute.class);
        exec = Utils.methodInvoker(comp, execute);
        findAll(comp, ins, outs, ens);
    }

    /**
     * Get the component that is wrapped in this access proxy
     *
     * @return the component
     */
    public Object getComponent() {
        return comp;
    }

    void setInput(String name, Access fa) {
        ins.put(name, fa);
    }

    void setOutput(String name, Access fa) {
        outs.put(name, fa);
    }

    /**
     * Get the all the inputs.
     *
     * @return list of input field access objects
     */
    public Collection<Access> inputs() {
        return ins.values();
    }

    /**
     * Get the all the outputs.
     *
     * @return list of output field assess objects
     */
    public Collection<Access> outputs() {
        return outs.values();
    }

    /**
     * Get a single input field.
     *
     * @param field the name of the field
     * @return the input access object
     */
    public Access input(String field) {
        return ins.get(field);
    }

    /**
     * get a single output field.
     *
     * @param field the name of the field
     * @return the output Field access object
     */
    public Access output(String field) {
        return outs.get(field);
    }

    final void exec() throws ComponentException {
        try {
            ens.fireWait(this);
            // synchonized in()
            for (Access a : ins.values()) {     // wait for all inputs to arrive
                if (a.getClass() == FieldAccess.class) {
                    a.in();
                }
            }
            // un synchonized in()
            for (Access a : ins.values()) {     // wait for all inputs to arrive
                if (a.getClass() == FieldObjectAccess.class || a.getClass() == FieldValueAccess.class
                        || a.getClass() == AsyncFieldAccess.class) {
                    a.in();            // not synchonized.
                }
            }
            
            ens.fireStart(this);
            exec.invoke();                           // execute the object's exec method
            ens.fireFinnish(this);

            // unsynchronized out
            for (Access a : outs.values()) {    // notify for output.
                if (a.getClass() == FieldObjectAccess.class || a.getClass() == AsyncFieldAccess.class) {
                    a.out();
                }
            }
            // synchronized out
            for (Access a : outs.values()) {    // notify for output.
                if (a.getClass() == FieldAccess.class) {
                    a.out();
                }
            }
        } catch (InvocationTargetException ex) {
            throw new ComponentException(ex.getCause(), comp);
        } catch (Exception ex) {
            throw new ComponentException(ex, comp);
        }
    }

    void callAnnotatedMethod(Class<? extends Annotation> ann, boolean lazy) {
        try {
            getMethodOfInterest(comp, ann).invoke(comp);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex.getCause());
        } catch (IllegalArgumentException ex) {
            if (!lazy) {
                throw new RuntimeException(ex.getMessage());
            }
        }
    }

    /**
     * Find the execute method.
     *
     * @Execute or execute() will match in this order.
     *
     * @param cmp the Object where to look for
     * @return the method
     */
    @SuppressWarnings("unchecked")
    private static Method getMethodOfInterest(Object cmp, Class<? extends Annotation> ann) {
        Class cmpClass = cmp.getClass();
        Class infoClass = infoClass(cmpClass);
        Method[] ms = infoClass.getMethods();
        for (Method m : ms) {
            if (m.getAnnotation(ann) != null) {
                if (m.getReturnType() != Void.TYPE || m.getParameterTypes().length > 0) {
                    throw new IllegalArgumentException("Invalid Method signature: " + m);
                }
                try {
                    return cmpClass.getMethod(m.getName());
                } catch (Exception ex) {
                    throw new ComponentException("Cannot find/access method: " + m);
                }
            }
        }
        throw new IllegalArgumentException("No " + ann.getCanonicalName() + " found in " + cmp.getClass());
    }

    private static void findAll(Object cmp, Map<String, Access> ins, Map<String, Access> outs, Notification ens) {
        Class cmpClass = cmp.getClass();
        Class infoClass = infoClass(cmpClass);
        for (Field f : infoClass.getFields()) {
            try {
                if (f.getAnnotation(In.class) != null) {
                    ins.put(f.getName(), new FieldAccess(cmp, cmpClass.getField(f.getName()), ens));
                }
                if (f.getAnnotation(Out.class) != null) {
                    outs.put(f.getName(), new FieldAccess(cmp, cmpClass.getField(f.getName()), ens));
                }
            } catch (Exception ex) {
                throw new ComponentException("Cannot find/access field: " + f);
            }
        }
    }

/// static helper methods
    /**
     * Call an method by Annotation.
     *
     * @param o the object to call.
     * @param ann the annotation
     * @param lazy if true, the a missing annotation is OK. if false the
     * annotation has to be present or a Runtime exception is thrown.
     */
    
    public static void callAnnotated(Object o, Class<? extends Annotation> ann, boolean lazy) {
        try {
            getMethodOfInterest(o, ann).invoke(o);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex.getCause());
        } catch (IllegalArgumentException ex) {
            if (!lazy) {
                throw new RuntimeException(ex.getMessage());
            }
        }
    }

    /**
     * Get the info class for a component object
     *
     * @param cmp the component object
     * @return the class that contains the annotations.
     */
    public static Class infoClass(Class cmp) {
        Class info = null;
        try {
            info = Class.forName(cmp.getName() + "CompInfo");
        } catch (ClassNotFoundException E) {        // there is no info class,
            info = cmp;
        }
        return info;
    }

    /**
     * Adjust the output path.
     *
     * @param outputDir the output directory
     * @param comp the component object
     * @param log the logger
     * @return true is adjusted, false otherwise.
     */
    public static boolean adjustOutputPath(File outputDir, Object comp, Logger log) {
        boolean adjusted = false;
        ComponentAccess cp = new ComponentAccess(comp);
        for (Access in : cp.inputs()) {
            String fieldName = in.getField().getName();
            Class fieldType = in.getField().getType();
            if (fieldType == File.class) {
                Role role = in.getField().getAnnotation(Role.class);
                if (role != null && Annotations.plays(role, Role.OUTPUT)) {
                    try {
                        File f = (File) in.getField().get(comp);
                        if (f != null && !f.isAbsolute()) {
                            f = new File(outputDir, f.getName());
                            in.setFieldValue(f);
                            adjusted = true;
                            if (log.isLoggable(Level.CONFIG)) {
                                log.config("Adjusting output for '" + fieldName + "' to " + f);
                            }
                        }
                    } catch (Exception ex) {
                        throw new ComponentException("Failed adjusting output path for '" + fieldName);
                    }
                }
            }
        }
        return adjusted;
    }

    /**
     * Create a default parameter set
     *
     * @param comp the component object
     * @return the default properties
     */
    public static Properties createDefault(Object comp) {
        Properties p = new Properties();
        ComponentAccess ca = new ComponentAccess(comp);
        // over all input slots.
        for (Access in : ca.inputs()) {
            try {
                String name = in.getField().getName();
                Object o = in.getField().get(comp);
                if (o != null) {
                    String value = o.toString();
                    p.put(name, value);
                }
            } catch (Exception ex) {
                throw new ComponentException("Failed access to field: " + in.getField().getName());
            }
        }
        return p;
    }

    public static String dump(Object comp) throws Exception {
        StringBuilder b = new StringBuilder();
        b.append("//" + comp.toString() + ":\n");
        b.append("// In\n");
        ComponentAccess cp = new ComponentAccess(comp);
        for (Access in : cp.inputs()) {
            String name = in.getField().getName();
            Object val = in.getFieldValue();
            b.append("    " + name + ": " + Conversions.convert(val, String.class) + "\n");
        }
        b.append("// Out\n");
        for (Access in : cp.outputs()) {
            String name = in.getField().getName();
            Object val = in.getFieldValue();
            b.append("    " + name + ": " + Conversions.convert(val, String.class) + "\n");
        }
        b.append("\n");
        return b.toString();
    }

    public static void rangeCheck(Object comp, boolean in, boolean out) throws Exception {
        ComponentAccess cp = new ComponentAccess(comp);
        Collection<Access> acc = new ArrayList<Access>();
        if (in) {
            acc.addAll(cp.inputs());
        }
        if (out) {
            acc.addAll(cp.outputs());
        }
        for (Access a : acc) {
            String name = a.getField().getName();
            Object val = a.getFieldValue();
            Range range = a.getField().getAnnotation(Range.class);
            if (range != null) {
                if (val instanceof Number) {
                    double v = ((Number) val).doubleValue();
                    if (!Annotations.inRange(range, v)) {
                        throw new ComponentException(name + " not in range " + v);
                    }
                } else if (val instanceof double[]) {
                    double[] v = (double[]) val;
                    for (int i = 0; i < v.length; i++) {
                        if (!Annotations.inRange(range, v[i])) {
                            throw new ComponentException(name + " not in range " + v[i]);
                        }
                    }
                }
            }
        }
    }
}
