///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package oms3;
//
//import groovy.lang.Binding;
//import groovy.lang.GroovyClassLoader;
//import groovy.lang.GroovyRuntimeException;
//import groovy.lang.GroovyShell;
//import groovy.lang.MissingPropertyException;
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.List;
//import java.util.logging.Handler;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import org.codehaus.groovy.control.MultipleCompilationErrorsException;
//import org.codehaus.groovy.syntax.SyntaxException;
//
///**
// * Command Line interface to run simulations.
// *
// * @author od
// */
//public class CLI {
//
//    protected static final Logger log = Logger.getLogger("oms3.sim");
//    //
//    static final List<String> simExt = Arrays.asList(".sim", ".luca", ".esp", ".fast");
//    static final List<String> flags = Arrays.asList("-r", "-e", "-d", "-o", "-a", "-s", "-b");
//
//    static {
//        if (System.getProperty("java.version").compareTo("1.6") < 0) {
//            throw new RuntimeException("Java 1.6+ required.");
//        }
//        Logger l0 = Logger.getLogger("");
//        Handler[] handlers = l0.getHandlers();
//        for( Handler handler : handlers ) {
//            l0.removeHandler(handler);
//        }
//    }
//
//    private CLI() {
//    }
//
//    /**
//     * Executes a simulation.
//     * 
//     * @param file the file to execute
//     * @param ll the log level
//     * @param cmd the command to call (e.g. run)
//     * @throws Exception
//     */
//    public static Object sim( String file, String ll, String cmd ) throws Exception {
//        String f = readFile(file);
//        Object o = createSim(f, false, ll, file);
//        return invoke(o, cmd);
//    }
//
//    /**
//     * Executed plain groovy.
//     * 
//     * @param file the groovy file
//     * @param ll the log level.
//     * @param cmd
//     * @throws Exception
//     */
//    public static void groovy( String file, String ll, String cmd ) throws Exception {
//        String f = readFile(file);
//        Object o = createSim(f, true, ll, file);
//    }
//
//    /**
//     * Invokes a simulation method. (run | doc | analysis | ...)
//     * 
//     * @param target the target simulation object
//     * @param name the name of the method (eg. run())
//     * @throws Exception
//     */
//    public static Object invoke( Object target, String name ) throws Exception {
//        return target.getClass().getMethod(name).invoke(target);
//    }
//
//    /**
//     * Read a file and provide its content as String.
//     * 
//     * @param name the file name
//     * @return the content as String
//     * @throws IOException something bad happened.
//     */
//    public static String readFile( String name ) {
//        StringBuilder b = new StringBuilder();
//        try {
//            BufferedReader r = new BufferedReader(new FileReader(name));
//            String line;
//            while( (line = r.readLine()) != null ) {
//                b.append(line).append('\n');
//            }
//            r.close();
//        } catch (IOException E) {
//            throw new ComponentException(E.getMessage());
//        }
//        return b.toString();
//    }
//
//    public static Object createSim( String script, boolean groovy, String ll ) {
//        return createSim(script, groovy, ll, null);
//    }
//
//    /**
//     * Create a simulation object.
//     * 
//     * @param script the script 
//     * @param groovy
//     * @param ll
//     * @return the simulation object.
//     */
//    public static Object createSim( String script, boolean groovy, String ll, String file ) {
//        file = (file == null) ? "unknown" : file;
//        setOMSProperties();
//        Level.parse(ll); // may throw IAE
//        String prefix = groovy ? "" : "import static oms3.SimConst.*\n" + "def __sb__ = new oms3.SimBuilder(logging:'" + ll
//                + "')\n" + "__sb__.";
//        ClassLoader parent = Thread.currentThread().getContextClassLoader();
//        Binding b = new Binding();
//        b.setVariable("oms_version", System.getProperty("oms.version"));
//        b.setVariable("oms_home", System.getProperty("oms.home"));
//        b.setVariable("oms_prj", System.getProperty("oms.prj"));
//        GroovyShell shell = new GroovyShell(new GroovyClassLoader(parent), b);
//
//        try {
//            return shell.evaluate(prefix + script);
//        } catch (MultipleCompilationErrorsException E) {
//            int n = E.getErrorCollector().getErrorCount();
//            if (n > 0) {
//                SyntaxException syn = E.getErrorCollector().getSyntaxError(0);
//                int line = syn.getLine() + (groovy ? 0 : -2);
//                throw new ComponentException(new File(file).getName() + " [line:" + line + " column:" + syn.getStartColumn()
//                        + "]  " + syn.getOriginalMessage());
//            } else {
//                throw E;
//            }
//        } catch (MissingPropertyException E) {
//            throw new ComponentException("Cannot handle property '" + E.getProperty() + "' in " + file);
//        } catch (GroovyRuntimeException E) {
//            throw new ComponentException(E.getMessage() + " in '" + file + "'");
//        }
//    }
//
//    public static Object evaluateGroovyScript( String file ) {
//        String content = readFile(file);
//        return createSim(content, true, "OFF", file);
//    }
//
//    private static void setOMSProperties() {
//        String oms_work = System.getProperty("oms3.work");
//        if (oms_work != null) {
//            System.setProperty("oms.prj", oms_work);
//        }
//        System.setProperty("oms.version", oms3.Utils.getVersion());
//        if (System.getProperty("oms.home") == null) {
//            System.setProperty("oms.home", System.getProperty("user.home") + File.separator + ".oms" + File.separator
//                    + oms3.Utils.getVersion());
//        }
//    }
//
//    private static void usage() {
//        System.err.println("usage: java -jar oms-all.jar [-l <loglevel> ] [-r|-e|-d|-a|-s|-o] <simfile>");
//        System.err.println(" Command line access to simulations.");
//        System.err.println("           -r   run the <simfile>");
//        System.err.println("           -e   edit parameter in <simfile>");
//        System.err.println("           -o   open the last output folder in desktop <simfile>");
//        System.err.println("           -d   document the <simfile>");
//        System.err.println("           -a   run the <simfile> analysis");
//        System.err.println("           -s   create SHA <simfile> digest");
//        System.err.println("           -mcp model classpath (jar files not specified in sim)");
//        System.err.println("           -l <loglevel> set the log level:");
//        System.err.println("                OFF|ALL|SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST");
//    }
//
//    static boolean isSim( String file ) {
//        return simExt.contains(file.substring(file.lastIndexOf('.')));
//    }
//
//    public static void main( String[] args ) {
//        String ll = "OFF";
//        String cmd = null;
//        String file = null;
//        try {
//            log.setLevel(Level.OFF);
//            for( int i = 0; i < args.length; i++ ) {
//                if (flags.contains(args[i])) {
//                    cmd = args[i];
//                    file = args[++i];
//                } else if (args[i].equals("-l")) {
//                    ll = args[++i];
//                } else {
//                    usage();
//                    return;
//                }
//            }
//            if (file == null) {
//                usage();
//                return;
//            }
//
//            try {
//                log.setLevel(Level.parse(ll));
//            } catch (IllegalArgumentException E) {
//                throw new ComponentException(E.getMessage());
//            }
//
//            boolean isgroovy = !isSim(file);
//            Object target = createSim(readFile(file), isgroovy, ll, file);
//
//            // ignore all
//            if (isgroovy) {
//                return;
//            }
//
//            if (cmd.equals("-r")) {
//                invoke(target, "run");
//            } else if (cmd.equals("-e")) {
//                invoke(target, "edit");
//            } else if (cmd.equals("-d")) {
//                invoke(target, "doc");
//            } else if (cmd.equals("-o")) {
//                invoke(target, "output");
//            } else if (cmd.equals("-a")) {
//                invoke(target, "graph");
//            } else if (cmd.equals("-s")) {
//                invoke(target, "dig");
//            } else if (cmd.equals("-b")) {
//                invoke(target, "build");
//            }
//        } catch (Throwable E) {
//            // Throwable origE = E;
//            System.err.println();
//            System.err.println("ERROR");
//            System.err.println("---------------------------------------------");
//            Level level = log.getLevel();
//            if (level != Level.OFF) {
//                // print the whole stack
//                E.printStackTrace(System.err);
//            } else {
//                Throwable cause = E.getCause();
//                String localizedMessage;
//                if (cause != null) {
//                    localizedMessage = cause.getLocalizedMessage();
//                } else {
//                    localizedMessage = E.getLocalizedMessage();
//                }
//                String[] split = localizedMessage.split(":");
//                if (split.length > 1) {
//                    if (split[0].contains(".")) {
//                        StringBuilder sb = new StringBuilder();
//                        for( int i = 1; i < split.length; i++ ) {
//                            sb.append(":").append(split[i]);
//                        }
//                        localizedMessage = sb.substring(1);
//                    }
//                }
//                System.err.println(localizedMessage);
//                // // ..or
//                // while (!(E instanceof ComponentException) && E != null) {
//                // E = E.getCause();
//                // }
//                // if (E == null) {
//                // System.err.println("Internal Problem, please report to http://oms.javaforge.com");
//                // origE.printStackTrace(System.err);
//                // System.exit(1);
//                // }
//                // ComponentException ce = (ComponentException) E;
//                // if (ce.getCause() != null) {
//                // // Exception within the model
//                // System.err.println("Exception in component '" + ce.getSource() + "':");
//                // ce.getCause().printStackTrace(System.err);
//                // } else {
//                // // Exception within the system
//                // System.err.println(ce.getMessage());
//                // }
//            }
//            System.exit(1);
//        }
//    }
//}
