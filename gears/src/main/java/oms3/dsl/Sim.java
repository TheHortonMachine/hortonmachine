/*
 * $Id: Sim.java 7cba5ba59d73 2018-11-29 francesco.serafin.3@gmail.com $
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

import groovy.lang.Closure;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.*;

import oms3.ComponentAccess;
import oms3.annotations.*;
import oms3.Compound;
import oms3.Notification.*;

import oms3.doc.Documents;
import oms3.ngmf.util.OutputStragegy;
import oms3.ngmf.util.UnifiedParams;
import oms3.ngmf.util.Validation;
import oms3.ComponentException;
import oms3.util.Components;

/**
 * Core Simulation DSL
 *
 * @author od
 */
public class Sim extends AbstractSimulation {

    String alg = System.getProperty("oms3.digest.algorithm", "SHA-256");
    // The ontology
    Ontology ontology;
    List<Efficiency> eff = new ArrayList<Efficiency>();
    List<Summary> sum = new ArrayList<Summary>();
    File lastFolder;
    // Simulation resources.
    boolean digest = false;
    boolean sanitychecks = true;
    Closure pre;
    Closure post;


    /**
     * perform sanity checks for the model, default is true.
     *
     * @param sanitychecks enable/disable sanity check
     */
    public void setSanitychecks(boolean sanitychecks) {
        this.sanitychecks = sanitychecks;
    }


    public void setDigest(boolean digest) {
        this.digest = digest;
    }


    public void setPre(Closure pre) {
        this.pre = pre;
    }


    public void setPost(Closure post) {
        this.post = post;
    }


    @Override
    public Buildable create(Object name, Object value) {
        if (name.equals("ontology")) {
            return ontology = new Ontology();
        } else if (name.equals("efficiency")) {
            Efficiency e = new Efficiency();
            eff.add(e);
            return e;
        } else if (name.equals("summary")) {
            Summary e = new Summary();
            sum.add(e);
            return e;
//        } else if (name.equals("pre")) {
//            pre = (Closure) value;
//            return LEAF;
//        } else if (name.equals("post")) {
//            System.out.println("post here.");
//            post = (Closure) value;
//            System.out.println("post done.");
//            System.out.println(post);
//            return LEAF;
        }
        return super.create(name, value);
    }


    @Override
    public Object run() throws Exception {
        super.run();

        if (!sanitychecks) {
            System.setProperty("oms.skipCheck", "true");
        }

        if (log.isLoggable(Level.CONFIG)) {
            log.config("Run configuration ...");
        }

        if (digest) {
            String d = digest(res);
            System.setProperty("oms3.digest", d);
            if (log.isLoggable(Level.CONFIG)) {
                log.config("Setting system property 'oms3.digest' to " + d);
            }
        }

        // Path
        String libPath = model.getLibpath();
        if (libPath != null) {
            System.setProperty("jna.library.path", libPath);
            if (log.isLoggable(Level.CONFIG)) {
                log.config("Setting jna.library.path to " + libPath);
            }
        }

        Object comp = model.newModelComponent();
        if (log.isLoggable(Level.CONFIG)) {
            log.config("TL component " + comp);
        }

        Logger logger = Logger.getLogger("oms3.model");
        logger.setLevel(Level.OFF);
        // setup component logging
        Logging l = model.getComponentLogging();

        // 'sref' holds static references to Logger. If this 
        // array is missing then the weak references might get GC'ed
        // and the settings (loglevel) below are lost. The components 
        // will then get a new logger object!
        // the 'sref' just keeps the reference alive.
        List<Logger> sref = new ArrayList<>();
        sref.add(logger);

        Map<String, String> cl = l.getCompLevels();
        for (String compname : cl.keySet()) {
            String cll = cl.get(compname);
            Level level = Level.parse(cll);
            logger = Logger.getLogger("oms3.model." + compname);
            logger.setUseParentHandlers(false);
            logger.setLevel(level);

            if (log.isLoggable(Level.INFO)) {
                log.info("Set logger: '" + logger.getName() + "' to level " + logger.getLevel().toString());
            }

            ConsoleHandler ch = new ConsoleHandler();
            ch.setLevel(level);
            ch.setFormatter(new GenericBuilderSupport.CompLR());
            logger.addHandler(ch);
            sref.add(logger);
        }

        // call the prerun scripts
        if (pre != null) {
            pre.call(this);
        }

        if (log.isLoggable(Level.INFO)) {
            log.info("Init ...");
        }

        ComponentAccess.callAnnotated(comp, Initialize.class, true);

        // setting the input data;
        UnifiedParams parameter = model.getParameter();

        if (vars != null) {
            parameter.add(vars);
        }

        log.config("Input Parameter : " + parameter);

        boolean success = parameter.setInputData(comp, log);
        if (!success) {
            throw new ComponentException("There are Parameter problems. Simulation exits.");
        }

        lastFolder = getOutputPath();

        boolean adjusted = ComponentAccess.adjustOutputPath(lastFolder, comp, log);
        // only create this folder if there is a need.
        if (adjusted) {
            lastFolder.mkdirs();
        }

        if (comp instanceof Compound && log.isLoggable(Level.FINEST)) {
            Compound c = (Compound) comp;
            c.addListener(new Listener() {

                @Override
                public void notice(Type arg0, EventObject arg1) {
                    log.finest(arg0 + " -> " + arg1);
                }
            });
        }

        if (sanitychecks) {
            if (comp instanceof Compound) {
                Compound c = (Compound) comp;
                c.addListener(new Listener() {

                    @Override
                    public void notice(Type arg0, EventObject arg1) {
                        if (arg0 == Type.EXCEPTION) {
                            ExceptionEvent ee = (ExceptionEvent) arg1;
                            if (ee.getException() != null) {
                                log.severe(arg0 + " -> " + ee);
                            }
                        }
                    }
                });

                if (log.isLoggable(Level.FINE)) {
                    c.addListener(new Listener() {

                        @Override
                        public void notice(Type arg0, EventObject arg1) {
                            if (arg0 == Type.OUT) {
                                DataflowEvent e = (DataflowEvent) arg1;
                                Object v = e.getValue();
                                if (v == null) {
                                    log.fine("'null' output from " + e.getAccess().toString());
                                }
                            }
                        }
                    });
                }
            }
        }

        for (Efficiency e : eff) {
            e.setup(comp);
        }
        for (Summary e : sum) {
            e.setup(comp);
        }
        for (Output e : out) {
            e.setup(comp, lastFolder, getName());
        }

        // execute phases and be done.
        if (log.isLoggable(Level.INFO)) {
            log.info("Exec ...");
        }
        long t2 = System.currentTimeMillis();
        ComponentAccess.callAnnotated(comp, Execute.class, false);
        long t3 = System.currentTimeMillis();

        if (log.isLoggable(Level.INFO)) {
            log.info("Finalize ...");
        }
        ComponentAccess.callAnnotated(comp, Finalize.class, true);

        for (Efficiency e : eff) {
            e.printEff(lastFolder);
        }
        for (Summary e : sum) {
            e.printSum(lastFolder);
        }
        for (Output e : out) {
            e.done();
        }

        if (log.isLoggable(Level.INFO)) {
            log.info("Finished [" + (t3 - t2) + " ms]");
        }

        if (post != null) {
            post.call(this);
        }
        return comp;
    }

