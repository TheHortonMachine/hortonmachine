/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import oms3.gen.MethodInvoker;
import oms3.compiler.Compiler;
import java.lang.reflect.Method;
import oms3.gen.Access;

/**
 *
 * @author od
 */
public class Utils {

    static String oms_version = null;

    public static synchronized String getVersion() {
        if (oms_version == null) {
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(Utils.class.getResourceAsStream("version.txt")));
                oms_version = r.readLine();
                r.close();
            } catch (Exception ex) {
                oms_version = "?";
            }
        }
        return oms_version;
    }

    public static void main(String[] args) {
        System.out.println(getVersion());
    }

    /** Reflective invocation
     *
     * @param target
     * @param method
     * @return
     */
    static MethodInvoker reflective(final Object target, final Method method) {
        return new MethodInvoker() {

            @Override
            public void invoke() throws Exception {
                method.invoke(target);
            }

            @Override
            public void setTarget(Object target) {
            }
        };
    }

    static MethodInvoker compiled(Object target, Method method) {
        try {
            Compiler tc = Compiler.singleton(null);
            Class jc = tc.getCompiledClass(maClassName(target));
            if (jc == null) {
                String cl = methodInvoker(target, method);
                jc = tc.compileSource(maClassName(target), cl);
            }
            MethodInvoker o1 = (MethodInvoker) jc.newInstance();
            o1.setTarget(target);
            return o1;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    static Access compiled(Object target, Field field) {
        try {
            Compiler tc = Compiler.singleton(null);
            Class jc = tc.getCompiledClass(faClassName(target, field));
            if (jc == null) {
                String cl = fieldAccessor(target, field);
//                System.out.println(cl);
                jc = tc.compileSource(faClassName(target, field), cl);
            }
            Access o1 = (Access) jc.newInstance();
            o1.setTarget(target);
            return o1;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    static final String maClassName(Object target) {
        return target.getClass().getCanonicalName().replace('.', '_') + "_";
    }

    static final String faClassName(Object target, Field f) {
        return target.getClass().getCanonicalName().replace('.', '_') + "_" + f.getName();
    }

    static String fieldAccessor(Object target, Field f) {
        String cName = target.getClass().getName().replace('$', '.');
        String classPrefix = getClassPref(f.getType());
        return "public final class " + faClassName(target, f) + " implements oms3.gen." + classPrefix + "Access {\n"
                + " " + cName + " t;\n"
                + " public void setTarget(Object t) {\n"
                + "   this.t=(" + cName + ")t;\n"
                + " }\n"
                + " public Object toObject() {\n"
                + "    return t." + f.getName() + ";\n"
                + " }\n"
                + " public final " + classPrefix + " get() {\n"
                + "    return t." + f.getName() + ";\n"
                + " }\n"
                + " public final void pass(oms3.gen.Access from) {\n"
                + "    t." + f.getName() + " = " + cast(f.getType()) + "((oms3.gen." + classPrefix + "Access) from).get();\n"
                + " }\n"
                + "}\n";
    }

    static String getClassPref(Class c) {
        if (c.isPrimitive()) {
            return c.getSimpleName();
        }
        return "Object";
    }

    static String cast(Class c) {
        if (c.isPrimitive()) {
            return "";
        }
        return "(" + c.getCanonicalName() + ")";
    }

    static String methodInvoker(Object target, Method m) {
        String cName = target.getClass().getName().replace('$', '.');
        return "public final class " + maClassName(target) + " implements oms3.gen.MethodInvoker {\n"
                + " " + cName + " m;\n"
                + " public final void setTarget(Object m) {\n"
                + "   this.m=(" + cName + ")m;\n"
                + " }\n"
                + " public final void invoke() throws Exception {\n"
                + "   m." + m.getName() + "();\n"
                + " }\n"
                + "}\n";
    }
}
