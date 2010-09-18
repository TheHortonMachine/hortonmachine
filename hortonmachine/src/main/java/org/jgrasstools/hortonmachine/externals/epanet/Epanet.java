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
package org.jgrasstools.hortonmachine.externals.epanet;

import java.io.File;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Status;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.hortonmachine.externals.epanet.core.Components;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetException;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetNativeFunctions;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetWrapper;
import org.jgrasstools.hortonmachine.externals.epanet.core.LinkTypes;
import org.jgrasstools.hortonmachine.externals.epanet.core.LinkParameters;
import org.jgrasstools.hortonmachine.externals.epanet.core.NodeParameters;
import org.jgrasstools.hortonmachine.externals.epanet.core.NodeTypes;
import org.jgrasstools.hortonmachine.externals.epanet.core.TimeParameterCodes;
import org.jgrasstools.hortonmachine.externals.epanet.core.TimeParameterCodesStatistic;

@Description("The main Epanet module")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Epanet")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class Epanet extends JGTModel {

    @Description("The inp file.")
    @In
    public String inInp = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Execute
    public void process() throws Exception {
        EpanetWrapper ep = new EpanetWrapper("epanet2_64bit",
                "D:\\development\\jgrasstools-hg\\jgrasstools\\hortonmachine\\src\\main\\resources\\");

        File inFile = new File(inInp);
        String report = inFile.getAbsolutePath() + ".rpt";
        ep.ENopen(inInp, report, "");

        // reportLinks(ep);
        // reportNodes(ep);

        if (false) {
            ep.ENsolveH();
            ep.ENsaveH();
            // ep.ENreport();
        } else {
            long[] t = new long[1];
            long[] tstep = new long[1];
            ep.ENopenH();
            ep.ENinitH(0);
            do {
                ep.ENrunH(t);

                reportLinks(ep);
                reportNodes(ep);

                ep.ENnextH(tstep);
                System.out.println("TIME: " + t[0]);
                System.out.println("TIMESTEP: " + tstep[0]);

            } while( tstep[0] > 0 );

            ep.ENcloseH();
        }

        ep.ENclose();

    }
    private void reportLinks( EpanetWrapper ep ) throws EpanetException {
        int linksNum = ep.ENgetcount(Components.EN_LINKCOUNT);
        System.out.println("Links found: " + linksNum);
        for( int i = 1; i <= linksNum; i++ ) {
            String linkId = ep.ENgetlinkid(i);
            LinkTypes type = ep.ENgetlinktype(i);
            float flow = ep.ENgetlinkvalue(i, LinkParameters.EN_FLOW);
            float vel = ep.ENgetlinkvalue(i, LinkParameters.EN_VELOCITY);
            float headloss = ep.ENgetlinkvalue(i, LinkParameters.EN_HEADLOSS);
            float status = ep.ENgetlinkvalue(i, LinkParameters.EN_STATUS);
            System.out.println(linkId + " - " + type.getDescription() + " - " + flow + " - " + vel + " - " + status + " - "
                    + headloss);
        }
    }

    private void reportNodes( EpanetWrapper ep ) throws EpanetException {
        int nodesNum = ep.ENgetcount(Components.EN_NODECOUNT);
        System.out.println("Nodes found: " + nodesNum);
        for( int i = 1; i <= nodesNum; i++ ) {
            String nodeId = ep.ENgetnodeid(i);
            NodeTypes type = ep.ENgetnodetype(i);
            float dem = ep.ENgetnodevalue(i, NodeParameters.EN_DEMAND);
            float head = ep.ENgetnodevalue(i, NodeParameters.EN_HEAD);
            float press = ep.ENgetnodevalue(i, NodeParameters.EN_PRESSURE);
            System.out.println(nodeId + " - " + type.getDescription() + " - " + dem + " - " + head + " - " + press);
        }
    }

}
