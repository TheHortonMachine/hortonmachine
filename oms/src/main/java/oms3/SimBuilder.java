/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3;

import oms3.dsl.Buildable;
import oms3.dsl.GenericBuilderSupport;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * SimBuilder class for all oms simulation DSLs
 *
 * @author od
 */
public class SimBuilder extends GenericBuilderSupport {

    public static void checkInstall() {
        String jv = System.getProperty("java.version");
        String gv = InvokerHelper.getVersion();
        String ov = oms3.Utils.getVersion();

        System.out.print("  Java:" + jv + " Groovy:" + gv + " OMS:" + ov);
        if (jv.compareTo("1.6") > 0 && gv.compareTo("1.5.6") >= 0 && ov.compareTo("3.0") >= 0) {
            System.out.println("  ...Correct Installation.");
        } else {
            System.out.println("  ...Incorrect Installation!, check versions.");
        }
    }

    @Override
    public  Class<? extends Buildable> lookupTopLevel(Object name) {
        String cl = null;
        if (name.toString().equals("sim")) {
            cl = "oms3.dsl.Sim";
        } else if (name.toString().equals("esp")) {
            cl = "oms3.dsl.esp.Esp";
        } else if (name.toString().equals("luca")) {
            cl = "oms3.dsl.cosu.Luca";
        } else if (name.toString().equals("fast")) {
            cl = "oms3.dsl.cosu.Fast";
        } else if (name.toString().equals("dds")) {
            cl = "oms3.dsl.cosu.DDS";
//        } else if (name.toString().equals("cluster")) {
//            cl = "oms3.dsl.cluster.Cluster";
        } else if (name.toString().equals("glue")) {
            cl = "oms3.dsl.cosu.Glue";
        } else if (name.toString().equals("test")) {
            cl = "oms3.dsl.Test";
        } else if (name.toString().equals("chart")) {
            cl = "oms3.dsl.analysis.Chart";
        } else {
            throw new IllegalArgumentException(name.toString());
        }
        try {
            return (Class<? extends Buildable>) Class.forName(cl);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
