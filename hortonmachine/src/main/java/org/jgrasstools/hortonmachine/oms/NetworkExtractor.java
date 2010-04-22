/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jgrasstools.hortonmachine.oms;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import oms3.Compound;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.Role;

import org.jgrasstools.gears.io.arcgrid.ArcgridCoverageReader;
import org.jgrasstools.gears.io.arcgrid.ArcgridCoverageWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.demmanipulation.pitfiller.Pitfiller;
import org.jgrasstools.hortonmachine.modules.geomorphology.draindir.DrainDir;
import org.jgrasstools.hortonmachine.modules.geomorphology.flow.FlowDirections;
import org.jgrasstools.hortonmachine.modules.network.extractnetwork.ExtractNetwork;

/**
 *
 * @author Olaf David
 */
public class NetworkExtractor extends Compound {

    // Model parameter
    @Role(Role.PARAMETER)
    @In
    public String inFilePath;

    @Role(Role.PARAMETER)
    @In
    public String networkFilePath;

    @Role(Role.PARAMETER)
    @In
    public double lambda;

    @Role(Role.PARAMETER)
    @In
    public double tcaThreshold;

    // components
    private ArcgridCoverageReader reader = new ArcgridCoverageReader();
    private ArcgridCoverageWriter writer = new ArcgridCoverageWriter();
    private Pitfiller pitfiller = new Pitfiller();
    private FlowDirections flow = new FlowDirections();
    private DrainDir drain = new DrainDir();
    private ExtractNetwork network = new ExtractNetwork();

    @Initialize
    public void init() {

        // for now
        val2in(-9999.0, reader, "fileNovalue");
        val2in(JGTConstants.doubleNovalue, reader, "novalue");

        val2in(new PrintStreamProgressMonitor(System.out, System.out), pitfiller, "pm");

        val2in(new PrintStreamProgressMonitor(System.out, System.out), flow, "pm");

        val2in(new PrintStreamProgressMonitor(System.out, System.out), drain, "pm");
        val2in(1, drain, "mode");
        val2in(false, drain, "fixedMode");
        val2in(CoverageUtilities.buildDummyCoverage(), drain, "flowFixedCoverage");

        val2in(new PrintStreamProgressMonitor(System.out, System.out), network, "pm");
        val2in(0, network, "mode");
        val2in(false, network, "doFeatureCollection");
        val2in(CoverageUtilities.buildDummyCoverage(), network, "slopeCoverage");
        val2in(CoverageUtilities.buildDummyCoverage(), network, "classCoverage");

        // reader to pit
        out2in(reader, "coverage", pitfiller, "elevationCoverage");
        // pit to flow
        out2in(pitfiller, "pitfillerCoverage", flow);
        // pit to drain
        out2in(pitfiller, "pitfillerCoverage", drain);
        // flow to drain
        out2in(flow, "flowDirectionsCoverage", drain, "flowCoverage");
        // drain map to network
        out2in(drain, "draindirCoverage", network, "flowCoverage");
        // tca map to network
        out2in(drain, "tcaCoverage", network, "tcaCoverage");
        // network to writer
        out2in(network, "networkCoverage", writer, "coverage");

        in2in("inFilePath", reader, "arcgridCoveragePath");

        in2in("lambda", drain);
        in2in("tcaThreshold", network, "threshold");

        in2in("networkFilePath", writer, "arcgridCoveragePath");

        initializeComponents();
    }

    public static void main( String[] args ) throws Exception {
        // File f = new File(System.getProperty("oms3.work") + "/data", "LeafCatch.dat");
        //
        // Model hymod = new Model();
        // hymod.climateFile = f.toString();
        // hymod.alpha = 0.85;
        // hymod.bexp = 1.5;
        // hymod.cmax = 100;
        // hymod.kq = 0.75;
        // hymod.nq = 3;
        // hymod.ks = 0.01;
        // hymod.ns = 1;
        //
        // ComponentAccess.callAnnotated(hymod, Initialize.class, true);
        // ComponentAccess.callAnnotated(hymod, Execute.class, false);
        // ComponentAccess.callAnnotated(hymod, Finalize.class, true);
        //
        // System.out.println("done.");
        // System.exit(0); // ?????
    }
}
