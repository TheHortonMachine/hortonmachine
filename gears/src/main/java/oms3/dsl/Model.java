/*
 * $Id: Model.java b4e75b003f61 2016-05-13 odavid@colostate.edu $
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
package oms3.dsl;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import oms3.ngmf.util.UnifiedParams;
import oms3.ComponentException;
import oms3.annotations.Name;
import oms3.annotations.Role;
import oms3.compiler.ModelCompiler;
import oms3.io.CSProperties;
import oms3.io.CSTable;
import oms3.io.DataIO;
import oms3.util.Components;

public class Model implements Buildable {

    public static final String DSL_NAME = "model";
    protected static final Logger log = Logger.getLogger("oms3.sim");
    //
    String classname;
    Resource res;
    List<Params> params = new ArrayList<Params>();
    Logging l = new Logging();
    //
    KVPContainer comps = new KVPContainer();
    KVPContainer out2in = new KVPContainer();
    KVPContainer feedback = new KVPContainer();
    //
    String control = null;
    String controlClass = "oms3.Compound";
    //
    URLClassLoader modelClassLoader;
    
    // explicit component list.
    String explComp = null;

    @Override
    public Buildable create(Object name, Object value) {
        if (name.equals("parameter")) {
            Params p = new Params();
            params.add(p);
            return p;
        } else if (name.equals("resource")) {
            res.addResource(value);
            return LEAF;
        } else if (name.equals("logging")) {
            return l;
        } else if (name.equals("components")) {
            // the list of components to autogenerate a model from
            if (value != null) {
                explComp = value.toString();
                return LEAF;
            }
            return comps;
        } else if (name.equals("connect")) {
            return out2in;
        } else if (name.equals("feedback")) {
            return feedback;
        }
        throw new ComponentException("Unknown element: '" + name.toString() + "'");
    }

    KVPContainer getComponents() {
        return comps;
    }

    KVPContainer getConnects() {
        return out2in;
    }

    public Logging getComponentLogging() {
        return l;
    }

    Resource getRes() {
        return res;
    }

    void setRes(Resource res) {
        this.res = res;
    }

    public List<Params> getParams() {
        return params;
    }

    @Deprecated
    public void setIter(String c) {
        setWhile(c);
    }

    public void setWhile(String c) {
        control = c;
        controlClass = "oms3.control.While";
    }

    public void setUntil(String c) {
        control = c;
        controlClass = "oms3.control.Until";
    }

    public void setIf(String c) {
        control = c;
        controlClass = "oms3.control.If";
    }

    public void setClassname(String cn) {
        classname = cn;
    }

    public String getClassname() {
        return classname;
    }

    public String getLibpath() {
        List<File> f = res.filterDirectories();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < f.size(); i++) {
            b.append(f.get(i));
            if (i < f.size() - 1) {
                b.append(File.pathSeparatorChar);
            }
        }
        return b.toString();
    }
    String[] out2out;

    public void setOut2Out(String[] s) {
        out2out = s;
    }

    private static List<File> getExtraResources() {
        List<File> sc = new ArrayList<File>();
        String simPath = System.getProperty("oms.sim.resources");
        if (log.isLoggable(Level.CONFIG)) {
            log.config("oms.sim.resources '" + simPath + "'");
        }
        if (simPath != null && !simPath.isEmpty()) {
            simPath = simPath.replaceAll("\"", "");
            for (String s : simPath.split("\\s*" + File.pathSeparator + "\\s*")) {
                sc.add(new File(s));
            }
        }
        return sc;
    }
    
    /**
     * get the URL class loader for all the resources (just for jar files
     *
     * @return
     * @throws Exception
     */
    private synchronized URLClassLoader getClassLoader() {
        if (modelClassLoader == null) {
            List<File> jars = res.filterFiles("jar");     // jars as defined in
            List<File> cli_jars = getExtraResources();    // cli extra jars
            List<File> dirs = res.filterDirectories();    // testing
            List<URL> urls = new ArrayList<URL>();

            try {
                for (int i = 0; i < jars.size(); i++) {
                    urls.add(jars.get(i).toURI().toURL());
                    if (log.isLoggable(Level.CONFIG)) {
                        log.config("classpath entry from simulation: " + jars.get(i));
                    }
                }
                for (int i = 0; i < dirs.size(); i++) {
                    urls.add(dirs.get(i).toURI().toURL());
                    if (log.isLoggable(Level.CONFIG)) {
                        log.config("dir entry: " + dirs.get(i));
                    }
                }
                for (int i = 0; i < cli_jars.size(); i++) {
                    urls.add(cli_jars.get(i).toURI().toURL());
                    if (log.isLoggable(Level.CONFIG)) {
                        log.config("classpath entry from CLI: " + cli_jars.get(i));
                    }
                }
                urls.add(new URL("file:" + System.getProperty("oms.prj") + "/dist/"));
                if (log.isLoggable(Level.CONFIG)) {
                    log.config("Sim loading classpath : " + "file:" + System.getProperty("oms.prj") + "/dist/");
                }
            } catch (MalformedURLException ex) {
                throw new ComponentException("Illegal resource:" + ex.getMessage());
            }
            modelClassLoader = new URLClassLoader(urls.toArray(new URL[0]),
                    Thread.currentThread().getContextClassLoader());
        }
        return modelClassLoader;
    }
    // cached model component class
    Class modelComponentClass = null;

    public synchronized Object newModelComponent() throws Exception {
        if (modelComponentClass == null) {
            URLClassLoader loader = getClassLoader();
            if (classname == null) {
                modelComponentClass = getGeneratedComponent(loader);
            } else {
                try {
                    modelComponentClass = loader.loadClass(getComponentClassName(classname));
                } catch (ClassNotFoundException E) {
                    throw new IllegalArgumentException("Component/Model not found '" + classname + "'");
                }
            }
        }
        return modelComponentClass.newInstance();
    }

    private List<Param> getAllParam() {
        List<Param> parameter = new ArrayList<Param>();
        for (Params paras : getParams()) {
            parameter.addAll(paras.getParam());
        }
        return parameter;
    }

    /**
     * Check the parameter file
     *
     * @param file
     */
    static void checkParameter(File file) throws IOException {
        if (file == null || !file.exists() || !file.canRead()) {
            throw new RuntimeException("Cannot access " + file);
        }
        if (!DataIO.propertyExists("Parameter", file)) {
            throw new RuntimeException("Missing parameter section.");
        }
        CSProperties prop = DataIO.properties(new FileReader(file), "Parameter");
        if (prop.getName() == null) {
            throw new RuntimeException("Missing property set name.");
        }

        for (String key : prop.keySet()) {
            Object v = prop.get(key);
            if (v == null) {
                throw new RuntimeException("No value for property " + key);
            }
            String val = (String) v;
            String d = prop.getInfo(key).get("role");
            if (d != null) {
                if (d.equalsIgnoreCase("dimension")) {
                    try {
                        int i = Integer.parseInt(val);
                        if (i < 0) {
                            throw new RuntimeException("Invalid dimension value for " + key);
                        }

                    } catch (NumberFormatException E) {
                        throw new RuntimeException("No value for property " + key);
                    }
                }
            }
        }

    }

    public UnifiedParams getParameter() throws IOException {
        CSProperties p = DataIO.properties();
        List<String> _1D = new ArrayList<>();
        List<String> _2D = new ArrayList<>();

        for (Params paras : getParams()) {
            String f = paras.getFile();
            if (f != null) {
                File file = new File(f);
                List<String> properties = DataIO.properties(file);
                if (!properties.isEmpty()) {
                    for (String name : properties) {
                        CSProperties prop = DataIO.properties(file, name);
                        p.putAll(prop);
                    }
                }

                // check for tables in the file.
                List<String> tables = DataIO.tables(file);
                if (!tables.isEmpty()) {
                    for (String name : tables) {
                        CSTable t = DataIO.table(file, name);
                        // convert them to Properties.
                        CSProperties prop = DataIO.fromTable(t);
//                            String bound; 
                        String bounds = t.getInfo().get("bound");
                        if (bounds != null && (t.getInfo().get("bound").contains(","))) {
                            _2D.add(name);
                        } else {
                            _1D.add(name);
                        }
                        p.putAll(prop);
                    }
                } else {
                    _1D = DataIO.keysByMeta(p, "role", "dimension");
                    _2D = DataIO.keysForBounds(p, 2);
                }
            }

            // sim script parameter, no meta data here, 
            // just key value pairs.
            for (Param param : paras.getParam()) {
                p.put(param.getName().replace('.', '_'), param.getValue());
            }
        }
        if (_1D.size() > 0) {
            p.getInfo().put("oms.1D", _1D.toString().replace('[', '{').replace(']', '}'));
        }

        if (_2D.size() > 0) {
            p.getInfo().put("oms.2D", _2D.toString().replace('[', '{').replace(']', '}'));
        }
       
        return new UnifiedParams(p);
    }

    // @Name alias -> class name
    Map<String, String> nameClassMap;

    private Map<String, String> getName_ClassMap() {
        if (nameClassMap == null) {
            nameClassMap = new HashMap<String, String>();
            for (URL url : getClassLoader().getURLs()) {
                try {
                    for (Class<?> class1 : Components.getComponentClasses(url)) {
                        Name name = class1.getAnnotation(Name.class);
                        if (name != null && !name.value().isEmpty()) {
                            if (name.value().indexOf(".") > -1) {
                                log.warning("@Name cannot contain '.' character : " + name.value() + " in  " + class1.getName());
                                continue;
                            }
                            String prev = nameClassMap.put(name.value(), class1.getName());
                            if (prev != null) {
                                log.warning("duplicate @Name: " + name.value() + " for " + prev + " and " + class1.getName());
                            }
                            if (log.isLoggable(Level.CONFIG)) {
                                log.config("Added '@Name' alias '" + name.value() + "' for class: " + class1.getName());
                            }
                        }
                    }
                } catch (IOException E) {
                    throw new ComponentException("Cannot access: " + url);
                }
            }
        }
        return nameClassMap;
    }

    /**
     * Fetch an alias
     *
     * @param alias the name of the alias file (containing the real class name)
     * @return the alias content or null if there is none. (first line of that
     * alias file)
     */
    public static String aliasLookup(String alias) {
        if (alias == null || alias.isEmpty()) {
            throw new IllegalArgumentException("alias");
        }

        InputStream in = Model.class.getResourceAsStream("/META-INF/aliases/" + alias);
        if (in == null) {
            return null;
        }

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(in), 256);
            return br.readLine();
        } catch (IOException E) {
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException E) {
                    return null;
                }
            }
        }
    }

    private String getComponentClassName(String id) {
        String cn = id;
        // check for a package
        if (cn.indexOf('.') == -1) {
            // no, this is an id
            // lookup from META-INF
            cn = aliasLookup(id);
            if (cn == null) {
                // scan all classes.
                cn = getName_ClassMap().get(id);
                if (cn == null) {
                    throw new ComponentException("Unknown component name: " + id);
                }
            }
        }
        return cn;
    }

    private Class<?> getGeneratedComponent(URLClassLoader loader) {
        try {
            // TODO Generate Digest instead of UUID.
            String name = "Comp_" + UUID.randomUUID().toString().replace('-', '_');
            String source = (explComp ==null) ? generateSource(name) : generateSourceExpl(name);
//            System.out.println(source);
            if (log.isLoggable(Level.FINE)) {
                log.fine("Generated Class :" + name);
                log.fine("Generated Source:\n" + source);
            }

            ModelCompiler mc = ModelCompiler.create(System.getProperty("oms.modelcompiler"));
            Class jc = mc.compile(log, loader, name, source);

            // TODO refactor for more generic use (internal, external compiler).
//            oms3.compiler.Compiler.compile1(log, name, source);  // This is external javac
//            Class jc = loader.loadClass(name);

//            oms3.compiler.Compiler tc = oms3.compiler.Compiler.singleton(loader);
//            Class jc = tc.compileSource(name, source);
            return jc;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private String generateSourceExpl(String cname) throws Exception {
        if (control != null) {
            if (control.indexOf('.') == -1) {
                throw new IllegalArgumentException("Not a valid control reference (object.field): '" + control + "'");
            }
        }
        
        String[] a = explComp.trim().split("\\s+");
        Object[] comp = new Object[a.length];
        for (int i = 0; i < comp.length; i++) {
            comp[i] = getClassLoader().loadClass(a[i]).newInstance();
        }

        ByteArrayOutputStream ba = new ByteArrayOutputStream(1024);
        PrintStream b = new PrintStream(ba);

        b.println("import java.io.File;\n" +
                "import oms3.annotations.*;\n" +
                "import java.util.Calendar;\n" +
                "import static oms3.annotations.Role.*;\n" +
                "import oms3.control.*;\n\n" +
                "public class " + cname + " extends " + controlClass + " {\n\n");
        //
        Components.figureOutParamDeclarations(b,comp);
        Components.declare(b, comp);
        b.println("  @Initialize ");
        b.println("  public void init() {");
        if (control != null) {
            String[] it = control.split("\\.");
            b.append("  conditional(" + it[0] + ", \"" + it[1] + "\");\n");
        }
        Components.figureOutMapIn(b, comp);
        Components.figureOutConnect(b,comp);
        
        if (feedback.hasEntries()) {
           StringBuilder tmp = new StringBuilder();
           kvpExpand(tmp, feedback.entries, "feedback");        
           b.println(tmp);
        }
        
        b.println("    initializeComponents();");
        b.println("  }");
        b.println("}\n");
        b.close();
        return ba.toString();
    }
    
    private String generateSource(String cname) throws Exception {
        if (control != null) {
            if (control.indexOf('.') == -1) {
                throw new IllegalArgumentException("Not a valid control reference (object.field): '" + control + "'");
            }
        }

        List<Param> all_params = getAllParam();

        StringBuilder b = new StringBuilder();
        b.append("import java.util.*;\n");
        b.append("import oms3.*;\n");
        b.append("import oms3.annotations.*;\n");
        b.append("public class " + cname + " extends " + controlClass + " {\n");
        b.append("\n");

        // Fields
        for (Param param : all_params) {
            String p = param.getName();
//            if (p.indexOf('.') == -1) {
//                throw new IllegalArgumentException("Not a valid parameter reference (object.field): '" + p + "'");
//            }
            String[] name = p.split("\\.");
            String type = getClassForParameter(name[0], name[1], true);
            String role = getClassForParameter(name[0], name[1], false);
            b.append(" // " + p + "\n");
            b.append(" @Role(\"" + role + "\")\n");
            b.append(" @In public " + type + " " + name[0] + "_" + name[1] + ";\n");
            b.append("\n");
        }

        // out2out
        if (out2out != null) {
            for (String p : out2out) {
                if (p.indexOf('.') == -1) {
                    throw new IllegalArgumentException("Not a valid output reference (object.field): '" + p + "'");
                }
                String[] name = p.split("\\.");
                String type = getClassForParameter(name[0], name[1], true);
                String role = getClassForParameter(name[0], name[1], false);
                b.append(" // " + p + "\n");
                b.append(" @Role(\"" + role + "\")\n");
                b.append(" @Out public " + type + " " + name[0] + "_" + name[1] + ";\n");
                b.append("\n");
            }
        }

        // Components
        for (KVP def : comps.entries) {
            String compClass = def.getValue().toString();
            compClass = getComponentClassName(compClass); // name -> class
            b.append(" public " + compClass + " " + def.getKey() + " = new " + compClass + "();\n");
        }
        b.append("\n\n");

        // init version.
        b.append(" @Initialize\n");
        b.append(" public void init() {\n");
        if (control != null) {
            String[] it = control.split("\\.");
            b.append("  conditional(" + it[0] + ", \"" + it[1] + "\");\n");
        }

        // in2in
        for (Param param : all_params) {
            String[] name = param.getName().split("\\.");
            b.append("  in2in(\"" + name[0] + '_' + name[1] + "\", " + name[0] + ", \"" + name[1] + "\");\n");
        }

        // out2out
        if (out2out != null) {
            for (String param : out2out) {
                String[] name = param.split("\\.");
                b.append("  out2out(\"" + name[0] + '_' + name[1] + "\", " + name[0] + ", \"" + name[1] + "\");\n");
            }
        }

        // out2in
        kvpExpand(b, out2in.entries, "out2in");

        // feedback
        kvpExpand(b, feedback.entries, "feedback");

        b.append("  initializeComponents();\n");
        b.append(" }\n");
        b.append("}\n");
        return b.toString();
    }

    void kvpExpand(StringBuilder b, List<KVP> l, String method) {
        for (KVP c : l) {
            String key = c.getKey().trim();
            String val = c.getValue().toString().trim();

            if (key.contains(".(") && key.contains(")")) {
                // multiple outs
                String from_obj = key.substring(0, key.indexOf(".("));
                String from_fields_ = key.substring(key.indexOf('(') + 1, key.indexOf(')'));
                String[] from_fields = from_fields_.split("\\s+");
                String[] to_objs = val.split("\\s+");
                for (String to_obj : to_objs) {
                    for (String from_field : from_fields) {
                        b.append("  " + method + "(" + from_obj + ", \"" + from_field + "\", " + to_obj + ", \"" + from_field + "\");\n");
                    }
                }
                continue;
            }

            String[] from = key.split("\\.");
            if (from.length != 2) {
                throw new RuntimeException("Invalid @Out reference: '" + c.getKey());
            }

            String[] t = val.split("\\s+");    // multiple @In
            for (String kvp : t) {
                String to_obj = kvp;           // default target is just object name 
                String to_field = from[1];     // same as @Out
                if (kvp.indexOf('.') > 0) {
                    String[] to = kvp.split("\\.");
                    if (to.length != 2) {
                        throw new RuntimeException("Invalid @In reference: '" + c.getKey());
                    }
                    to_obj = to[0];
                    to_field = to[1];
                }
                b.append("  " + method + "(" + from[0] + ", \"" + from[1] + "\", " + to_obj + ", \"" + to_field + "\");\n");
            }
        }
    }

//    public static void main(String[] args) {
////        String test = "from.(abc def ce) efg";
////        System.out.println(test.substring(test.indexOf('(')+1, test.indexOf(')')));
////        System.out.println(test.substring(0, test.indexOf(".(")));
//        String[] a = new String[]{"1", "3", "4"};
//        String[] b = new String[]{"1 2 3"};
//        System.out.println(Arrays.toString(a));
//        System.out.println(Arrays.toString(b));
//
//    }
    String getClassForParameter(String object, String field, boolean type) {
        for (KVP def : comps.entries) {
            if (object.equals(def.getKey())) {
                String clname = def.getValue().toString();
                clname = getComponentClassName(clname);
                Class c;
                try {
                    c = getClassLoader().loadClass(clname);
                } catch (ClassNotFoundException ex) {
                    throw new ComponentException("Class not found: '" + clname + "'");
                }
                try {
                    if (type) {
                        String canName = c.getField(field).getType().getCanonicalName();
                        if (canName == null) {
                            throw new ComponentException("No canonical type name for : " + field);
                        }
                        return canName;
                    } else {
                        Role r = c.getField(field).getAnnotation(Role.class);
                        if (r != null) {
                            return r.value();
                        }
                        return Role.VARIABLE;
                    }
                } catch (NoSuchFieldException ex) {
                    throw new ComponentException("No such field: " + field);
                } catch (SecurityException ex) {
                    throw new ComponentException("Cannot access : " + field);
                }
            }
        }
        throw new ComponentException("Cannot find component '" + object + "'. in '" + object + "." + field + "'");
    }
}
