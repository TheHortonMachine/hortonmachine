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
import oms3.ComponentException;
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
    //
    RunContainer pre;
    RunContainer post;

    private RunContainer lazyPre() {
        if (pre == null) {
            pre = new RunContainer();
        }
        return pre;
    }

    private RunContainer lazyPost() {
        if (post == null) {
            post = new RunContainer();
        }
        return post;
    }

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

    @Override
    public Buildable create(Object name, Object value) {
        if (name.equals("model")) {
            if (model != null) {
                throw new ComponentException("Only one 'model' element allowed.");
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
        } else if (name.equals("pre")) {
            return lazyPre();
        } else if (name.equals("post")) {
            return lazyPost();
        } else if (name.equals("build")) {
            File buildFile = new File(System.getProperty("oms.prj") + File.separatorChar + "build.xml");
            if (!buildFile.exists()) {
                throw new ComponentException("No build file found: " + buildFile);
            }
            Exec e = new Exec(Exec.Type.ANT);
            e.setFile(buildFile.getAbsolutePath());
            lazyPre().l.add(e);
            return e;
        }
        throw new ComponentException("Unknown element '" + name.toString() + "'");
    }

    static void nativeLF() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            String osName = System.getProperty("os.name");
            if ((osName != null) && osName.toLowerCase().startsWith("lin")) {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
            }
        } catch (Exception E) {
            log.warning("Cannot set native L&F.");
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
            throw new ComponentException("No analysis element defined.");
        }
    }

    protected void doPreRuns() throws Exception {
        if (pre == null)
            return;
        pre.run();
    }

    protected void doPostRuns() throws Exception {
        if (post == null)
            return;
        post.run();
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
                    log.warning("Folder does not exist (yet): " + lastFolder);
                }
            }
        }
    }
}
