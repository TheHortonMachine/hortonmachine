/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.hortonmachine.oms;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import oms3.CLI;

/**
 * Launches an OMS3 script.
 *  
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ScriptLauncherCli {

    public static void main( String[] args ) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String scriptPath = null;
        String workPath = null;
        String mode = "OFF";
        for( int i = 0; i < args.length; i++ ) {
            if (i == 0) {
                if (!new File(args[0]).exists()) {
                    printUsage();
                    return;
                }
                scriptPath = args[0];
            } else {
                if (args[i].startsWith("--work")) {
                    workPath = args[i + 1];
                    i++;
                }
                if (args[i].startsWith("--mode")) {
                    mode = args[i + 1];
                }
            }
        }

        if (workPath != null) {
            System.setProperty("oms3.work", workPath);
        } else {
            String tempdir = System.getProperty("java.io.tmpdir");
            File omsTmp = new File(tempdir + File.separator + "oms");
            if (!omsTmp.exists())
                if (!omsTmp.mkdirs())
                    throw new IOException();
            System.setProperty("oms3.work", omsTmp.getAbsolutePath());
        }

        String script = CLI.readFile(scriptPath);

        Object o = createSim(script, false, mode);
        o.getClass().getMethod("run").invoke(o);
        // the result of dispatching the method represented by this object on obj with parameters
        // args
        // CLI.invoke(o, "run");

    }

    /**
     * Create a simulation.
     * 
     * @param script
     * @param groovy
     * @param loggingMode can be OFF|ALL|SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST.
     * @return
     */
    @SuppressWarnings("nls")
    public static Object createSim( String script, boolean groovy, String loggingMode ) {
        Level.parse(loggingMode);
        StringBuilder sb = new StringBuilder();
        sb.append("import static oms3.SimConst.*\n");
        sb.append("import java.util.*\n");
        sb.append("import oms3.SimBuilder\n");
        // sb.append("import org.jgrasstools.gears.libs.monitor.*\n");
        // sb.append("org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor pm = (org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor) new PrintStreamProgressMonitor(System.out, System.err);\n");
        sb.append("def sb = new SimBuilder(logging:'" + loggingMode + "');\n");
        sb.append(script);
        String finalScript = sb.toString();

        if (!loggingMode.equals("OFF")) {
            System.out.println(finalScript);
        }

        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        GroovyShell shell = new GroovyShell(new GroovyClassLoader(parent), new Binding());
        return shell.evaluate(finalScript);
    }

    private static void printUsage() {
        System.out.println("USAGE: " + "\njava -jar jgrasstools.jar" + "\n pathToScript" + "\n [--work <working_folder_path>]"
                + "\n [--mode <loglevel>");
    }

}
