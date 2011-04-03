/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import oms3.CLI;
import oms3.util.Processes;

/** 
 *
 * @author od
 */
public class Exec implements Buildable {

    protected static final Logger log = Logger.getLogger("oms3.sim");

    /** execution types */
    public static enum Type {

        GROOVY,
        ANT,
        EXE, // not used yet
        JAVA, // not used yet
    }
    /** the file to execute */
    String file;
    /** The execution type */
    Type type;
    /** optional ant target to run, if null it runs the default target */
    String targets = "";

    public Exec(Type type) {
        this.type = type;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setTargets(String targets) {
        this.targets = targets;
    }

    public void run() throws Exception {
        File runFile = new File(file);
        if (!runFile.exists()) {
            throw new IllegalArgumentException("Not found : " + file);
        }
        if (log.isLoggable(Level.INFO)) {
            log.info("Executing: " + file);
        }
        switch (type) {
            // groovy execution
            case GROOVY:
                CLI.evaluateGroovyScript(runFile.getAbsolutePath());
                break;
            // Ant execution
            case ANT:
                String q = File.pathSeparatorChar == ';' ? "\"" : "";
                String version = System.getProperty("oms.version");
                String oms3Home = System.getProperty("oms.home");

                String[] antoptions = {};
                File conf = new File(System.getProperty("oms.prj") + File.separatorChar + ".oms", "oms.conf");
                if (conf.exists()) {
                    Properties p = new Properties();
                    FileReader fr = new FileReader(conf);
                    p.load(fr);
                    fr.close();
                    String jvmo = p.getProperty("ant.options");
                    if (jvmo != null) {
                        antoptions = jvmo.trim().split("\\s+");
                    }
                }

                Processes p = new Processes(new File(File.pathSeparatorChar == ';' ? "ant.bat" : "ant"));
                p.setVerbose(true);
                p.setArguments(antoptions,
                        "-Doms.version=" + version,
                        "-lib", q + oms3Home + q,
                        "-f", q + runFile.toString() + q,
                        targets.trim().split("\\s+"));
                p.redirectOutput(new PrintStream(System.out, true));
                p.redirectError(new PrintStream(System.out, true));
                try {
                    int exitValue = p.exec();
                    if (exitValue != 0) {
                        throw new RuntimeException("ant failed, simulation stopped.");
                    }
                } catch (IOException E) {
                    System.out.println("Cannot run 'ant', check installation and paths: " + E.getMessage());
                }
                break;
            default:
                throw new RuntimeException("Unknown Execution type. " + type);
        }
    }

    @Override
    public Buildable create(Object name, Object value) {
        return LEAF;
    }
//    public static void main(String[] args) {
//                Project p = new Project();
//                p.setUserProperty("ant.file", runFile.getAbsolutePath());
//                DefaultLogger consoleLogger = new DefaultLogger();
//                // TODO fix error output
//                consoleLogger.setErrorPrintStream(System.out);
//                consoleLogger.setOutputPrintStream(System.out);
//                consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
//                p.addBuildListener(consoleLogger);
//                try {
//                    p.fireBuildStarted();
//                    p.init();
//                    ProjectHelper helper = ProjectHelper.getProjectHelper();
//                    p.addReference("ant.projectHelper", helper);
//                    p.setProperty("oms.version", oms3.Utils.getVersion());
//                    helper.parse(p, runFile);
//                    p.executeTarget(target == null ? p.getDefaultTarget() : target);
//                    p.fireBuildFinished(null);
//                } catch (BuildException E) {
//                    p.fireBuildFinished(E);
//                }
//        String jvmo = "";
//        String[] jvmoptions = jvmo.trim().split("\\s+");
//        System.out.println(Arrays.toString(jvmoptions));
//    }
}
