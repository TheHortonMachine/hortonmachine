package oms3.dsl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import oms3.io.CSProperties;
import oms3.io.DataIO;

public class Model implements Buildable {

    protected static final Logger log = Logger.getLogger("oms3.sim");
    String classname;
    List<Params> params = new ArrayList<Params>();
    Resource res;
    Logging l = new Logging();
    //
    KVPContainer comps = new KVPContainer();
    KVPContainer out2in = new KVPContainer();
    KVPContainer feedback = new KVPContainer();
    String iter;
    //
    private URLClassLoader modelClassLoader;

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
        throw new IllegalArgumentException(name.toString());
    }

    public Logging getComponentLogging() {
        return l;
    }

    public Resource getRes() {
        return res;
    }

    public void setRes(Resource res) {
        this.res = res;
    }

    public void setIter(String iter) {
        this.iter = iter;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public List<Params> getParams() {
        return params;
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

    /** get the URL classloader for all the resources (just for jar files
     * 
     * @return
     * @throws Exception
     */
    private synchronized URLClassLoader getClassLoader() throws Exception {
        if (modelClassLoader == null) {
            List<File> jars = res.filterFiles("jar");
            List<URL> urls = new ArrayList<URL>();
            for (int i = 0; i < jars.size(); i++) {
                urls.add(jars.get(i).toURI().toURL());
            }
            URL[] u = urls.toArray(new URL[0]);
            if (log.isLoggable(Level.CONFIG)) {
                for (URL url : u) {
                    log.info("class path entry from simulation: " + url.toString());
                }
            }
            modelClassLoader = new URLClassLoader(u, Thread.currentThread().getContextClassLoader());
        }
        return modelClassLoader;
    }

    public Object getComponent() throws Exception {
        URLClassLoader loader = getClassLoader();
        if (classname == null) {
            return getGeneratedComponent(loader);
        }
        try {
            Class c = loader.loadClass(classname);
            return c.newInstance();
        } catch (ClassNotFoundException E) {
            throw new IllegalArgumentException("Component/Model not found '" + classname + "'");
        }
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
                p.putAll(DataIO.properties(new FileReader(new File(f)), "Parameter"));
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

    Object getGeneratedComponent(URLClassLoader loader) {
        try {
            oms3.compiler.Compiler tc = oms3.compiler.Compiler.singleton(loader);
            String name = "Comp_" + UUID.randomUUID().toString().replace('-', '_');
            String cl = generateSource(name);
            if (log.isLoggable(Level.CONFIG)) {
                log.config("Generated Source:\n" + cl);
            }
            Class jc = tc.compileSource(name, cl);
            return jc.newInstance();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String generateSource(String cname) throws Exception {
        String sc = "oms3.Compound";
        if (iter != null) {
            sc = "oms3.control.Iteration";
        }

        StringBuilder b = new StringBuilder();
        b.append("import java.util.*;\n");
        b.append("import oms3.*;\n");
        b.append("import oms3.annotations.*;\n");
        b.append("public class " + cname + " extends " + sc + " {\n");
        b.append("\n");

        // Fields
        for (Param param : getParam()) {
            String p = param.getName();
            if (p.indexOf('.') == -1) {
                throw new IllegalArgumentException("Not a valid parameter reference (object.field): '" + p + "'");
            }
            
            String[] name = p.split("\\.");
            if (name.length != 2) {
                throw new IllegalArgumentException("Not a valid parameter reference (object.field): '" + p + "'");
            }

            String type = getClassForParameter(name[0], name[1]);
            b.append(" // " + p + "\n");
            b.append(" @Role(Role.PARAMETER)\n");
            b.append(" @In public " + type + " " + name[0] + "_" + name[1] + ";\n");
            b.append("\n");
        }

        // Components
        for (KVP def : comps.entries) {
            b.append(" public " + def.getValue() + " " + def.getKey() + " = new " + def.getValue() + "();\n");
        }
        b.append("\n");
        b.append("\n");

        // init version.
        b.append(" @Initialize\n");
        b.append(" public void init() {\n");
        if (iter != null) {
            String[] it = iter.split("\\.");
            b.append("  conditional(" + it[0] + ", \"" + it[1] + "\");\n");
        }

        // in2in
        for (Param param : getParam()) {
            String[] name = param.getName().split("\\.");
            b.append("  in2in(\"" + name[0] + '_' + name[1] + "\", " + name[0] + ", \"" + name[1] + "\");\n");
        }

        // out2in
        for (KVP c : out2in.entries) {
            String[] from = c.getKey().split("\\.");
            String[] to = c.getValue().toString().split("\\.");
            b.append("  out2in(" + from[0] + ", \"" + from[1] + "\", " + to[0] + ", \"" + to[1] + "\");\n");
        }

        // feedback
        for (KVP kvp : feedback.entries) {
            String[] from = kvp.getKey().split("\\.");
            String[] to = kvp.getValue().toString().split("\\.");
            b.append("  feedback(" + from[0] + ", \"" + from[1] + "\", " + to[0] + ", \"" + to[1] + "\");\n");
        }

        b.append("  initializeComponents();\n");
        b.append(" }\n");
        b.append("}\n");
        return b.toString();
    }

    String getClassForParameter(String object, String parameter) throws Exception {
        // find the parameter class.
        for (KVP def : comps.entries) {
            if (object.equals(def.getKey())) {
                URLClassLoader loader = getClassLoader();
                Class c = loader.loadClass(def.getValue().toString());
                return c.getDeclaredField(parameter).getType().getSimpleName();
            }
        }
        throw new IllegalArgumentException("Cannot find component '" + object + "'.");
    }
}
