package org.hortonmachine.jython_tmp;
//package org.hortonmachine.jython;
//
//import java.awt.Dimension;
//import java.io.File;
//import java.util.Date;
//
//import javax.swing.JFrame;
//
//import org.hortonmachine.gears.io.geopaparazzi.geopap4.TimeUtilities;
//import org.hortonmachine.gears.libs.modules.HMConstants;
//import org.hortonmachine.gui.console.LogConsoleController;
//import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
//import org.hortonmachine.gui.utils.GuiBridgeHandler;
//import org.joda.time.DateTime;
//import org.python.core.PyFunction;
//import org.python.core.PyInteger;
//import org.python.util.PythonInterpreter;
//
//public class JythonRunner {
//
//    private GuiBridgeHandler guiBridge;
//
//    public JythonRunner( GuiBridgeHandler guiBridge ) {
//        this.guiBridge = guiBridge;
//    }
//
//    public void run( String script ) {
//        LogConsoleController logConsole = new LogConsoleController(null);
//
//        guiBridge.showWindow(logConsole.asJComponent(), "Console Log");
//
//        new Thread(() -> {
//            try (PythonInterpreter pythonInterpreter = new PythonInterpreter()) {
//                pythonInterpreter.setOut(logConsole.getLogAreaPrintStream());
//                pythonInterpreter.setErr(logConsole.getLogAreaPrintStream());
//
//                File scriptFile = new File(script);
//                String name = "";
//                if (scriptFile.exists()) {
//                    name = scriptFile.getName();
//                }
//                logConsole.beginProcess(name);
//                if (scriptFile.exists()) {
//                    pythonInterpreter.execfile(script);
//                } else {
//                    pythonInterpreter.exec(script);
//                }
//                // in case of gvsig scripts, a main method is inserted
//                PyFunction pf = (PyFunction) pythonInterpreter.get("main");
//                if (pf != null)
//                    pf.__call__();
//            } finally {
//                logConsole.finishProcess();
//                logConsole.stopLogging();
//            }
//        }).start();
//
//    }
//
//    public static void main( String[] args ) {
//        String script = "print 'Hello world'";
//
//        JythonRunner jythonRunner = new JythonRunner(new DefaultGuiBridgeImpl());
//        jythonRunner.run(script);
//    }
//
//}
