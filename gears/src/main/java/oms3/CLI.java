/*
 * $Id: CLI.java 1fe99f9c399e 2018-12-16 francesco.serafin.3@gmail.com $
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
package oms3;

import com.sun.jna.Native;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.lang.MissingPropertyException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import oms3.ngmf.util.PreProcessor;
import oms3.ngmf.util.Jars;
import oms3.dsl.AbstractSimulation;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.syntax.SyntaxException;

/**
 * Command Line interface to run simulations.
 *
 * @author od
 */
public class CLI {

    protected static final Logger log = Logger.getLogger("oms3.sim");
    //
    static final List<String> simExt = Arrays.asList(".sim", ".luca", ".esp", ".fast", ".ps");
    static final List<String> flags = Arrays.asList("-r", "-e", "-d", "-o", "-a", "-s", "-b", "-p");


    static {
        if (System.getProperty("java.version").compareTo("1.6") < 0) {
            throw new RuntimeException("Java 1.6+ required.");
        }
    }


    private CLI() {
    }


    /**
     * Executes a simulation dsl.
     *
     * @param file the file to execute
     * @param ll the log level
     * @param cmd the command to call (e.g. run)
     * @return the simulation object
     * @throws Exception generic exception
     */
    public static Object dsl(String file, String ll, String cmd) throws Exception {
        return dsl(file, ll, cmd, defaultBinding());
    }


    /**
     * Executes a simulation dsl.
     *
     * @param file the file to execute
     * @param loglevel the log level
     * @param cmd the command to call (e.g. run)
     * @param binding the groovy binding
     * @return the simulation object
     * @throws Exception generic exception
     */
    public static Object dsl(String file, String loglevel, String cmd, Map binding) throws Exception {
        Object o = createSim(readFile(file), loglevel, file, binding);
        return invoke(o, cmd);
    }


    public static Object dsl(String file, String loglevel, String cmd, Binding binding) throws Exception {
        Object o = createSim(readFile(file), loglevel, file, binding);
        return invoke(o, cmd);
    }


    /**
     * Executes a sim (no includes here though)
     *
     * @param file the file to execute
     * @param filename the file name
     * @param loglevel the log level
     * @param cmd the command to call (e.g. run)
     * @return the simulation object
     * @throws Exception generic exception
     */
    public static Object dsl(InputStream file, String filename, String loglevel, String cmd) throws Exception {
        Object o = createSim(readFile(file), loglevel, filename, defaultBinding());
        return invoke(o, cmd);
    }


    /**
     * Executed plain groovy.
     *
     * @param file the groovy file
     * @param loglevel the log level.
     * @return the simulation object
     * @throws Exception generic exception
     */
    public static Object groovy(String file, String loglevel) throws Exception {
        return createSim(readFile(file, defaultBinding()), loglevel, file, defaultBinding());
    }


    /**
     * Executed plain groovy.
     *
     * @param file the groovy file
     * @param loglevel the log level
     * @param binding the groovy binding
     * @return the simulation object
     * @throws Exception generic exception
     */
    public static Object groovy(String file, String loglevel, Map<String, String> binding) throws Exception {
        return createSim(readFile(file, new HashMap<>(binding)), loglevel, file, binding);
    }


    /**
     * Invokes a simulation method. (run | doc | analysis | ...)
     *
     * @param target the target simulation object
     * @param name the name of the method (e.g. run())
     * @throws Exception generic exception
     */
    private static Object invoke(Object target, String name) throws Exception {
        return target.getClass().getMethod(name).invoke(target);
    }


    /**
     * Read a file and provide its content as String.
     *
     * @param name the file name
     * @return the content as String
     * @throws IOException something bad happened.
     */
    private static String readFile(String name) {
        try {
            return PreProcessor.getContent(new File(name), new HashMap());
        } catch (IOException E) {
            throw new ComponentException(E.getMessage());
        }
    }

