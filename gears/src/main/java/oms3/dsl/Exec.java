///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package oms3.dsl;
//
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.Writer;
//import java.util.Properties;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import oms3.CLI;
//import oms3.ComponentException;
//import oms3.util.Processes1;
//
///** 
// *
// * @author od
// */
//public class Exec implements Buildable {
//
//    protected static final Logger log = Logger.getLogger("oms3.sim");
//
//    /** execution types */
//    public static enum Type {
//
//        GROOVY,
//        ANT,
//        EXE, // not used yet
//        JAVA, // not used yet
//    }
//    /** the file to execute */
//    String file;
//    /** The execution type */
//    Type type;
//    /** optional ant target to run, if null it runs the default target */
//    String targets = "";
//
//    public Exec(Type type) {
//        this.type = type;
//    }
//
//    public void setFile(String file) {
//        this.file = file;
//    }
//
//    public void setTargets(String targets) {
//        this.targets = targets;
//    }
//
//    public void setTarget(String targets) {
//        this.targets = targets;
//    }
//
//    public String getTargets() {
//        return targets;
//    }
//
//    public void run() throws Exception {
//        File runFile = new File(file);
//        if (!runFile.exists()) {
//            throw new ComponentException("Not found : " + file);
//        }
//        if (log.isLoggable(Level.INFO)) {
//            log.info("Executing: " + file);
//        }
//        switch (type) {
//            // groovy execution
//            case GROOVY:
//                CLI.evaluateGroovyScript(runFile.getAbsolutePath());
//                break;
//            // Ant execution
//            case ANT:
//                String q = File.pathSeparatorChar == ';' ? "\"" : "";
//                String version = System.getProperty("oms.version");
//                String oms3Home = System.getProperty("oms.home");
//
//                String[] antoptions = {};
//                File conf = new File(System.getProperty("oms.prj") + File.separatorChar + ".oms", "project.properties");
//                if (conf.exists()) {
//                    Properties p = new Properties();
//                    FileReader fr = new FileReader(conf);
//                    p.load(fr);
//                    fr.close();
//                    String opt = p.getProperty("ant.options");
//                    if (log.isLoggable(Level.INFO)) {
//                        log.info("ant.options: " + opt);
//                    }
//                    if (opt != null) {
//                        antoptions = opt.trim().split("\\s+");
//                    }
//                }
//                Processes1 p = new Processes1(new File(File.pathSeparatorChar == ';' ? "ant.bat" : "ant"));
//                p.setLog(log);
//                p.setArguments(antoptions,
//                        "-Doms.version=" + version,
//                        "-lib", q + oms3Home + q,
//                        "-f", q + runFile.toString() + q,
//                        targets.trim().split("\\s+"));
//
//                p.redirectOutput(new Writer() {
//
//                    @Override
//                    public void write(char[] cbuf, int off, int len) throws IOException {
//                        System.out.println(new String(cbuf, off, len));
//                    }
//
//                    @Override
//                    public void flush() throws IOException {
//                        System.out.flush();
//                    }
//
//                    @Override
//                    public void close() throws IOException {
//                    }
//                });
//
//                p.redirectError(new Writer() {
//
//                    @Override
//                    public void write(char[] cbuf, int off, int len) throws IOException {
//                        System.err.println(new String(cbuf, off, len));
//                    }
//
//                    @Override
//                    public void flush() throws IOException {
//                        System.err.flush();
//                    }
//
//                    @Override
//                    public void close() throws IOException {
//                    }
//                });
//
//                try {
//                    if (log.isLoggable(Level.INFO)) {
//                        log.info("Starting ...");
//                    }
//                    int exitValue = p.exec();
//                    if (log.isLoggable(Level.INFO)) {
//                        log.info("Finished with exit code " + exitValue);
//                    }
//                    if (exitValue != 0) {
//                        throw new ComponentException("ant failed, simulation stopped.");
//                    }
//                } catch (IOException E) {
//                    throw new ComponentException("Check your 'PATH', " + E.getMessage());
//                }
//                break;
//            default:
//                throw new ComponentException("Unknown Execution type. " + type);
//        }
//    }
//
//    @Override
//    public Buildable create(Object name, Object value) {
//        return LEAF;
//    }
//}
