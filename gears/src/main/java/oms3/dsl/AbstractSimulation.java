///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package oms3.dsl;
//
//import java.awt.Desktop;
//import java.awt.Image;
//import java.awt.Toolkit;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.logging.Logger;
//
//import javax.swing.JFrame;
//import javax.swing.UIManager;
//
//import ngmf.ui.PEditor;
//import ngmf.util.OutputStragegy;
//import oms3.ComponentAccess;
//import oms3.ComponentException;
//import oms3.io.CSProperties;
//import oms3.io.DataIO;
//
///**
// *
// * @author od
// */
//abstract public class AbstractSimulation implements Buildable {
//
//    protected static final Logger log = Logger.getLogger("oms3.sim");
//    Model model;
//    String name;
//    Resource res = new Resource();
//    OutputDescriptor output = new OutputDescriptor();
//    List<Output> out = new ArrayList<Output>();
////    Chart analysis;
//    //
//    Exec build;
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    protected String getName() {
//        return name == null ? getClass().getSimpleName() : name;
//    }
//
//    public Model getModel() {
//        return model;
//    }
//
//    protected OutputDescriptor getOutput() {
//        return output;
//    }
//
//    protected List<Output> getOut() {
//        return out;
//    }
//
//    @Override
//    public Buildable create(Object name, Object value) {
//        if (name.equals("model")) {
//            if (model != null) {
//                throw new ComponentException("Only one 'model' element allowed.");
//            }
//            model = new Model();
//            model.setRes(res);
//            return model;
//        } else if (name.equals("resource")) {
//            res.addResource(value);
//            return LEAF;
//        } else if (name.equals("output")) {
//            Output e = new Output();
//            out.add(e);
//            return e;
//        } else if (name.equals("analysis")) {
////            return analysis = new Chart();
//        } else if (name.equals("outputstrategy")) {
//            return output;
//        } else if (name.equals("build")) {
//            File buildFile = new File(System.getProperty("oms.prj") + File.separatorChar + "build.xml");
//            if (!buildFile.exists()) {
//                throw new ComponentException("No build file found: " + buildFile);
//            }
//            build = new Exec(Exec.Type.ANT);
//            build.setFile(buildFile.getAbsolutePath());
//            return build;
//        }
//        throw new ComponentException("Unknown element '" + name.toString() + "'");
//    }
//
//    static void nativeLF() {
//        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//            String osName = System.getProperty("os.name");
//            if ((osName != null) && osName.toLowerCase().startsWith("lin")) {
//                UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
//            }
//        } catch (Exception E) {
//            log.warning("Cannot set native L&F.");
//        }
//    }
//
//    public Object run() throws Exception {
//        throw new UnsupportedOperationException("Not supported.");
//    }
//
//    public void graph() throws Exception {
////        if (analysis != null) {
////            OutputStragegy st = getOutput().getOutputStrategy(getName());
////            nativeLF();
////            analysis.run(st, getName());
////        } else {
//            throw new ComponentException("No analysis element defined.");
////        }
//    }
//
//    public void doc() throws Exception {
//        throw new UnsupportedOperationException("Not supported.");
//    }
//
//    public void dig() throws Exception {
//        throw new UnsupportedOperationException("Not supported.");
//    }
//
//     /** Edit parameter file content. Edit only the 
//     * 
//     * @throws Exception
//     */
//    public void edit() throws Exception {
//        List<File> l = new ArrayList<File>();
//        for (Params p : model.getParams()) {
//            if (p.getFile() != null) {
//                l.add(new File(p.getFile()));
//            }
//        }
//        if (l.isEmpty()) {
//            throw new ComponentException("No parameter files to edit.");
//        }
//
//        // initial Parameter set generation
//        if (l.size() == 1) {
//            File f = l.get(0);
//            if (!f.exists()) {
//                // create the default parameter and fill it.
//                CSProperties p = DataIO.properties(ComponentAccess.createDefault(model.getComponent()));
//                DataIO.save(p, f, "Parameter");
//            }
//        }
//
//        //
//        nativeLF();
//        PEditor p = new PEditor(l);
//        // the frame
//        Image im = Toolkit.getDefaultToolkit().getImage(
//                getClass().getResource("/ngmf/ui/table.png"));
//        JFrame f = new JFrame();
//        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        f.getContentPane().add(p);
//        f.setIconImage(im);
//        f.setTitle("Parameter " + getName());
//        f.setSize(800, 600);
//        f.setLocation(500, 200);
//        f.setVisible(true);
//        f.toFront();
//        System.out.flush();
//    }
//    
//    public void build() throws Exception {
//        if (build != null) {
//            build.run();
//        } else {
//            System.err.println("  No build file to run.");
//        }
//    }
//
//    public void output() throws Exception {
//        if (Desktop.isDesktopSupported()) {
//            Desktop desktop = Desktop.getDesktop();
//            if (desktop.isSupported(Desktop.Action.OPEN)) {
//                OutputStragegy st = output.getOutputStrategy(getName());
//                File lastFolder = st.lastOutputFolder();
//                if (lastFolder.exists()) {
//                    desktop.open(lastFolder);
//                } else {
//                    log.warning("Folder does not exist (yet): " + lastFolder);
//                }
//            }
//        }
//    }
//}
