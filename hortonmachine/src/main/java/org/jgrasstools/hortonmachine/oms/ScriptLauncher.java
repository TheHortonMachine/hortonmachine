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
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import oms3.CLI;

import org.jgrasstools.gears.JGrassGears;
import org.jgrasstools.hortonmachine.HortonMachine;

/**
 * Launches an OMS3 script.
 *  
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ScriptLauncher {

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

        // add modules imports
        LinkedHashMap<String, Class< ? >> modulename2class = HortonMachine.getInstance().moduleName2Class;
        Set<Entry<String, Class< ? >>> entries = modulename2class.entrySet();
        for( Entry<String, Class< ? >> entry : entries ) {
            // there has to be at least a whitespace before the name
            String name = entry.getKey();
            Class< ? > class1 = entry.getValue();
            script = substituteClass(script, name, class1);
        }
        modulename2class = JGrassGears.getInstance().moduleName2Class;
        entries = modulename2class.entrySet();
        for( Entry<String, Class< ? >> entry : entries ) {
            String name = entry.getKey();
            Class< ? > class1 = entry.getValue();
            script = substituteClass(script, name, class1);
        }

        Object o = createSim(script, false, mode);
//        CLI.invoke(o, "run");
    }

    @SuppressWarnings("nls")
    public static Object createSim( String script, boolean groovy, String loggingMode ) {
        Level.parse(loggingMode);
        StringBuilder sb = new StringBuilder();
        sb.append("import static oms3.SimConst.*\n");
        sb.append("import oms3.SimBuilder\n");
        sb.append(script);
        String finalScript = sb.toString();

        if (!loggingMode.equals("OFF")) {
            System.out.println(finalScript);
        }

        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        GroovyShell shell = new GroovyShell(new GroovyClassLoader(parent), new Binding());
        return shell.evaluate(finalScript);
    }

    @SuppressWarnings("nls")
    private static String substituteClass( String script, String name, Class< ? > class1 ) {
        // names of modules are between apici: 'name'
        // script = script.replaceAll("\\\\'" + name + "\\\\'", "'" + class1.getCanonicalName() +
        // "'");
        script = script.replaceAll("(?<=')" + name + "(?=')", class1.getCanonicalName());

        // script = script.replaceAll("'{1}" + name, "'" + class1.getCanonicalName());
        // script = script.replaceAll(" {1}" + name, " " + class1.getCanonicalName());
        // script = script.replaceAll("\t{1}" + name, "\t" + class1.getCanonicalName());
        // script = script.replaceAll("\n{1}" + name, "\n" + class1.getCanonicalName());
        return script;
    }

    private static void printUsage() {
        System.out.println("USAGE: " + "\njava -jar jgrasstools.jar" + "\n pathToScript" + "\n [--work <working_folder_path>]"
                + "\n [--mode <loglevel>");
    }

}
