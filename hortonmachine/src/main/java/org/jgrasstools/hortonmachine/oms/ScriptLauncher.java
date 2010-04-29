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

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map.Entry;

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
        }

        String script = CLI.readFile(scriptPath);

        // add modules imports
        LinkedHashMap<String, Class< ? >> modulename2class = HortonMachine.moduleName2Class;
        Set<Entry<String, Class< ? >>> entries = modulename2class.entrySet();
        for( Entry<String, Class< ? >> entry : entries ) {
            String name = entry.getKey();
            Class< ? > class1 = entry.getValue();
            script = script.replaceAll(name, class1.getCanonicalName());
        }
        modulename2class = JGrassGears.moduleName2Class;
        entries = modulename2class.entrySet();
        for( Entry<String, Class< ? >> entry : entries ) {
            String name = entry.getKey();
            Class< ? > class1 = entry.getValue();
            script = script.replaceAll(name, class1.getCanonicalName());
        }

        System.out.println(script);

        Object o = CLI.createSim(script, false, mode);
        CLI.invoke(o, "run");
    }

    private static void printUsage() {
        System.out.println("USAGE: " + "\njava -jar jgrasstools.jar" + "\n pathToScript"
                + "\n [--work <working_folder_path>]" + "\n [--mode <loglevel>");
    }

}
