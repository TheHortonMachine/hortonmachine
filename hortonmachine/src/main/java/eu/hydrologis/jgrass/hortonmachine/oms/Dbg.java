/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.hydrologis.jgrass.hortonmachine.oms;

import oms3.CLI;

/**
 *
 * @author od
 */
public class Dbg {

    public static void main( String[] args ) throws Exception {
//        System.setProperty("oms3.work", "D:/development/hortonmachine-svn/trunk/hortonmachine/src/test/resources");
//        System.setProperty("oms3.work", "D:/development/hortonmachine-svn/trunk/hortonmachine");
        System.setProperty("oms3.work", "/od/software/hm/trunk/hortonmachine");
        String f = CLI.readFile(System.getProperty("oms3.work") +"/src/main/java/eu/hydrologis/jgrass/hortonmachine/oms/TinyPitfiller.sim");
        Object o = CLI.createSim(f, false, "OFF");
        CLI.invoke(o, "run");
    }

}
