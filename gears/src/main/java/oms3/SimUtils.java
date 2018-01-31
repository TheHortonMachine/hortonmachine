/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oms3.ngmf.ui.Convert;
import oms3.annotations.Description;

/**
 *
 * @author od
 */
public class SimUtils {

    private SimUtils() {
    }
    // type conversion table.
    private static Map<Class, String> types = new HashMap<Class, String>() {

        {
            put(Calendar.class, "JAMSCalendar");
            put(double.class, "JAMSDouble");
            put(int.class, "JAMSInteger");
            put(boolean.class, "JAMSBoolean");
            put(float.class, "JAMSFloat");
            put(long.class, "JAMSLong");
            put(String.class, "JAMSString");
            put(double[].class, "JAMSDoubleArray");
            put(int[].class, "JAMSIntegerArray");
            put(boolean[].class, "JAMSBooleanArray");
            put(float[].class, "JAMSFloatArray");
            put(long[].class, "JAMSLongArray");
            put(String[].class, "JAMSStringArray");
        }
    };

    public static void jams_wrap(List<String> jars, String srcPath, List<String> className) throws Exception {
        if (jars == null || jars.size() == 0) {
            throw new RuntimeException("No jar files!");
        }

        if (srcPath == null) {
            throw new RuntimeException("No src path!");
        }

        if (className == null || className.size() == 0) {
            throw new RuntimeException("No components to convert!");
        }

        URL[] u = new URL[jars.size()];
        for (int i = 0; i < jars.size(); i++) {
            if (!new File(jars.get(i)).exists()) {
                throw new RuntimeException("Not found:" + jars.get(i));
            }
            u[i] = new File(jars.get(i)).toURI().toURL();
        }

        URLClassLoader cl = new URLClassLoader(u, Thread.currentThread().getContextClassLoader());
        for (String cn : className) {
            Class c = Class.forName(cn, true, cl);
            Object o = c.newInstance();
            String jamsClass = doit(o);
            String name = "JAMS" + c.getSimpleName();
            String pack = c.getPackage().getName().replace('.', '/');
            String file = srcPath + File.separator + pack + File.separator + name + ".java";
            File f = new File(file);
            f.getParentFile().mkdirs();
            PrintWriter pw = new PrintWriter(f);
            pw.println(jamsClass);
            pw.close();
            System.out.println("  Created '" + f + "' for " + c.getName());
        }
    }

    private static String doit(Object oms3) {
        StringBuffer b = new StringBuffer();
        Class cl = oms3.getClass();

        ComponentAccess a = new ComponentAccess(oms3);
        b.append("package " + cl.getPackage().getName() + ";\n");
        b.append("\n");
        b.append("import jams.data.*;\n");
        b.append("import jams.model.*;\n");
        b.append("\n");
        b.append("@JAMSComponentDescription(title = \"" + cl.getName() + "\", \n");
        b.append("                    description = \"" + "\",\n");
        b.append("                    author = \"" + "\")\n");
        b.append("public class JAMS" + cl.getSimpleName() + " extends JAMSComponent {\n");
        b.append("\n");
        b.append("    private " + cl.getName() + " oms3 = new " + cl.getName() + "();\n");
        b.append("\n");
        b.append("//Read access variables\n");
        for (Access acc : a.inputs()) {
            b.append("    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,\n");
            b.append("       update = JAMSVarDescription.UpdateType.RUN,\n");
            b.append("       description = \"" + acc.getField().getAnnotation(Description.class).value() + "\")\n");
            b.append("    public " + types.get(acc.getField().getType()) + " " + acc.getField().getName() + ";\n");
            b.append("\n");
        }
        b.append("\n");
        b.append("//Write Access variables\n");
        for (Access acc : a.outputs()) {
            b.append("    @JAMSVarDescription(access = JAMSVarDescription.AccessType.WRITE,\n");
            b.append("       update = JAMSVarDescription.UpdateType.RUN,\n");
            b.append("       description = \"" + acc.getField().getAnnotation(Description.class).value() + "\")\n");
            b.append("    public " + types.get(acc.getField().getType()) + " " + acc.getField().getName() + ";\n");
            b.append("\n");
        }
        b.append("\n");
        b.append("    public void run() throws JAMSEntity.NoSuchAttributeException {\n");
        b.append("        // passing reads into in's\n");
        for (Access acc : a.inputs()) {
            b.append("        oms3." + acc.getField().getName() + " = " + acc.getField().getName() + ".getValue();\n");
        }
        b.append("\n");
        b.append("        // calling the oms3 execute\n");
        b.append("        oms3.execute();\n");
        b.append("\n");
        b.append("        // reading the outs\n");
        for (Access acc : a.outputs()) {
            b.append("        " + acc.getField().getName() + ".setValue(oms3." + acc.getField().getName() + ");\n");
        }
        b.append("    }\n");
        b.append("}\n");
        return b.toString();
    }

    public static void mms_convert(List<String> files)  {

//        for (String s : files) {
//            String res;
//            if (s.endsWith("statvar")) {
//                res = Convert.statvar(s);
//            } else if (s.endsWith("params")) {
//                res = Convert.param(s);
//            } else if (s.endsWith("data")) {
//                res = Convert.data(s);
//            } else {
//                res = "Error: Cannot handle " + s + "\n";
//            }
//            System.out.print(res);
//        }
    }
    
//    public static void main(String[] args) throws Exception {
//        jams_wrap(Arrays.asList( "/od/projects/ngmf.jams/dist/ngmf.jams.jar" ),
//                "/od/projects/ngmf.jams/src",
//                Arrays.asList("ngmfjams.CalcLanduseStateVars"));
//    }
}
