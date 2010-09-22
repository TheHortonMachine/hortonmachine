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
import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.FileUtilities;
import org.jgrasstools.hortonmachine.externals.epanet.core.Components;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetException;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetWrapper;
import org.jgrasstools.hortonmachine.externals.epanet.core.LinkParameters;
import org.jgrasstools.hortonmachine.externals.epanet.core.LinkTypes;
import org.jgrasstools.hortonmachine.externals.epanet.core.NodeParameters;
import org.jgrasstools.hortonmachine.externals.epanet.core.NodeTypes;
import org.jgrasstools.hortonmachine.externals.epanet.core.types.Junction;
import org.jgrasstools.hortonmachine.externals.epanet.core.types.Pipe;
import org.jgrasstools.hortonmachine.externals.epanet.core.types.Pump;
import org.jgrasstools.hortonmachine.externals.epanet.core.types.Reservoir;
import org.jgrasstools.hortonmachine.externals.epanet.core.types.Tank;
import org.jgrasstools.hortonmachine.externals.epanet.core.types.Valve;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

@Description("The main Epanet module")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "www.hydrologis.com")
@Keywords("Epanet")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class Epanet extends JGTModel {

    @Description("The epanet dynamic lib file.")
    @In
    public String inDll = null;

    @Description("The inp file.")
    @In
    public String inInp = null;

    @Description("The start time.")
    @In
    public String tStart = "1970-01-01 00:00"; //$NON-NLS-1$

    @Description("The current time.")
    @Out
    public String tCurrent = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The pipes result data.")
    @Out
    public List<Pipe> pipesList = null;

    @Description("The junctions result data.")
    @Out
    public List<Junction> junctionsList = null;

    @Description("The pumps result data.")
    @Out
    public List<Pump> pumpsList = null;

    @Description("The valves result data.")
    @Out
    public List<Valve> valvesList = null;

    @Description("The tanks result data.")
    @Out
    public List<Tank> tanksList = null;

    @Description("The reservoirs result data.")
    @Out
    public List<Reservoir> reservoirsList = null;

    private EpanetWrapper ep;

    private long[] t = new long[1];
    private long[] tstep = new long[1];
    public DateTimeFormatter formatter = JGTConstants.utcDateFormatterYYYYMMDDHHMM;

    private DateTime current = null;

    @Initialize
    public void initProcess() {
        // activate time
        doProcess = true;
    }

    @Execute
    public void process() throws Exception {
        if (ep == null) {
            if (inDll == null) {
                // I am feeling lucky
                ep = new EpanetWrapper("epanet2", null);
            } else {
                File dllFile = new File(inDll);
                String nameWithoutExtention = FileUtilities.getNameWithoutExtention(dllFile);
                ep = new EpanetWrapper(nameWithoutExtention, dllFile.getParentFile().getAbsolutePath());
            }

            current = formatter.parseDateTime(tStart);
            tCurrent = tStart;

            ep.ENopen(inInp, inInp + ".rpt", "");
            ep.ENopenH();
            ep.ENinitH(0);
        } else {
            current = current.plusSeconds((int) tstep[0]);
            tCurrent = current.toString(formatter);
        }

        pipesList = new ArrayList<Pipe>();
        junctionsList = new ArrayList<Junction>();
        pumpsList = new ArrayList<Pump>();
        valvesList = new ArrayList<Valve>();
        tanksList = new ArrayList<Tank>();
        reservoirsList = new ArrayList<Reservoir>();

        ep.ENrunH(t);

        extractLinksData(ep);
        extractNodesData(ep);

        ep.ENnextH(tstep);

        if (tstep[0] <= 0) {
            doProcess = false;
        }
    }
    public void finish() throws EpanetException {
        ep.ENcloseH();
        ep.ENclose();
    }

    private void extractLinksData( EpanetWrapper ep ) throws EpanetException {
        int linksNum = ep.ENgetcount(Components.EN_LINKCOUNT);
        for( int i = 1; i <= linksNum; i++ ) {
            LinkTypes type = ep.ENgetlinktype(i);

            switch( type ) {
            case EN_GPV:
            case EN_PRV:
            case EN_PSV:
            case EN_PBV:
            case EN_FCV:
            case EN_TCV: {
                Valve v = new Valve();
                v.id = ep.ENgetlinkid(i);
                v.flow = ep.ENgetlinkvalue(i, LinkParameters.EN_FLOW);
                v.velocity = ep.ENgetlinkvalue(i, LinkParameters.EN_VELOCITY);
                v.headloss = ep.ENgetlinkvalue(i, LinkParameters.EN_HEADLOSS);
                v.status = ep.ENgetlinkvalue(i, LinkParameters.EN_STATUS);
                valvesList.add(v);
                break;
            }
            case EN_CVPIPE:
            case EN_PIPE:
                Pipe p = new Pipe();
                p.id = ep.ENgetlinkid(i);
                p.flow = ep.ENgetlinkvalue(i, LinkParameters.EN_FLOW);
                p.velocity = ep.ENgetlinkvalue(i, LinkParameters.EN_VELOCITY);
                p.headloss = ep.ENgetlinkvalue(i, LinkParameters.EN_HEADLOSS);
                p.status = ep.ENgetlinkvalue(i, LinkParameters.EN_STATUS);
                pipesList.add(p);
                break;
            case EN_PUMP:
                Pump pu = new Pump();
                pu.id = ep.ENgetlinkid(i);
                pu.flow = ep.ENgetlinkvalue(i, LinkParameters.EN_FLOW);
                pu.velocity = ep.ENgetlinkvalue(i, LinkParameters.EN_VELOCITY);
                pu.headloss = ep.ENgetlinkvalue(i, LinkParameters.EN_HEADLOSS);
                pu.status = ep.ENgetlinkvalue(i, LinkParameters.EN_STATUS);
                pu.energy = ep.ENgetlinkvalue(i, LinkParameters.EN_ENERGY);
                pumpsList.add(pu);
                break;
            default:
                break;
            }
        }
    }

    private void extractNodesData( EpanetWrapper ep ) throws EpanetException {
        int nodesNum = ep.ENgetcount(Components.EN_NODECOUNT);
        for( int i = 1; i <= nodesNum; i++ ) {
            NodeTypes type = ep.ENgetnodetype(i);

            switch( type ) {
            case EN_JUNCTION:
                Junction j = new Junction();
                j.id = ep.ENgetnodeid(i);
                j.demand = ep.ENgetnodevalue(i, NodeParameters.EN_DEMAND);
                j.head = ep.ENgetnodevalue(i, NodeParameters.EN_HEAD);
                j.pressure = ep.ENgetnodevalue(i, NodeParameters.EN_PRESSURE);
                junctionsList.add(j);
                break;
            case EN_RESERVOIR:
                Reservoir r = new Reservoir();
                r.id = ep.ENgetnodeid(i);
                r.demand = ep.ENgetnodevalue(i, NodeParameters.EN_DEMAND);
                r.head = ep.ENgetnodevalue(i, NodeParameters.EN_HEAD);
                reservoirsList.add(r);
                break;
            case EN_TANK:
                Tank t = new Tank();
                t.id = ep.ENgetnodeid(i);
                t.demand = ep.ENgetnodevalue(i, NodeParameters.EN_DEMAND);
                t.head = ep.ENgetnodevalue(i, NodeParameters.EN_HEAD);
                t.pressure = ep.ENgetnodevalue(i, NodeParameters.EN_PRESSURE);
                tanksList.add(t);
                break;

            default:
                break;
            }
        }
    }

}
