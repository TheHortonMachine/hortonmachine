/*
 * $Id: Exec.java 2fb4d513ea17 2014-06-04 odavid@colostate.edu $
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import oms3.CLI;
import oms3.ComponentException;
import oms3.util.ProcessExecution;

/**
 *
 * @author od
 */
public class Exec extends AbstractBuildableLeaf {

    protected static final Logger log = Logger.getLogger("oms3.sim");

    /**
     * execution types
     */
    public static enum Type {

        GROOVY,
        ANT,
        EXE, // not used yet
        JAVA, // not used yet
    }
    /**
     * the file to execute
     */
    String file;
    /**
     * The execution type
     */
    Type type;
    /**
     * optional ant target to run, if null it runs the default target
     */
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

    public void setTarget(String targets) {
        this.targets = targets;
    }

    public String getTargets() {
        return targets;
    }

    public void run() throws Exception {
        File runFile = new File(file);
        if (!runFile.exists()) {
            throw new ComponentException("Not found : " + file);
        }
        if (log.isLoggable(Level.INFO)) {
            log.info("Executing: " + file);
        }
        switch (type) {
            // groovy execution
            case GROOVY:
                CLI.groovy(runFile.getAbsolutePath(), log.getLevel().toString());
                break;
            // Ant execution
            case ANT:
                String q = File.pathSeparatorChar == ';' ? "\"" : "";
                String version = System.getProperty("oms.version");
                String oms3Home = System.getProperty("oms.home");

                String[] antoptions = {};
                File conf = new File(System.getProperty("oms.prj") + File.separatorChar + ".oms", "project.properties");
                if (conf.exists()) {
                    Properties p = new Properties();
                    FileReader fr = new FileReader(conf);
                    p.load(fr);
                    fr.close();
                    String opt = p.getProperty("ant.options");
                    if (log.isLoggable(Level.INFO)) {
                        log.info("ant.options: " + opt);
                    }
                    if (opt != null) {
                        antoptions = opt.trim().split("\\s+");
                    }
                }
                ProcessExecution p = new ProcessExecution(new File(File.pathSeparatorChar == ';' ? "ant.bat" : "ant"));
                p.setLogger(log);
                p.setArguments(antoptions,
                        "-Doms.version=" + version,
                        "-lib", q + oms3Home + q,
                        "-f", q + runFile.toString() + q,
                        targets.trim().split("\\s+"));

                p.redirectOutput(new Writer() {
                    @Override
                    public void write(char[] cbuf, int off, int len) throws IOException {
                        System.out.println(new String(cbuf, off, len));
                    }

                    @Override
                    public void flush() throws IOException {
                        System.out.flush();
                    }

                    @Override
                    public void close() throws IOException {
                    }
                });

                p.redirectError(new Writer() {
                    @Override
                    public void write(char[] cbuf, int off, int len) throws IOException {
                        System.err.println(new String(cbuf, off, len));
                    }

                    @Override
                    public void flush() throws IOException {
                        System.err.flush();
                    }

                    @Override
                    public void close() throws IOException {
                    }
                });

                try {
                    if (log.isLoggable(Level.INFO)) {
                        log.info("Starting ...");
                    }
                    int exitValue = p.exec();
                    if (log.isLoggable(Level.INFO)) {
                        log.info("Finished with exit code " + exitValue);
                    }
                    if (exitValue != 0) {
                        throw new ComponentException("ant failed, simulation stopped.");
                    }
                } catch (IOException E) {
                    throw new ComponentException("Check your 'PATH', " + E.getMessage());
                }
                break;
            default:
                throw new ComponentException("Unknown Execution type. " + type);
        }
    }
}