    @Override
    public void doc() throws Exception {
        OutputStragegy st = getOutput().getOutputStrategy(getName());
        st.lastOutputFolder().mkdirs();
        document(new File(st.lastOutputFolder(), getName() + ".xml"));
    }


    /**
     * Generate Simulation Documentation in Docbook 5.
     *
     * @param file
     * @throws Exception
     */
    void document(File file) throws Exception {
        Locale locale = Locale.getDefault();
        if (System.getProperty("oms3.locale.lang") != null) {
            locale = new Locale(System.getProperty("oms3.locale.lang"));
        }
        Documents.db5Sim(file, // output file
                model.newModelComponent().getClass(), // the model class
                model.getParameter().getParams(), // all merged parameter
                getName(),
                locale);// simulation name

        System.out.println(" Generated: " + file);
    }

    @Override
    public void dig() throws Exception {
        System.out.println("Digest [" + alg + "]:");
        System.out.println(digest());
    }


    /**
     * Get the simulation digest record.
     *
     * @return the digest record string
     * @throws Exception generic exception
     */
    public String digest() throws Exception {
        StringBuilder b = new StringBuilder();
        b.append(digest(res) + '\n');
        Collection<Class<?>> c = Components.internalComponents(model.newModelComponent().getClass());
        // model classes
        for (Class<?> cl : c) {
            b.append("    " + cl.getName() + " & ");
            SourceInfo si = (SourceInfo) cl.getAnnotation(SourceInfo.class);
            if (si != null) {
                b.append(si.value());
            }
            b.append(" ; ");
            VersionInfo vi = (VersionInfo) cl.getAnnotation(VersionInfo.class);
            if (vi != null) {
                b.append(vi.value());
            }
            b.append('\n');
        }
        // parameter files.
        for (File f : res.filterFiles("csv")) {
            b.append("    " + f.getName() + " & ");
            b.append('\n');
        }
        return b.toString();
    }


    /**
     * Create a message digest for the simulation
     *
     * @param r simulation resources
     * @return the digest as
     */
    private String digest(Resource r) {
        if (r.getRecources().isEmpty()) {
            return null;
        }
        List<File> f = new ArrayList<File>();
        for (String s : r.getRecources()) {
            f.add(new File(s));
        }
        return Validation.hexDigest(alg, f.toArray(new File[0]));
    }
}
