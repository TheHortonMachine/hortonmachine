/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.nap;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * @author od
 */
public abstract class NetLogo implements AnnotationHandler {

    String libname;
    String modName;
    String javaExecFunction;
    File srcFile;
    File incFile;
    File genFile;
    String packageName;
    Map<String, Map<String, String>> compAnn;
    /** All the declarations */
    List<Decl> decl = new ArrayList<Decl>();
    List<NetLogo> __incl = new ArrayList<NetLogo>();

    private static class Decl {

        Map<String, Map<String, String>> ann;
        String type;
        String name;
        String nlname;
        String decl;

        Decl(Map<String, Map<String, String>> ann, String type, String name, String nlName, String decl) {
            this.ann = ann;
            this.type = type;
            this.name = name;
            this.nlname = nlName;
            this.decl = decl;
        }

        boolean isOut() {
            return ann.containsKey("Out");
        }

        boolean isIn() {
            return ann.containsKey("In");
        }

        /** Parse a single src line
         *
         * @param decl
         * @param ann
         * @return
         */
        static List<Decl> parse(String annName, String decl, Map<String, Map<String, String>> ann) {
            String nlName = getGlobal(decl);
            List<Decl> l = new ArrayList<Decl>();
            String type = ann.get(annName).get("type");
            String name = ann.get(annName).get("name");
            String jType = type != null ? unquote(type) : "Number";
            String jName = name != null ? unquote(name) : nlName;
            ann.get(annName).clear();
            l.add(new Decl(ann, jType, jName, nlName, decl));
            return l;
        }
    }

    public void setGenFile(File genFile) {
        this.genFile = genFile;
    }

    public void setSrcFile(File srcFile) {
        this.srcFile = srcFile;
    }

    void setRelativeFile(String incFile) {
        this.packageName = new File(incFile).getParent().toString();
        this.incFile = new File(incFile);
    }

    public void setLibname(String libname) {
        this.libname = libname;
    }

    @Override
    public void handle(Map<String, Map<String, String>> ann, String line) {
        if (ann.containsKey("Execute")) {
            javaExecFunction = getFunction(line.trim());
        } else if (ann.containsKey("In")) {
            decl.addAll(Decl.parse("In", line.trim(), ann));
        } else if (ann.containsKey("Out")) {
            decl.addAll(Decl.parse("Out", line.trim(), ann));
        } else if (line.trim().startsWith("globals")) {
            compAnn = ann;
        }
    }

    @Override
    public void start(String src) {
        // parse the included files firs
        if (src.contains("__includes")) {
            String incl = src.substring(src.indexOf("__includes"));
            incl = incl.substring(incl.indexOf("["), incl.indexOf("]"));
            System.out.println(incl);
            incl = incl.replace("\"", "");
            System.out.println(incl);
            String[] includes = incl.split("\\s+");
            for (String inc : includes) {
                File inclFile = new File(srcFile.getParent(), inc);
                System.out.println(inclFile);
                NetLogo nl = new NetLogo() {

                    @Override
                    public void log(String msg) {
                        System.out.println(msg);
//                        JNAComponentTask.this.log(msg, Project.MSG_VERBOSE);
                    }
                };
                nl.setGenFile(new File(genFile.getParent(), inclFile.toString().substring(0, inclFile.toString().lastIndexOf('.')) + ".java"));
                nl.setSrcFile(new File(genFile.getParent(), inclFile.toString()));
                nl.setRelativeFile(incFile.toString());
                __incl.add(nl);
            }
        }
    }

