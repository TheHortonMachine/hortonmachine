/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.UIManager;
import ngmf.util.OutputStragegy;
import oms3.dsl.analysis.Chart;

/**
 *
 * @author od
 */
abstract public class AbstractSimulation implements Buildable {

    protected static final Logger log = Logger.getLogger("oms3.sim");
    Model model;
    String name;
    Resource res = new Resource();
    OutputDescriptor output = new OutputDescriptor();
    List<Output> out = new ArrayList<Output>();
    Chart analysis;

    public void setName(String name) {
        this.name = name;
    }

    protected String getName() {
        return name == null ? getClass().getSimpleName() : name;
    }

    protected Model getModel() {
        return model;
    }

    protected OutputDescriptor getOutput() {
        return output;
    }

    protected List<Output> getOut() {
        return out;
    }

    protected void handleException(Throwable ex) throws RuntimeException {
        log.severe(ex.getClass().getName() + " " + ex.getMessage());
        ex.printStackTrace(System.out);     // there is no stack trace within NB.
    }

    @Override
    public Buildable create(Object name, Object value) {
        if (name.equals("model")) {
            if (model != null) {
                throw new IllegalArgumentException("Only one 'model' allowed.");
            }
            model = new Model();
            model.setRes(res);
            return model;
        } else if (name.equals("resource")) {
            res.addResource(value);
            return LEAF;
        } else if (name.equals("output")) {
            Output e = new Output();
            out.add(e);
            return e;
        } else if (name.equals("analysis")) {
            return analysis = new Chart();
        } else if (name.equals("outputstrategy")) {
            return output;
        }
        throw new IllegalArgumentException(name.toString());
    }

    static void nativeLF() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            String osName = System.getProperty("os.name");
            if ((osName != null) && osName.toLowerCase().startsWith("lin")) {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
            }
        } catch (Exception E) {
            System.out.println("Cannot set native L&F.");
        }
    }

    public Object run() throws Exception {
        throw new UnsupportedOperationException("Not supported.");
    }

    public void graph() throws Exception {
        if (analysis != null) {
            OutputStragegy st = getOutput().getOutputStrategy(getName());
            nativeLF();
            analysis.run(st, getName());
        } else {
            System.out.println("No analysis defined.");
        }
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

    public void output() throws Exception {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                OutputStragegy st = output.getOutputStrategy(getName());
                File lastFolder = st.lastOutputFolder();
                if (lastFolder.exists()) {
                    desktop.open(lastFolder);
                } else {
                    System.out.println("Folder does not exist (yet): " + lastFolder);
                }
            }
        }
    }
}
