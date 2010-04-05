/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.hydrologis.jgrass.hortonmachine.oms;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import oms3.Compound;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.Role;
import eu.hydrologis.jgrass.hortonmachine.io.arcgrid.ArcgridCoverageReader;
import eu.hydrologis.jgrass.hortonmachine.io.arcgrid.ArcgridCoverageWriter;
import eu.hydrologis.jgrass.hortonmachine.libs.models.HMConstants;
import eu.hydrologis.jgrass.hortonmachine.libs.monitor.PrintStreamProgressMonitor;
import eu.hydrologis.jgrass.hortonmachine.modules.demmanipulation.pitfiller.Pitfiller;

/**
 *
 * @author Olaf David
 */
public class TinyPitfiller extends Compound {

    // Model parameter
    @Role(Role.PARAMETER)
    @In public String inFilePath;

    @Role(Role.PARAMETER)
    @In public String outFilePath;

    // components
    private ArcgridCoverageReader reader = new ArcgridCoverageReader();
    private ArcgridCoverageWriter writer = new ArcgridCoverageWriter();
    private Pitfiller pitfiller = new Pitfiller();

    @Initialize
    public void init() {
        // for now
        val2in(-9999.0, reader, "fileNovalue");
        val2in(HMConstants.doubleNovalue, reader, "novalue");
        val2in(new PrintStreamProgressMonitor(System.out, System.out), pitfiller, "pm");
        /*
         * read coverage from the reader links to the input
         * coverage needed by pitfiller. 
         */
        out2in(reader, "coverage", pitfiller, "elevationCoverage");
        /*
         * processed coverage by pitfiller links to the 
         * coverage writer.
         */
        out2in(pitfiller, "pitfillerCoverage", writer, "coverage");

        /*
         * map a variable named inFilePath to the input 
         * of the reader.
         */
        in2in("inFilePath", reader, "arcgridCoveragePath");

        /*
         * map a variable named outFilePath to the input 
         * of the writer.
         */
        in2in("outFilePath", writer, "arcgridCoveragePath");

        initializeComponents();
    }

  
}
