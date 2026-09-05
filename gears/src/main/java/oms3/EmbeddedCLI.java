/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3;

import java.util.jar.Manifest;
import oms3.ngmf.util.Jars;

/**
 *
 * @author od
 */
public class EmbeddedCLI {

    public static void main(String[] args) throws Exception {
        Manifest mf = new Manifest(EmbeddedCLI.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
        String simName = mf.getMainAttributes().getValue(Jars.MANIFEST_NAME_OMS_DSL);
        CLI.setOMSProperties(simName, "run");
        if (System.getProperty("oms.prj")==null) {
            System.setProperty("oms.prj", System.getProperty("user.dir"));
        }
        CLI.dsl(EmbeddedCLI.class.getResourceAsStream("/META-INF/" + simName), simName, "INFO", "run");
    }
}
