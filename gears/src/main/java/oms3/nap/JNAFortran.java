///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package oms3.nap;
//
//import java.io.File;
//import java.io.PrintStream;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//
///**
// *
// * @author od
// */
//public abstract class JNAFortran implements AnnotationHandler {
//
//    String libname;
//    String modName;
//    String javaExecFunction;
//    File srcFile;
//    File genFile;
//    String packageName;
//    Map<String, Map<String, String>> compAnn;
//    /** All the declarations */
//    List<Decl> decl = new ArrayList<Decl>();
//
//    private static class Decl {
//
//        Map<String, Map<String, String>> ann;
//        String type;
//        String name;
//        String decl;
//
//        Decl(Map<String, Map<String, String>> ann, String type, String name, String decl) {
//            this.ann = ann;
//            this.type = type;
//            this.name = name;
//            this.decl = decl.toLowerCase();
//        }
//
//        boolean isOut() {
//            return ann.containsKey("Out");
//        }
//
//        boolean isIn() {
//            return ann.containsKey("In");
//        }
//
//        boolean isScalar() {
//            return type.equals("float") || type.equals("double") || type.equals("boolean")
//                    || type.equals("int");
//        }
//
//        boolean isArray() {
//            return type.endsWith("[]");
//        }
//
//        String getBaseType() {
//            if (isArray()) {
//                return type.substring(0, type.indexOf('['));
//            }
//            throw new RuntimeException("Not an array declaration: " + toString());
//        }
//
//        String getReferenceType() {
//            if (type.equals("float")) {
//                return "FloatByReference";
//            } else if (type.equals("double")) {
//                return "DoubleByReference";
//            } else if (type.equals("int")) {
//                return "IntByReference";
//            }
//            throw new IllegalArgumentException(type);
//        }
//
//        String getDimension() {
//            if (isArray()) {
//                String dim = decl.substring(decl.indexOf("dimension"));
//                dim = dim.substring(dim.indexOf('(') + 1, dim.indexOf(')'));
//                return dim.trim();
//            }
//            throw new RuntimeException("Illegal Dimension :" + decl);
//        }
//
//        //   fDecl  INTEGER(C_INT), VALUE :: erosion_inp_len
//        static String getJavaType(String fDecl) {
//            String d = fDecl.toLowerCase().trim();
//
//            if (d.contains("c_float") && isArray(fDecl)) {
//                return "float[]";
//            } else if (d.contains("c_int") && isArray(fDecl)) {
//                return "int[]";
//            } else if (d.contains("c_double") && isArray(fDecl)) {
//                return "double[]";
//            } else if (d.contains("c_float")) {
//                return "float";
//            } else if (d.contains("c_int")) {
//                return "int";
//            } else if (d.contains("c_double")) {
//                return "double";
//            } else if (d.startsWith("char")) {
//                return "String";
//            }
//            throw new IllegalArgumentException(fDecl);
//        }
//
//        static String[] getDeclNames(String fDecl) {
//            String d = fDecl.toLowerCase().trim();
//            String s[] = d.split("::");
//            if (s.length != 2) {
//                throw new IllegalArgumentException(fDecl);
//            }
//            return s[1].trim().split("\\s*,\\s*");
//        }
//
//        /** Parse a single src line
//         *
//         * @param decl
//         * @param ann
//         * @return
//         */
//        static List<Decl> parse(String decl, Map<String, Map<String, String>> ann) {
//            List<Decl> l = new ArrayList<Decl>();
//            String jType = getJavaType(decl);
//            String[] names = getDeclNames(decl);
//            for (String name : names) {
//                l.add(new Decl(ann, jType, name, decl));
//            }
//            return l;
//        }
//
//        static boolean isArray(String fDecl) {
//            String d = fDecl.toLowerCase().trim();
//            return d.indexOf("dimension") > -1;
//        }
//    }
//    JNAComponentTask task;
//
//    public JNAFortran(JNAComponentTask task) {
//        this.task = task;
//    }
//
//    public void setGenFile(File genFile) {
//        this.genFile = genFile;
//    }
//
//    public void setSrcFile(File srcFile) {
//        this.srcFile = srcFile;
//    }
//
//    void setRelativeFile(String incFile) {
//        this.packageName = new File(incFile).getParent().toString();
//    }
//
//    public void setLibname(String libname) {
//        this.libname = libname;
//    }
//
//    List<Decl> getOutArrays(List<Decl> l) {
//        List<Decl> o = new ArrayList<Decl>();
//        for (Decl decl : l) {
//            if (decl.isArray() && decl.isOut() && !decl.isIn()) {
//                o.add(decl);
//            }
//        }
//        return o;
//    }
//
//    @Override
//    public void handle(Map<String, Map<String, String>> ann, String line) {
//        if (ann.containsKey("Execute")) {
//            javaExecFunction = ann.get("Execute").get("value");
//            if (javaExecFunction == null) {
//                line = line.trim();
//                javaExecFunction = line.substring(line.indexOf(' '), line.indexOf('('));
//            } else {
//                javaExecFunction = AnnotationParser.trimQuotes(javaExecFunction);
//            }
//            javaExecFunction = javaExecFunction.trim().toLowerCase();
//            compAnn = ann;
//        } else if (ann.containsKey("In") || ann.containsKey("Out")) {
//            decl.addAll(Decl.parse(line.trim(), ann));
//        }
//    }
//
//    @Override
//    public void start(String src) {
//        src = src.toLowerCase();
////        if (src.contains("module")) {
////            String line = src.substring(src.indexOf("module"));
////            line = src.substring(0, src.indexOf("\n"));
////            String[] mdecl = line.trim().split("\\s+");
////            if (mdecl.length > 0) {
////                modName = mdecl[1];
////            }
////        }
//    }
//
//    @Override
//    public void done() throws Exception {
//        if (javaExecFunction == null) {
//            return;
//        }
//        genFile.getParentFile().mkdirs();
//
//        String className = genFile.getName().substring(0, genFile.getName().indexOf('.'));
//        PrintStream w = new PrintStream(genFile);
//
//        List<Decl> outArr = getOutArrays(decl);
//
//        w.println("// OMS3 Native proxy from '" + srcFile.getPath() + "'");
//        w.println("// Generated at " + new Date());
//        w.println("package " + packageName.replace('/', '.') + ";");
//        w.println();
//        w.println("import com.sun.jna.ptr.*;");
//        w.println("import java.util.logging.Level;");
//        w.println("import java.util.logging.Logger;");
//        w.println("import oms3.annotations.*;");
//        w.println();
//
//        compAnn.remove("Execute");
//        w.print(AnnotationParser.toString(compAnn));
//        w.println("public class " + className + " {");
//        w.println();
//        if (task.genlogging) {
//            w.println("  static final Logger log = Logger.getLogger(\"oms3.model.\" +");
//            w.println("           " + className + ".class.getName());");
//        }
//
//        if (task.gensingleton) {
//            w.println("  private static " + className + " instance;");
//            w.println("  public static synchronized " + className + " instance() {");
//            w.println("    if (instance == null) {");
//            w.println("      instance = new " + className + "();");
//            w.println("    }");
//            w.println("    return instance;");
//            w.println("  }");
//        }
//
//        w.println();
//        w.println("  public " + className + "() {");
//        if (task.genprotected) {
//            w.println("     com.sun.jna.Native.setProtected(true);");
//        }
//        w.println("  }");
//
//        if (!outArr.isEmpty()) {
//            w.println(" private boolean __shouldInit__ = true;");
//        }
//        w.println();
//
//        for (Decl d : decl) {
//            w.print(AnnotationParser.toString(d.ann));
//            w.println(" public " + d.type + " " + d.name + ";");
//            w.println();
//        }
//
//        w.println(" @Execute");
//        w.println(" public void exec() throws Exception {");
//
//        if (!outArr.isEmpty()) {
//            w.println("  if(__shouldInit__) {");
//            for (Decl decl : outArr) {
//                w.println("      " + decl.name + " = new " + decl.getBaseType() + "[" + decl.getDimension() + "];");
//            }
//            w.println("      __shouldInit__ = false;");
//            w.println("  }");
//        }
//        for (Decl d : decl) {
//            if (d.isScalar()) {
//                w.println("  " + d.getReferenceType() + " " + d.name + "__ = new " + d.getReferenceType() + "(" + d.name + ");");
//            }
//        }
//        w.print("   __Native__.lib." + getNativeName() + "(");
//        for (int i = 0; i < decl.size(); i++) {
//            Decl d = decl.get(i);
//            w.print("\n     " + d.name);
//            if (d.isScalar()) {
//                w.print("__");
//            }
//            if (d.type.equals("String")) {
//                w.print(", " + d.name + ".length()");
//            }
//            if (i < decl.size() - 1) {
//                w.print(",");
//            }
//        }
//
//        w.println(");");
//
//        for (Decl d : decl) {
//            if (d.isScalar() && d.isOut()) {
//                w.println("  " + d.name + " = " + d.name + "__.getValue();");
//            }
//        }
//
//        if (task.genlogging) {
//            w.println("  if(log.isLoggable(Level.INFO)) {");
//            w.println("   log.info(oms3.ComponentAccess.dump(this));");
//            w.println("  }");
//        }
//
//        w.println(" }");
//
//        w.println();
//        w.println(" @DLL(\"" + libname + "\")");
//        w.println(" interface __Native__ extends com.sun.jna.Library {");
//        w.println("   // library mapping reference");
//        w.println("   __Native__ lib = oms3.util.NativeLibraries.bind(__Native__.class);");
//        w.println();
//        w.println("   // DLL function(s)");
//        w.print("   void " + getNativeName() + "(");
//
//        for (int i = 0; i < decl.size(); i++) {
//            Decl d = decl.get(i);
//            if (d.isScalar()) {
//                w.print("\n     ByReference " + d.name);
//            } else {
//                w.print("\n     " + d.type + " " + d.name);
//                if (d.type.equals("String")) {
//                    w.print(", int " + d.name + "_len");
//                }
//            }
//            if (i < decl.size() - 1) {
//                w.print(",");
//            }
//        }
//
//        w.println(");");
//        w.println(" }");
//        w.println("}");
//        w.close();
//
//        // tag the timestamp.
//        genFile.setLastModified(srcFile.lastModified());
//    }
//
//    String getNativeName() {
//        return modName != null ? ("__" + modName + "_MOD_" + javaExecFunction)
//                : javaExecFunction + "_";
//    }
//
//    public static void main(String[] args) throws Exception {
////        if (args.length != 1) {
////            return;
////        }
////
////        File natSrc = new File(args[0]);
////        File natSrc = new File(System.getProperty("user.dir") + "/test/oms3/ap/Arr.f90");
////        AnnotationHandler ah = new JNA();
////        AnnotationParser.handle(natSrc, ah);
//
//        System.out.println(Arrays.toString(Decl.getDeclNames("CHARACTER(C_CHAR, len=hyd2er_len)  :: a")));
////        System.out.println(getDeclModifier("CHARACTER(C_CHAR, len=hyd2er_len)"));
//    }
//}