    @Override
    public void done() throws Exception {
        // process nls files
        for (NetLogo nls : __incl) {
            try {
                AnnotationParser.handle(nls.srcFile, nls);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }


        if (javaExecFunction == null) {
            return;
        }
        genFile.getParentFile().mkdirs();

        String className = genFile.getName().substring(0, genFile.getName().indexOf('.'));
        PrintStream w = new PrintStream(genFile);

        w.println("// OMS3 NetLogo proxy for '" + srcFile.getPath() + "'");
        w.println("// Generated at " + new Date());
        w.println("package " + packageName.replace('/', '.') + ";");
        w.println();
        w.println("import oms3.annotations.*;");
        w.println("import org.nlogo.headless.HeadlessWorkspace;");
        w.println("import org.nlogo.app.App;");
        w.println("import java.io.File;");
        w.println();

        compAnn.remove("Execute");

        w.print(AnnotationParser.toString(compAnn));
        w.println("public class " + className + " {");
        w.println();
        w.println(" static final String __NLSRC__ = " + getNLSrc() + ";");
        w.println();
        w.println(" private HeadlessWorkspace __ws__;");
        w.println();
        w.println(" @In public boolean gui = false;");
        w.println();

        for (Decl d : decl) {
            w.print(AnnotationParser.toString(d.ann));
            w.println(" public " + d.type + " " + d.name + ";");
            w.println();
        }
        w.println(" @Execute");
        w.println(" public void exec() throws Exception {");

        // HEADLESS
        w.println("   if (!gui) {");
        w.println("     if (__ws__ == null) {");
        w.println("         __ws__ = HeadlessWorkspace.newInstance();");
        w.println("         __ws__.open(__NLSRC__);");
        w.println("     }");

        for (Decl d : decl) {
            if (d.isIn()) {
                w.println("     __ws__.command(\"set " + d.name + " \" + " + d.name + ".toString());");
            }
        }
        w.println("     __ws__.command(\"" + javaExecFunction + "\");");
        for (Decl d : decl) {
            if (d.isOut()) {
                w.println("     " + d.name + " = (" + d.type + ") __ws__.report(\"" + d.nlname + "\");");
            }
        }
        // GUI
        w.println("   } else { ");
        w.println("     App.main(new String[]{});");
        w.println("     java.awt.EventQueue.invokeAndWait(new Runnable() {");
        w.println("        public void run() {");
        w.println("          try {");
        w.println("            App.app.open(__NLSRC__);");
        w.println("          } catch (java.io.IOException ex) {");
        w.println("            throw  new RuntimeException(ex);");
        w.println("          }");
        w.println("        }");
        w.println("      });");
        for (Decl d : decl) {
            if (d.isIn()) {
                w.println("     App.app.command(\"set " + d.name + " \" + " + d.name + ".toString());");
            }
        }
        w.println("     App.app.command(\"" + javaExecFunction + "\");");
        for (Decl d : decl) {
            if (d.isOut()) {
                w.println("     " + d.name + " = (" + d.type + ") App.app.report(\"" + d.nlname + "\");");
            }
        }
        w.println("   }");
        w.println(" }");
        w.println();

        w.println("  @Finalize");
        w.println("  public void done() throws InterruptedException {");
        w.println("    if (__ws__ != null) {");
        w.println("      __ws__.dispose();");
        w.println("      __ws__ = null;");
        w.println("    }");
        w.println("  }");
        w.println("}");
        w.close();

        // tag the timestamp.
        genFile.setLastModified(srcFile.lastModified());
    }

    String getNLSrc() {
        return "System.getProperty(\"oms.prj\") + File.separatorChar + \"src\" + File.separatorChar +  \"" + incFile.toString() + "\"";
    }

    static String getGlobal(String global) {
        if (global.startsWith(";;")) {
            return null;   // this is a real comment !
        }
        global = global.replace(";", " ");
        String[] s = global.trim().split("\\s+");
        if (s.length == 0) {
            throw new IllegalArgumentException(global);
        }
        return s[0];
    }

    static String getFunction(String func) {
        if (!func.startsWith("to ")) {
            throw new IllegalArgumentException("function " + func);
        }
        func = func.replace(";", " ");
        String[] s = func.trim().split("\\s+");
        if (s.length < 1) {
            throw new IllegalArgumentException(func);
        }
        return s[1];  // skip 'to'
    }

    static String unquote(String s) {
        return s.replace("\"", "");
    }
}
