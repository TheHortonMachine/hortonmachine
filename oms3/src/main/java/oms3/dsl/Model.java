package oms3.dsl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import oms3.ComponentException;
import oms3.annotations.Name;
import oms3.annotations.Role;
import oms3.compiler.ModelCompiler;
import oms3.io.CSProperties;
import oms3.io.CSTable;
import oms3.io.DataIO;
import oms3.util.Components;

public class Model implements Buildable {

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

    Logging getComponentLogging() {
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

    public void setClassname(String classname) {
        this.classname = classname;
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

    /** get the URL class loader for all the resources (just for jar files
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

    public Object getComponent() throws Exception {
        URLClassLoader loader = getClassLoader();
        Class c = null;
        if (classname == null) {
//            return getGeneratedComponent(loader);
//            classname = getGeneratedComponent(loader);
            c = getGeneratedComponent(loader);
        } else {
            try {
                c = loader.loadClass(getComponentClassName(classname));
            } catch (ClassNotFoundException E) {
                throw new IllegalArgumentException("Component/Model not found '" + classname + "'");
            }
        }
        return c.newInstance();
    }

    public List<Param> getParam() {
        List<Param> parameter = new ArrayList<Param>();
        for (Params paras : getParams()) {
            parameter.addAll(paras.getParam());
        }
        return parameter;
    }

    public CSProperties getParameter() throws IOException {
        CSProperties p = DataIO.properties();
        for (Params paras : getParams()) {
            String f = paras.getFile();
            if (f != null) {
                // original properties.
                p.putAll(DataIO.properties(new FileReader(new File(f)), "Parameter"));
                // check for tables in the file.
                List<String> tables = DataIO.tables(new File(f));
                if (!tables.isEmpty()) {
                    for (String name : tables) {
                        CSTable t = DataIO.table(new File(f), name);
                        // convert them to Properties.
                        CSProperties prop = DataIO.fromTable(t);
                        p.putAll(prop);
                    }
                }
            }
            for (Param param : paras.getParam()) {
                p.put(param.getName(), param.getValue());
            }
        }
        return p;
    }

    static Object get(Map<String, Object> inst, String key) {
        Object val = inst.get(key);
        if (val == null) {
            throw new IllegalArgumentException("No such component name '" + key + "'");
        }
        return val;
    }
    // @Name alias -> class name
    Map<String, String> nameClassMap;

    private Map<String, String> getName_ClassMap() {
        if (nameClassMap == null) {
            nameClassMap = new HashMap<String, String>();
            for (URL url : getClassLoader().getURLs()) {
                try {
                	if(url.toString().startsWith("file:null")) 
                		continue;
                	List<Class<?>> componentClasses = Components.getComponentClasses(url);
                    
					for (Class<?> class1 : componentClasses) {
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

    private String getComponentClassName(String id) {
        if (id.indexOf('.') == -1) {
            String cn = getName_ClassMap().get(id);
            
            if (cn == null) {
                throw new ComponentException("Unknown component name: " + id);
            }
            return cn;
        }
        return id;
    }

    private Class<?> getGeneratedComponent(URLClassLoader loader) {
        try {
            // TODO Generate Digest instead of UUID.
            String name = "Comp_" + UUID.randomUUID().toString().replace('-', '_');
            String source = generateSource(name);
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

    private String generateSource(String cname) throws Exception {
        if (control != null) {
            if (control.indexOf('.') == -1) {
                throw new IllegalArgumentException("Not a valid control reference (object.field): '" + control + "'");
            }
        }

        StringBuilder b = new StringBuilder();
        b.append("import java.util.*;\n");
        b.append("import oms3.*;\n");
        b.append("import oms3.annotations.*;\n");
        b.append("public class " + cname + " extends " + controlClass + " {\n");
        b.append("\n");

        // Fields
        for (Param param : getParam()) {
            String p = param.getName();
            if (p.indexOf('.') == -1) {
                throw new IllegalArgumentException("Not a valid parameter reference (object.field): '" + p + "'");
            }
            String[] name = p.split("\\.");
            String type = getClassForParameter(name[0], name[1], true);
            String role = getClassForParameter(name[0], name[1], false);
            b.append(" // " + p + "\n");
            b.append(" @Role(\"" + role + "\")\n");
            b.append(" @In public " + type + " " + name[0] + "_" + name[1] + ";\n");
            b.append("\n");
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
        for (Param param : getParam()) {
            String[] name = param.getName().split("\\.");
            b.append("  in2in(\"" + name[0] + '_' + name[1] + "\", " + name[0] + ", \"" + name[1] + "\");\n");
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
            String[] from = c.getKey().split("\\.");
            String tos = c.getValue().toString().trim();
            String[] t = tos.split("\\s+");    // multiple @In
            for (String kvp : t) {
                String to_obj = kvp;           // default target is just object name 
                String to_field = from[1];     // same as @Out
                if (kvp.indexOf('.') > 0) {
                    String[] to = kvp.split("\\.");
                    to_obj = to[0];
                    to_field = to[1];
                }
                b.append("  " + method + "(" + from[0] + ", \"" + from[1] + "\", " + to_obj + ", \"" + to_field + "\");\n");
            }
        }
    }

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