    /**
     * Read a file and provide its content as String.
     *
     * @param name the file name
     * @return the content as String
     * @throws IOException something bad happened.
     */
    private static String readFile(String name, Map bindings) {
        try {
            return PreProcessor.getContent(new File(name), bindings);
        } catch (IOException E) {
            throw new ComponentException(E.getMessage());
        }
    }


    private static String readFile(InputStream is) {
        StringBuilder b = new StringBuilder();
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = r.readLine()) != null) {
                b.append(line).append('\n');
            }
            r.close();
        } catch (IOException E) {
            throw new ComponentException(E.getMessage());
        }
        return b.toString();
    }


    /**
     * get the default Binding.
     *
     * @return the default binding
     */
    public static Map defaultBinding() {
        Map b = new HashMap<>();
        b.put("oms_version", System.getProperty("oms.version"));
        b.put("oms_home", System.getProperty("oms.home"));
        b.put("oms_prj", System.getProperty("oms.prj"));

        Map<String, Object> props = getVarOverrides();
        if (props != null) {
            for (Map.Entry<String, Object> entry : props.entrySet()) {
                b.put(entry.getKey(), Objects.toString(entry.getValue()));
            }
        }

        return b;
	}


    private static Object createSim(String script, String ll, String file, Map m) {
        return createSim(script, ll, file, new Binding(m));
    }


    /**
     * Create a simulation object.
     *
     * @param script the script
     * @param groovy
     * @param ll
     * @return the simulation object.
     */
    private static Object createSim(String script, String ll, String file, Binding m) {
        file = (file == null) ? "unknown" : file;
        try {
            log.setLevel(Level.parse(ll));
        } catch (IllegalArgumentException E) {
            throw new ComponentException(E.getMessage());
        }
        Level.parse(ll);                            // may throw IAE
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        GroovyShell shell = new GroovyShell(new GroovyClassLoader(parent), m);
        
        System.setProperty("oms.dsl", script);

        try {
            return shell.evaluate(script);
        } catch (MultipleCompilationErrorsException E) {
            int n = E.getErrorCollector().getErrorCount();
            if (n > 0) {
                SyntaxException syn = E.getErrorCollector().getSyntaxError(0);
                int line = syn.getLine();
//                throw new ComponentException(new File(file).getName() + " [line:"
                throw new ComponentException(file + " [line:"
                        + line + " column:" + syn.getStartColumn() + "]  " + syn.getOriginalMessage());
            } else {
                throw E;
            }
        } catch (MissingPropertyException E) {
            throw new ComponentException("Cannot handle property '" + E.getProperty() + "' in " + file);
        } catch (GroovyRuntimeException E) {
            throw new ComponentException(E.getMessage() + " in '" + file + "'");
        }
    }


    public static void setOMSProperties(String script, String cmd) {
        if (script != null) {
            System.setProperty("oms.script", script);
        }
        if (cmd != null) {
            System.setProperty("oms.cmd", cmd);
        }

        if (System.getProperty("jna.protected") != null) {
            Native.setProtected(true);
        }
        String oms_work = System.getProperty("oms3.work");
        if (oms_work != null) {
            System.setProperty("oms.prj", oms_work);
        } else {
            oms_work = System.getProperty("oms_prj");
            if (oms_work != null) {
                System.setProperty("oms.prj", oms_work);
            }
        }
        System.setProperty("oms.version", oms3.Utils.getVersion());
        if (System.getProperty("oms.home") == null) {
            System.setProperty("oms.home", System.getProperty("user.home")
                    + File.separator + ".oms" + File.separator + oms3.Utils.getVersion());
        }
    }


    private static void usage() {
        System.err.println("usage: java -jar oms-all.jar [-l <loglevel> ] [-r|-e|-d|-a|-s|-o|-p] <simfile>");
        System.err.println(" Command line access to simulations.");
        System.err.println("           -r   run the <simfile>");
        System.err.println("           -e   edit parameter in <simfile>");
        System.err.println("           -o   open the last output folder in desktop <simfile>");
        System.err.println("           -d   document the <simfile>");
        System.err.println("           -a   run the <simfile> analysis");
        System.err.println("           -s   create SHA <simfile> digest");
        System.err.println("           -p   package into one jar file");
        System.err.println("           -mcp model classpath (jar files not specified in sim)");
        System.err.println("           -l <loglevel> set the log level:");
        System.err.println("                OFF|ALL|SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST");
    }


    private static boolean isDSL(String file) {
        return simExt.contains(file.substring(file.lastIndexOf('.')));
    }


    private static Map<String, Object> getVarOverrides() {
        Properties p = System.getProperties();
        Map<String, Object> p1 = new HashMap<>();
        for (String key : p.stringPropertyNames()) {
            if (key.startsWith("dsl.var.")) {
                p1.put(key.substring("dsl.var.".length()), p.get(key));
            }
        }
        return p1.size() > 0 ? p1 : null;
    }


    public static void main(String[] args) {
        String ll = "OFF";
        String cmd = null;
        String file = null;
        try {
            log.setLevel(Level.OFF);
            for (int i = 0; i < args.length; i++) {
                if (flags.contains(args[i])) {
                    cmd = args[i];
                    file = args[++i];
                } else if (args[i].equals("-l")) {
                    ll = args[++i];
                } else {
                    usage();
                    return;
                }
            }
            if (file == null) {
                usage();
                return;
            }

            // set the properties
            setOMSProperties(file, cmd);

            // parse the file and get back the simulation
            Object target = groovy(file, ll);

            if (!isDSL(file)) {
                return;
            }

            // overwrite variables with some properties if
            // passed as system property (e.g. coef = 1.23):
            //   ...-Ddsl.var.coef=1.23 ...
            Map<String, Object> p = getVarOverrides();
            if (p != null) {
                if (target instanceof AbstractSimulation) {
                    ((AbstractSimulation) target).setVariableOverrides(p);
                }
            }

            if (cmd.equals("-r")) {
                invoke(target, "run");
            } else if (cmd.equals("-e")) {
                invoke(target, "edit");
            } else if (cmd.equals("-d")) {
                invoke(target, "doc");
            } else if (cmd.equals("-o")) {
                invoke(target, "output");
            } else if (cmd.equals("-a")) {
                invoke(target, "graph");
            } else if (cmd.equals("-s")) {
                invoke(target, "dig");
            } else if (cmd.equals("-b")) {
                invoke(target, "build");
            } else if (cmd.equals("-p")) {
                String oms_prj = System.getProperty("oms.prj");
                if (oms_prj == null) {
                    oms_prj = System.getProperty("user.dir");
                }
                String name = new File(file).getName();
                Jars.oneJar(new File(oms_prj, name.substring(0, name.lastIndexOf('.')) + ".jar").toString(), new File(file));
            }
        } catch (Throwable E) {
            Throwable origE = E;
            System.err.println();
            System.err.print(">>>> Error: ");
            if (log.getLevel() != Level.OFF) {
                // print the whole stack
                E.printStackTrace(System.err);
            } else {
                // ..or 
                while (!(E instanceof ComponentException) && E != null) {
                    E = E.getCause();
                }
                if (E == null) {
                    System.err.println("Internal Problem, please report to http://oms.javaforge.com");
                    origE.printStackTrace(System.err);
                    System.exit(1);
                }
                ComponentException ce = (ComponentException) E;
                if (ce.getCause() != null) {
                    // Exception within the model
                    System.err.println("Exception in component '" + ce.getSource() + "':");
                    ce.getCause().printStackTrace(System.err);
                } else {
                    // Exception within the system
                    System.err.println(ce.getMessage());
                }
            }
            System.exit(1);
        }
    }
}
