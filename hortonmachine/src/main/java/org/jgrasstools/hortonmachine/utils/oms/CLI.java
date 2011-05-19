/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jgrasstools.hortonmachine.utils.oms;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command Line interface to run simulations.
 *
 * @author od
 */
public class CLI {

    static {
        if (System.getProperty("java.version").compareTo("1.6") < 0) {
            throw new RuntimeException("Java 1.6+ required.");
        }
    }

    private CLI() {
    }

    /**
     * Executes a simulation.
     * 
     * @param file the file to execute
     * @param ll the log level
     * @param cmd the command to call (e.g. run)
     * @throws Exception
     */
    public static Object sim(String file, String ll, String cmd) throws Exception {
        String f = CLI.readFile(file);
        Object o = CLI.createSim(f, false, ll);
        return CLI.invoke(o, cmd);
    }

    /**
     * Executed plain groovy.
     * 
     * @param file the groovy file
     * @param ll the log level.
     * @param cmd
     * @throws Exception
     */
    public static void groovy(String file, String ll, String cmd) throws Exception {
        String f = CLI.readFile(file);
        Object o = CLI.createSim(f, true, ll);
    }

    /**
     * Invokes a simulation method. (run | doc | analysis | ...)
     * 
     * @param target the target simulation object
     * @param name the name of the method (eg. run())
     * @throws Exception
     */
    public static Object invoke(Object target, String name) throws Exception {
        return target.getClass().getMethod(name).invoke(target);
    }

    /**
     * Read a file and provide its content as String.
     * 
     * @param name the file name
     * @return the content as String
     * @throws IOException something bad happened.
     */
    public static String readFile(String name) throws IOException {
        StringBuilder b = new StringBuilder();
        BufferedReader r = new BufferedReader(new FileReader(name));
        String line;
        while ((line = r.readLine()) != null) {
            b.append(line).append('\n');
        }
        r.close();
        return b.toString();
    }

    /**
     * Create a simulation object.
     * 
     * @param script the script 
     * @param groovy
     * @param ll
     * @return the simulation object.
     */
    public static Object createSim(String script, boolean groovy, String ll) throws Exception {
        setOMSProperties();
        Level.parse(ll);                            // may throw IAE
        String prefix = groovy ? ""
                : "import static oms3.SimConst.*\n"
                + "def __sb__ = new oms3.SimBuilder(logging:'" + ll + "')\n"
                + "__sb__.";
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        Binding b = new Binding();
        b.setVariable("oms_version", System.getProperty("oms.version"));
        b.setVariable("oms_home", System.getProperty("oms.home"));
        b.setVariable("oms_prj", System.getProperty("oms.prj"));

        GroovyShell shell = new GroovyShell(new GroovyClassLoader(parent), b);
        return shell.evaluate(prefix + script);
    }

    public static Object evaluateGroovyScript(String file) throws Exception {
        String content = readFile(file);
        return createSim(content, true, "OFF");
    }

    private static void setOMSProperties() {
        String oms_work = System.getProperty("oms3.work");
        if (oms_work != null) {
            System.setProperty("oms.prj", oms_work);
        }
        System.setProperty("oms.version", oms3.Utils.getVersion());
        if (System.getProperty("oms.home") == null) {
            System.setProperty("oms.home", System.getProperty("user.home")
                    + File.separator + ".oms" + File.separator + oms3.Utils.getVersion());
        }
    }

    private static void usage() {
        System.err.println("usage: java -jar oms-all.jar [-l <loglevel> ] [-r|-e|-d|-a|-s|-o] <simfile>");
        System.err.println(" Command line access to simulations.");
        System.err.println("           -r   run the <simfile>");
        System.err.println("           -e   edit parameter in <simfile>");
        System.err.println("           -o   open the last output folder in desktop <simfile>");
        System.err.println("           -d   document the <simfile>");
        System.err.println("           -a   run the <simfile> analysis");
        System.err.println("           -s   create SHA <simfile> digest");
        System.err.println("           -mcp model classpath (jar files not specified in sim)");
        System.err.println("           -l <loglevel> set the log level:");
        System.err.println("                OFF|ALL|SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST");
    }

    static List<String> simExt = Arrays.asList(".sim", ".luca", ".esp", ".fast");
    static List<String> flags = Arrays.asList("-r", "-e", "-d", "-o", "-a", "-s");

    static boolean isSim(String file) {
        return simExt.contains(file.substring(file.lastIndexOf('.')));
    }

    public static void main(String[] args) {
        String ll = "WARNING";
        String cmd = null;
        String file = null;
        try {
            for (int i = 0; i < args.length; i++) {
                if (flags.contains(args[i])) {
                    cmd = args[i];
                    file = args[++i];
//                }
//                if (args[i].equals("-r")) {
//                    boolean isgroovy = !isSim(args[++i]);
//                    Logger log = Logger.getLogger("oms3.sim");
//                    log.setLevel(Level.parse(ll));
//                    Object target = createSim(readFile(args[i]), isgroovy, ll);
//                    if (!isgroovy) {
//                        invoke(target, "run");
//                    }
//                } else if (args[i].equals("-e")) {
//                    Object target = createSim(readFile(args[++i]), false, ll);
//                    invoke(target, "edit");
//                } else if (args[i].equals("-d")) {
//                    Object target = createSim(readFile(args[++i]), false, ll);
//                    invoke(target, "doc");
//                } else if (args[i].equals("-o")) {
//                    Object target = createSim(readFile(args[++i]), false, ll);
//                    invoke(target, "output");
//                } else if (args[i].equals("-a")) {
//                    Object target = createSim(readFile(args[++i]), false, ll);
//                    invoke(target, "graph");
//                } else if (args[i].equals("-s")) {
//                    Object target = createSim(readFile(args[++i]), false, ll);
//                    invoke(target, "dig");
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
            boolean isgroovy = !isSim(file);
            Logger log = Logger.getLogger("oms3.sim");
            log.setLevel(Level.parse(ll));
            Object target = createSim(readFile(file), isgroovy, ll);

            if (cmd.equals("-r")) {
                if (!isgroovy) {
                    invoke(target, "run");
                }
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
            }
        } catch (Exception E) {
            E.printStackTrace(System.out);
            System.exit(1);
        }
    }
}
