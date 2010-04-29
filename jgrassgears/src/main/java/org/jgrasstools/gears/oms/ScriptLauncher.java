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
package org.jgrasstools.gears.oms;

import java.io.File;

import oms3.CLI;

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

        if (args.length > 1) {
            System.setProperty("oms3.work", args[1]);
        }
        if (!new File(args[0]).exists()) {
            printUsage();
            return;
        }

        String f = CLI.readFile(args[0]);
        Object o = CLI.createSim(f, false, "CONFIG");
        CLI.invoke(o, "run");
    }

    private static void printUsage() {
        System.out.println("USAGE: java -jar jgrasstools.jar pathToScript [working folder path]");
    }

}
