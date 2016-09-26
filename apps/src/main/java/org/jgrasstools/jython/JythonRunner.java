package org.jgrasstools.jython;

import java.awt.Dimension;
import java.io.File;
import java.util.Date;

import javax.swing.JFrame;

import org.jgrasstools.gears.io.geopaparazzi.geopap4.TimeUtilities;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gui.console.LogConsoleController;
import org.joda.time.DateTime;
import org.python.core.PyFunction;
import org.python.core.PyInteger;
import org.python.util.PythonInterpreter;

public class JythonRunner {

    public void run( String script ) {
        LogConsoleController console = new LogConsoleController(null);

        JFrame frame = new JFrame("Jython Runner Console");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(console);
        // frame.setResizable(false);
        frame.setPreferredSize(new Dimension(600, 400));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        try (PythonInterpreter pythonInterpreter = new PythonInterpreter()) {
            pythonInterpreter.setOut(console.getLogAreaPrintStream());
            pythonInterpreter.setErr(console.getLogAreaPrintStream());

            File scriptFile = new File(script);
            String name = "";
            if (scriptFile.exists()) {
                name = scriptFile.getName();
            }
            console.beginProcess(name);
            if (scriptFile.exists()) {
                pythonInterpreter.execfile(script);
            } else {
                pythonInterpreter.exec(script);
            }
            PyFunction pf = (PyFunction) pythonInterpreter.get("main");
            if (pf != null)
                pf.__call__();
        } finally {
            console.finishProcess();
        }
    }

    public static void main( String[] args ) {
        String script = "/home/hydrologis/development/jython-gvsig-course.git/jython_essentials/basic types.py";

        JythonRunner jythonRunner = new JythonRunner();
        jythonRunner.run(script);
    }

}
