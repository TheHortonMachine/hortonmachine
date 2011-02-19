/*
 * $Id:$
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
package oms3.util;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oms3.*;
import oms3.annotations.Bound;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;
import oms3.annotations.Role;

/**
 * Basic component utility methods.
 * 
 * @author Olaf David
 */
public class Components {

    private Components() {
    }

    public static List<Field> parameter(Class comp) {
        List<Field> f = new ArrayList<Field>();
        for (Field field : comp.getFields()) {
            Role r = field.getAnnotation(Role.class);
            if (r != null && Annotations.plays(r, Role.PARAMETER)) {
                f.add(field);
            }
        }
        return f;
    }

    public static List<Field> inVars(Class comp) {
        List<Field> f = new ArrayList<Field>();
        for (Field field : comp.getFields()) {
            In in = field.getAnnotation(In.class);
            if (in != null) {
                Role r = field.getAnnotation(Role.class);
                if (r != null && Annotations.plays(r, Role.PARAMETER)) {
                    continue;
                }
                f.add(field);
            }
        }
        return f;
    }

    public static List<Field> outVars(Class comp) {
        List<Field> f = new ArrayList<Field>();
        for (Field field : comp.getFields()) {
            Out out = field.getAnnotation(Out.class);
            if (out != null) {
                f.add(field);
            }
        }
        return f;
    }

    static boolean isComponentClass(Class c) {
        Method[] m = c.getMethods();
        for (Method method : m) {
            if (method.getAnnotation(Execute.class) != null) {
                return true;
            }
        }
        return false;
    }

    public static Collection<Class> internalComponents(Class model) {
        Collection<Class> comps = new ArrayList<Class>();
        comps.add(model);
        internalComponents0(comps, model);
        return comps;
    }

    public static void internalComponents0(Collection<Class> comps, Class model) {
        for (Field f : model.getDeclaredFields()) {
            f.setAccessible(true);
            Class fc = f.getType();
            if (!fc.isPrimitive() && !fc.getName().startsWith("java.") && isComponentClass(fc)) {
                if (!comps.contains(fc)) {
                    comps.add(fc);
                    internalComponents0(comps, fc);
                }
            }
        }
    }

    public static void explore(Object comp) {
        explore(comp, System.out);
    }

    public static void explore(Object comp, PrintStream w) {
        Field[] fi = comp.getClass().getDeclaredFields();
        for (Field f : fi) {
            Role r = f.getAnnotation(Role.class);
            if (Annotations.plays(r, Role.PARAMETER)) {
                w.println("    field2in(model, \"" + f.getName() + "\", comp, \"" + f.getName() + "\");");
            } else if (Annotations.isInOut(f)) {
                w.println("    field2inout(hru, \"" + f.getName() + "\", comp, \"" + f.getName() + "\");");
            } else if (Annotations.isIn(f)) {
                w.println("    field2in(hru, \"" + f.getName() + "\", comp, \"" + f.getName() + "\");");
            } else if (Annotations.isOut(f)) {
                w.println("    out2field(comp, \"" + f.getName() + "\", hru, \"" + f.getName() + "\");");
            }
        }
    }

    /**
     * Figure out connectivity and generate Java statements.
     * @param comps
     */
    public static void figureOutConnect(PrintStream w, Object... comps) {
        // add all the components via Proxy.
        List<ComponentAccess> l = new ArrayList<ComponentAccess>();
        for (Object c : comps) {
            l.add(new ComponentAccess(c));
        }

        // find all out slots
        for (ComponentAccess cp_out : l) {
            w.println("// connect " + objName(cp_out));
            // over all input slots.
            for (Access fout : cp_out.outputs()) {
                String s = "   out2in(" + objName(cp_out) + ", \"" + fout.getField().getName() + "\"";
                for (ComponentAccess cp_in : l) {
                    // skip if it is the same component.
                    if (cp_in == cp_out) {
                        continue;
                    }
                    // out points to in
                    for (Access fin : cp_in.inputs()) {
                        // name equivalence enought for now.
                        if (fout.getField().getName().equals(fin.getField().getName())) {
                            s = s + ", " + objName(cp_in);
                        }
                    }
                }
                w.println(s + ");");
            }
            w.println();
        }
    }

    public static void figureOutParamDeclarations(PrintStream w, Object... comps) {

        Map<String, Access> m = new HashMap<String, Access>();

        for (Object c : comps) {
            ComponentAccess cp = new ComponentAccess(c);
//            System.out.println("// Parameter from " + objName(cp));
            // over all input slots.
            for (Access fin : cp.inputs()) {
                Role role = fin.getField().getAnnotation(Role.class);
                if (role != null && Annotations.plays(role, Role.PARAMETER)) {
                    // make sure parameter is only there once.
                    m.put(fin.getField().getName(), fin);
                }
            }
        }

        List<String> sl = new ArrayList<String>(m.keySet());
        Collections.sort(sl, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        for (String key : sl) {
            Access fin = m.get(key);
            Description d = fin.getField().getAnnotation(Description.class);
            if (d != null) {
                w.println("   @Description(\"" + d.value() + "\")");
            }
            Bound b = fin.getField().getAnnotation(Bound.class);
            if (b != null) {
                w.println("   @Bound(\"" + b.value() + "\")");
            }
            w.println("   @Role(\"" + fin.getField().getAnnotation(Role.class).value() + "\")");
            w.println("   @In public " + fin.getField().getType().getSimpleName() + " " + fin.getField().getName() + ";");
            w.println();
        }
    }

    public static void declare(PrintStream w, Object... comps) {
        w.println("// Declarartions");
        for (Object c : comps) {
            ComponentAccess cp = new ComponentAccess(c);
            w.println("    " + c.getClass().getName() + " " + objName(cp) + " = new " + c.getClass().getName() + "();");
        }
        w.println();
    }

    public static void figureOutMapIn(PrintStream w, Object... comps) {
        for (Object c : comps) {
            ComponentAccess cp = new ComponentAccess(c);
            w.println("// Input mapping to " + objName(cp));
            for (Access fin : cp.inputs()) {
                Role role = fin.getField().getAnnotation(Role.class);
                if (role != null && Annotations.plays(role, Role.PARAMETER)) {
                    w.println("   in2in(\"" + fin.getField().getName() + "\", " + objName(cp) + ");");
                }
            }
            w.println();
        }
    }

    public static void figureOutMapIn0(PrintStream w, Object... comps) {
        Map<String, List<String>> maps = new HashMap<String, List<String>>();

        for (Object c : comps) {
            ComponentAccess cp = new ComponentAccess(c);
            for (Access fin : cp.inputs()) {
                Role role = fin.getField().getAnnotation(Role.class);
                if (role != null && Annotations.plays(role, Role.PARAMETER)) {
                    List<String> l = maps.get(fin.getField().getName());
                    if (l == null) {
                        maps.put(fin.getField().getName(), l = new ArrayList<String>());
                    }
                    l.add(objName(cp));
                }
            }
        }

        List<String> sl = new ArrayList<String>(maps.keySet());
        Collections.sort(sl, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });

        for (String string : sl) {
            w.print("     in2in(\"" + string + "\"");
            List<String> l = maps.get(string);
            for (String string1 : l) {
                w.print(", " + string1);
            }
            w.println(");");
        }
    }

    static private String objName(ComponentAccess cp) {
        return cp.getComponent().getClass().getSimpleName().toLowerCase();
    }
}
