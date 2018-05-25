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

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import oms3.annotations.Bound;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;
import oms3.annotations.Range;
import oms3.annotations.Role;
import oms3.gen.MethodInvoker;
import oms3.util.Annotations;

/** 
 * Component Access.
 * 
 * This class manages reflective access to components internals for the
 * purpose of their integration into a model.
 * 
 * @author od (odavid@colostate.edu)
 * @version $Id$ 
 */
public class ComponentAccess {

    private static final Logger log = Logger.getLogger("oms3.sim");
    /** target component */
    Object comp;
    // in and out fields
    // name->fieldaccess
    Map<String, Access> ins = new LinkedHashMap<String, Access>();
    Map<String, Access> outs = new LinkedHashMap<String, Access>();
    // execution notification.
    Notification ens;
    /** Execute method. */
    final MethodInvoker exec;

//    public static int counter;
//    static final Object lock = new Object();
//    static void inc() {
//        synchronized (lock) {
//            counter++;
//        }
//    }
    
    public ComponentAccess(Object cmd) {
        this(cmd, null);
    }

    ComponentAccess(Object comp, Notification ens) {
        this.comp = comp;
        this.ens = ens;
        
        Method execute = getMethodOfInterest(comp, Execute.class);
        exec = Utils.reflective(comp, execute);
//        exec = Utils.compiled(comp, execute);
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
     * @return list of input field access objects
     */
    public Collection<Access> inputs() {
        return ins.values();
    }

    /**
     * Get the all the outputs.
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
     * @param field
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

    /** Find the execute method.
     *   @Execute or execute() will match in this order.
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
//                    ins.put(f.getName(), new FieldAccess(cmp, f, ens));
                }
                if (f.getAnnotation(Out.class) != null) {
                    outs.put(f.getName(), new FieldAccess(cmp, cmpClass.getField(f.getName()), ens));
//                    outs.put(f.getName(), new FieldAccess(cmp, f, ens));
                }
            } catch (Exception ex) {
                throw new ComponentException("Cannot find/access field: " + f);
            }
        }
    }

/// static helper methods
    /** Call an method by Annotation.
     *
     * @param o the object to call.
     * @param ann the annotation
     * @param lazy if true, the a missing annotation is OK. if false
     *        the annotation has to be present or a Runtime exception is thrown.
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
     * @param cmp
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
     * @param outputDir
     * @param comp
     * @param log
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

    /** Create a default parameter set
     *
     * @param comp
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

    static Map<String, Object> convert(Map<String, Object> m) {
        LinkedHashMap<String, Object> hm = new LinkedHashMap<String, Object>();
        Set<Entry<String, Object>> entrySet = m.entrySet();
        for (Entry<String, Object> entry : entrySet) {
            String key = entry.getKey();
            key = key.replace('.', '_');
            Object value = entry.getValue();
            hm.put(key, value);
        }
        return hm;
    }

    public static Object conv(Object inpValue, Class<?> fieldType) {
        Class inpType = inpValue.getClass();
        if (inpType == String.class && fieldType != String.class) {
            inpValue = Conversions.convert((String) inpValue, fieldType);
        } else if (inpType == BigDecimal.class && fieldType != BigDecimal.class) {
            inpValue = Conversions.convert(inpValue.toString(), fieldType);
        } else if (inpValue instanceof CharSequence) {
            inpValue = Conversions.convert(inpValue.toString(), fieldType);
        }
        return inpValue;
    }

    /**
     * Set the input data as map.
     * @param inp
     * @param comp
     * @param log
     */
    @SuppressWarnings("unchecked")
    public static boolean setInputData(Map<String, Object> inp, Object comp, Logger log) {
        PrintWriter w = null;
        File file = null;
        boolean success = true;
        ComponentAccess cp = new ComponentAccess(comp);
        inp = convert(inp);
        for (Access in : cp.inputs()) {
            String fieldName = in.getField().getName();
            Class fieldType = in.getField().getType();
            Object inpValue = inp.get(fieldName);
            if (inpValue != null) {
                // allow files and dates provided as strings and
                // convert them
                try {
                    inpValue = conv(inpValue, fieldType);
                    // check the range if possible.
                    if (Number.class.isAssignableFrom(fieldType) || fieldType == double.class || fieldType == float.class || fieldType == int.class) {
                        Range range = in.getField().getAnnotation(Range.class);
                        if (range != null) {
                            double v = ((Number) inpValue).doubleValue();
                            if (!Annotations.inRange(range, v)) {
                                if (log.isLoggable(Level.WARNING)) {
                                    log.warning("Value '" + v + "' not in Range: " + range);
                                }
                            }
                        }
                    }
                    in.setFieldValue(inpValue);
                    if (log.isLoggable(Level.CONFIG)) {
                        log.config("@In " + comp.getClass().getName() + "@" + fieldName + " <- '" + inpValue + "'");
                    }
                } catch (Exception ex) {
                    throw new ComponentException("Failed setting '" + fieldName + "' type " + in.getField().getType().getCanonicalName() + " <- " + ex.getMessage());
                }
                continue;
            } else {
                if (System.getProperty("oms.check_params") != null) {
                    try {
                        if (w == null) {
                            file = new File(System.getProperty("oms3.work", System.getProperty("user.dir")), "missing_params.csv");
                            w = new PrintWriter(new FileWriter(file));
                            w.println("# Missing parameter, copy those entries into one of your parameter files.");
                        }
                        String val = null;
                        Bound b = null;
                        Object o = in.getFieldValue();
                        if (o != null) {
                            val = o.toString();
                        } else {
                            b = in.getField().getAnnotation(Bound.class);
                            if (b != null) {
                                try {
                                    Object v = inp.get(b.value());
                                    if (v == null) {
                                        v = new Integer(0);
                                    }
                                    int dim = Integer.parseInt(v.toString());
                                    int[] d = new int[dim];
                                    val = Conversions.convert(d, String.class);
                                } catch (NumberFormatException E) {
                                    val = "?";
                                }
                            } else {
                                val = "?";
                            }
                        }
                        w.println("@P, " + fieldName + ",  \"" + val + "\"");
                        if (b != null) {
                            w.println(" bound, " + b.value());
                        }
                    } catch (Exception E) {
                        throw new RuntimeException(E);
                    }
                }
                if (log.isLoggable(Level.WARNING)) {
                    log.warning("No Input for '" + fieldName + "'");
                }
            }
        }
        if (w != null) {
            w.close();
            System.out.println("Missing parameter [" + file + "]");
            success = false;
        }
        return success;
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
