/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.nap;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * @author od
 */
public abstract class JNAFortran implements AnnotationHandler {

    String libname;
    String modName;
    String javaExecFunction;
    String description = "";
    File srcFile;
    File genFile;
    String packageName;

    private static class Decl {

        Map<String, Map<String, String>> ann;
        String type;
        String name;

        Decl(Map<String, Map<String, String>> ann, String type, String name) {
            this.ann = ann;
            this.type = type;
            this.name = name;
        }

        boolean isOut() {
            return ann.containsKey("Out");
        }

        boolean isIn() {
            return ann.containsKey("In");
        }

        boolean isScalar() {
            return type.equals("float") || type.equals("double") || type.equals("boolean")
                    || type.equals("int");
        }

        String getReferenceType() {
            if (type.equals("float")) {
                return "FloatByReference";
            } else if (type.equals("double")) {
                return "DoubleByReference";
            } else if (type.equals("int")) {
                return "IntByReference";
            }
            throw new IllegalArgumentException(type);
        }

        //   fDecl  INTEGER(C_INT), VALUE :: erosion_inp_len
        static String getJavaType(String fDecl) {
            String d = fDecl.toLowerCase().trim();

            if (d.contains("c_float") && isArray(fDecl)) {
                return "float[]";
            } else if (d.contains("c_int") && isArray(fDecl)) {
                return "int[]";
            } else if (d.contains("c_double") && isArray(fDecl)) {
                return "double[]";
            } else if (d.contains("c_float")) {
                return "float";
            } else if (d.contains("c_int")) {
                return "int";
            } else if (d.contains("c_double")) {
                return "double";
            } else if (d.startsWith("char")) {
                return "String";
            }
            throw new IllegalArgumentException(fDecl);
        }

        static String[] getDeclNames(String fDecl) {
            String d = fDecl.toLowerCase().trim();
            String s[] = d.split("::");
            if (s.length != 2) {
                throw new IllegalArgumentException(fDecl);
            }
            return s[1].trim().split("\\s*,\\s*");
        }

        /** Parse a single src line
         *
         * @param decl
         * @param ann
         * @return
         */
        static List<Decl> parse(String decl, Map<String, Map<String, String>> ann) {
            List<Decl> l = new ArrayList<Decl>();
            String jType = getJavaType(decl);
            String[] names = getDeclNames(decl);
            for (String name : names) {
                l.add(new Decl(ann, jType, name));
            }
            return l;
        }

        static boolean isArray(String fDecl) {
            String d = fDecl.toLowerCase().trim();
            return d.indexOf("dimension") > -1;
        }
    }
    /** All the declarations */
    List<Decl> decl = new ArrayList<Decl>();

    public void setGenFile(File genFile) {
        this.genFile = genFile;
    }

    public void setSrcFile(File srcFile) {
        this.srcFile = srcFile;
    }

    void setRelativeFile(String incFile) {
        this.packageName = new File(incFile).getParent().toString();
    }

    public void setLibname(String libname) {
        this.libname = libname;
    }

    @Override
    public void handle(Map<String, Map<String, String>> ann, String line) {
        if (ann.containsKey("Execute")) {
            javaExecFunction = ann.get("Execute").get("value");
            if (javaExecFunction == null) {
                line = line.trim();
                javaExecFunction = line.substring(line.indexOf(' '), line.indexOf('('));
            } else {
                javaExecFunction = AnnotationParser.trimQuotes(javaExecFunction);
            }
        } else if (ann.containsKey("In") || ann.containsKey("Out")) {
            decl.addAll(Decl.parse(line.trim(), ann));
        }
    }

    @Override
    public void start(String src) {
        src = src.toLowerCase();
        if (src.contains("module")) {
            String line = src.substring(src.indexOf("module"));
            line = src.substring(0, src.indexOf("\n"));
            String[] mdecl = line.trim().split("\\s+");
            if (mdecl.length > 0) {
                modName = mdecl[1];
            }
        }
    }

    @Override
    public void done() throws Exception {
        if (javaExecFunction == null) {
            return;
        }
        genFile.getParentFile().mkdirs();

        String className = genFile.getName().substring(0, genFile.getName().indexOf('.'));
        PrintStream w = new PrintStream(genFile);

        w.println("// OMS3 Native proxy from '" + srcFile.getPath() + "'");
        w.println("// Generated at " + new Date());
        w.println("package " + packageName.replace('/', '.') + ";");
        w.println();
        w.println("import com.sun.jna.ptr.*;");
        w.println("import oms3.annotations.*;");
        w.println();
        w.println("// " + description);
        w.println("public class " + className + " {");
        w.println();

        for (Decl d : decl) {
            w.print(AnnotationParser.toString(d.ann));
            w.println(" public " + d.type + " " + d.name + ";");
            w.println();
        }

        w.println(" @Execute");
        w.println(" public void exec() {");
        for (Decl d : decl) {
            if (d.isScalar()) {
                w.println("  " + d.getReferenceType() + " " + d.name + "__ = new " + d.getReferenceType() + "(" + d.name + ");");
            }
        }
        w.print("   __Native__.lib." + getNativeName() + "(");
        for (int i = 0; i < decl.size(); i++) {
            Decl d = decl.get(i);
            w.print(d.name);
            if (d.isScalar()) {
                w.print("__");
            }
            if (d.type.equals("String")) {
                w.print(", " + d.name + ".length()");
            }
            if (i < decl.size() - 1) {
                w.print(",");
            }
        }

        w.println(");");

        for (Decl d : decl) {
            if (d.isScalar() && d.isOut()) {
                w.println("  " + d.name + " = " + d.name + "__.getValue();");
            }
        }

        w.println(" }");
        w.println();
        w.println(" @DLL(\"" + libname + "\")");
        w.println(" interface __Native__ extends com.sun.jna.Library {");
        w.println("   // library mapping reference");
        w.println("   __Native__ lib = oms3.util.NativeLibraries.bind(__Native__.class);");
        w.println();
        w.println("   // DLL function(s)");
        w.print("   void " + getNativeName() + "(");

        for (int i = 0; i < decl.size(); i++) {
            Decl d = decl.get(i);
            if (d.isScalar()) {
                w.print("ByReference " + d.name);
            } else {
                w.print(d.type + " " + d.name);
                if (d.type.equals("String")) {
                    w.print(", int " + d.name + "_len");
                }
            }
            if (i < decl.size() - 1) {
                w.print(",");
            }
        }

        w.println(");");
        w.println(" }");
        w.println("}");
        w.close();


        // checking later the update.
        //  genFile.setLastModified(srcFile.lastModified());
    }

    String getNativeName() {
        return modName != null ? ("__" + modName + "_MOD_" + javaExecFunction)
                : javaExecFunction + "_";
    }

    public static void main(String[] args) throws Exception {
//        if (args.length != 1) {
//            return;
//        }
//
//        File natSrc = new File(args[0]);
//        File natSrc = new File(System.getProperty("user.dir") + "/test/oms3/ap/Arr.f90");
//        AnnotationHandler ah = new JNA();
//        AnnotationParser.handle(natSrc, ah);

        System.out.println(Arrays.toString(Decl.getDeclNames("CHARACTER(C_CHAR, len=hyd2er_len)  :: a")));
//        System.out.println(getDeclModifier("CHARACTER(C_CHAR, len=hyd2er_len)"));
    }
}
