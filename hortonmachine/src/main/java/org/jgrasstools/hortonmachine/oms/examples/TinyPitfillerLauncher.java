/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jgrasstools.hortonmachine.oms.examples;

import oms3.CLI;

/**
 *
 * @author od
 */
public class TinyPitfillerLauncher {

    // this is for debugging only. set breakpoints in the components
    // and start this file in debug mode.

    public static void main( String[] args ) throws Exception {
        System.setProperty("oms3.work", "/home/moovida/development/jgrasstools-hg/jgrasstools/hortonmachine/");
//        System.setProperty("oms3.work", "/Users/silli/workspace/hortonmachine/hortonmachine/");
//        System.setProperty("oms3.work", "D:/development/hortonmachine-svn/trunk/hortonmachine");
//        System.setProperty("oms3.work", "/od/software/hm/trunk/hortonmachine");
        String f = CLI.readFile(System.getProperty("oms3.work") +"/src/main/java/eu/hydrologis/jgrass/hortonmachine/oms/TinyPitfiller_1.sim");
        Object o = CLI.createSim(f, false, "CONFIG");
        CLI.invoke(o, "run");
    }

}
