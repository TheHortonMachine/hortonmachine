/*
 * $Id: AbstractSimulation.java 7cba5ba59d73 2018-11-29 francesco.serafin.3@gmail.com $
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

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import oms3.ngmf.util.OutputStragegy;
import oms3.ComponentException;

/**
 *
 * @author od
 */
abstract public class AbstractSimulation implements Buildable {

    protected static final Logger log = Logger.getLogger("oms3.sim");
    //
    Model model;
    String name;
    Resource res = new Resource();
    private OutputDescriptor output = new OutputDescriptor();
    List<Output> out = new ArrayList<>();
    Exec build;
    // model component (to be accessible from script)
    Object modelComp;
    private File op;
    Map<String, Object> vars;
    KVPContainer sysprops = new KVPContainer();


    public void setName(String name) {
        this.name = name;
    }


    public void setModelComponent(Object model) {
        modelComp = model;
    }


    public File getOutputPath() {
        if (op == null) {
            OutputStragegy st = output.getOutputStrategy(getName());
            op = st.nextOutputFolder();
            if (log.isLoggable(Level.CONFIG)) {
                log.config("Simulation output folder: " + op);
            }
        }
        return op;
    }


    public Object getModel() {
        if (model == null) {
            throw new ComponentException("No model component available as a part of this simulation.");
        }
        return modelComp;
    }


    protected String getName() {
        if (name != null) {
            return name;
        } else if (System.getProperty("oms.script") != null) {
            String s = System.getProperty("oms.script");
            s = new File(s).getName();
            if (s.contains(".")) {
                s = s.substring(0, s.indexOf("."));
            }
            name = s;
        } else {
            name = getClass().getSimpleName();
        }
        return name;
    }


    public Model getModelElement() {
        return model;
    }


    protected OutputDescriptor getOutput() {
        return output;
    }


    protected List<Output> getOut() {
        return out;
    }


    public void setVariableOverrides(Map<String, Object> vars) {
        this.vars = vars;
    }


    void setSystemProperties() {
        if (!sysprops.hasEntries()) {
            return;
        }
        for (KVP kvp : sysprops.getEntries()) {
            System.setProperty(kvp.getKey(), kvp.getValue().toString());
            log.config("Setting system property: " + kvp.getKey() + "=" + kvp.getValue());
        }
    }


    @Override
    public Buildable create(Object name, Object value) {
        if (name.equals(Model.DSL_NAME)) {
            if (model != null) {
                throw new ComponentException("Only one 'model' element allowed.");
            }
            model = new Model();
            model.setRes(res);
            return model;
        } else if (name.equals("systemproperties")) {
            return sysprops;
        } else if (name.equals("resource")) {
            res.addResource(value);
            return LEAF;
        } else if (name.equals(Output.DSL_NAME)) {
            Output e = new Output();
            out.add(e);
            return e;
        } else if (name.equals("outputstrategy")) {
            return output;
        } else if (name.equals("build")) {
            File buildFile = new File(System.getProperty("oms.prj") + File.separatorChar + "build.xml");
            if (!buildFile.exists()) {
                throw new ComponentException("No build file found: " + buildFile);
            }
            build = new Exec(Exec.Type.ANT);
            build.setFile(buildFile.getAbsolutePath());
            return build;
        }
        throw new ComponentException("Unknown element '" + name.toString() + "'");
    }
    
    protected void initRun() {
        setSystemProperties();
        if (getModelElement() == null) {
            throw new ComponentException("missing 'model' element.");
        }
    }


    public Object run() throws Exception {
        initRun();
        return null;
    }


    public void graph() throws Exception {
        throw new UnsupportedOperationException("Not supported.");
    }


    public void doc() throws Exception {
        throw new UnsupportedOperationException("Not supported.");
    }


    public void dig() throws Exception {
        throw new UnsupportedOperationException("Not supported.");
    }


    public void edit() throws Exception {
        throw new UnsupportedOperationException("Not supported.");
    }


    public void build() throws Exception {
        if (build != null) {
            build.run();
        } else {
            System.err.println("  No build file to run.");
        }
    }


    public void output() throws Exception {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                OutputStragegy st = output.getOutputStrategy(getName());
                File lastFolder = st.lastOutputFolder();
                if (lastFolder.exists()) {
                    desktop.open(lastFolder);
                } else {
                    log.warning("Folder does not exist (yet): " + lastFolder);
                }
            }
        }
    }
}
