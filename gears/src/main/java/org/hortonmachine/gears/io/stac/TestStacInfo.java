package org.hortonmachine.gears.io.stac;

import java.util.List;

import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;

public class TestStacInfo extends TestVariables {

    public TestStacInfo() throws Exception {

        try (HMStacManager stacManager = new HMStacManager(repoUrl, new LogProgressMonitor())) {
            stacManager.open();
            
            System.out.println("Supported conformance:");
            System.out.println("======================");
            System.out.println(stacManager.getConformanceSummary());

            System.out.println();
            System.out.println("Available Collections:");
            System.out.println("======================");
            List<HMStacCollection> collections = stacManager.getCollections();
            for( HMStacCollection c : collections ) {
                System.out.println(c.toString());
                System.out.println("----------------------------------------------");
            }
        }

    }

    public static void main( String[] args ) throws Exception {
        new TestStacInfo();
    }

}
