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
package org.jgrasstools.hortonmachine.modules.networktools.epanet;

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_inDll_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_inInp_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_junctionsList_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_pipesList_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_pumpsList_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_reservoirsList_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_tCurrent_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_tStart_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_tanksList_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_valvesList_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEPANET_warnings_DESCRIPTION;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.Components;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.EpanetException;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.EpanetWrapper;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.LinkParameters;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.LinkTypes;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.NodeParameters;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.NodeTypes;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.types.Junction;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.types.Pipe;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.types.Pump;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.types.Reservoir;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.types.Tank;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.types.Valve;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

@Description(OMSEPANET_DESCRIPTION)
@Author(name = OMSEPANET_AUTHORNAMES, contact = OMSEPANET_AUTHORCONTACTS)
@Keywords(OMSEPANET_KEYWORDS)
@Label(OMSEPANET_LABEL)
@Name(OMSEPANET_NAME)
@Status(OMSEPANET_STATUS)
@License(OMSEPANET_LICENSE)
public class OmsEpanet extends JGTModel {

    @Description(OMSEPANET_inDll_DESCRIPTION)
    @In
    public String inDll = null;

    @Description(OMSEPANET_inInp_DESCRIPTION)
    @In
    public String inInp = null;

    @Description(OMSEPANET_tStart_DESCRIPTION)
    @In
    public String tStart = "1970-01-01 00:00:00"; //$NON-NLS-1$

    @Description(OMSEPANET_tCurrent_DESCRIPTION)
    @Out
    public String tCurrent = null;

    @Description(OMSEPANET_pipesList_DESCRIPTION)
    @Out
    public List<Pipe> pipesList = null;

    @Description(OMSEPANET_junctionsList_DESCRIPTION)
    @Out
    public List<Junction> junctionsList = null;

    @Description(OMSEPANET_pumpsList_DESCRIPTION)
    @Out
    public List<Pump> pumpsList = null;

    @Description(OMSEPANET_valvesList_DESCRIPTION)
    @Out
    public List<Valve> valvesList = null;

    @Description(OMSEPANET_tanksList_DESCRIPTION)
    @Out
    public List<Tank> tanksList = null;

    @Description(OMSEPANET_reservoirsList_DESCRIPTION)
    @Out
    public List<Reservoir> reservoirsList = null;

    @Description(OMSEPANET_warnings_DESCRIPTION)
    @Out
    public String warnings = null;

    private EpanetWrapper ep;

    private long[] t = new long[1];
    private long[] tstep = new long[1];
    public static DateTimeFormatter formatter = JGTConstants.utcDateFormatterYYYYMMDDHHMMSS;

    private DateTime current = null;

    @Initialize
    public void initProcess() {
        // activate time
        doProcess = true;
    }

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        StringBuilder sb = new StringBuilder("");
        if (ep == null) {
            if (inDll == null) {
                // I am feeling lucky
                ep = new EpanetWrapper("epanet2", null);
            } else {
                File dllFile = new File(inDll);
                String nameWithoutExtention = FileUtilities.getNameWithoutExtention(dllFile);
                String path = dllFile.getParentFile().getAbsolutePath();
                ep = new EpanetWrapper(nameWithoutExtention, path);
            }

            // int version = ep.ENgetversion();

            current = formatter.parseDateTime(tStart);
            tCurrent = tStart;

            ep.ENopen(inInp, inInp + ".rpt", "");
            String w = ep.getWarningMessage();
            if (w != null)
                sb.append(w).append("\n");
            ep.ENopenH();
            w = ep.getWarningMessage();
            if (w != null)
                sb.append(w).append("\n");
            ep.ENinitH(0);
            w = ep.getWarningMessage();
            if (w != null)
                sb.append(w).append("\n");
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
        String w = ep.getWarningMessage();
        if (w != null)
            sb.append(w).append("\n");

        extractLinksData(ep);
        extractNodesData(ep);

        ep.ENnextH(tstep);
        w = ep.getWarningMessage();
        if (w != null)
            sb.append(w).append("\n");

        if (tstep[0] <= 0) {
            doProcess = false;
        }

        String warningsBuffer = sb.toString();
        if (warningsBuffer.length() > 0) {
            warnings = warningsBuffer;
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
                v.time = current;
                v.flow = ep.ENgetlinkvalue(i, LinkParameters.EN_FLOW)[0];
                v.velocity = ep.ENgetlinkvalue(i, LinkParameters.EN_VELOCITY)[0];
                v.headloss = ep.ENgetlinkvalue(i, LinkParameters.EN_HEADLOSS)[0];
                v.status = ep.ENgetlinkvalue(i, LinkParameters.EN_STATUS)[0];
                valvesList.add(v);
                break;
            }
            case EN_CVPIPE:
            case EN_PIPE:
                Pipe p = new Pipe();
                p.id = ep.ENgetlinkid(i);
                p.time = current;
                p.flow = ep.ENgetlinkvalue(i, LinkParameters.EN_FLOW);
                p.velocity = ep.ENgetlinkvalue(i, LinkParameters.EN_VELOCITY);
                p.headloss = ep.ENgetlinkvalue(i, LinkParameters.EN_HEADLOSS)[0];
                p.status = ep.ENgetlinkvalue(i, LinkParameters.EN_STATUS)[0];
                pipesList.add(p);
                break;
            case EN_PUMP:
                Pump pu = new Pump();
                pu.id = ep.ENgetlinkid(i);
                pu.time = current;
                pu.flow = ep.ENgetlinkvalue(i, LinkParameters.EN_FLOW)[0];
                pu.velocity = ep.ENgetlinkvalue(i, LinkParameters.EN_VELOCITY)[0];
                pu.headloss = ep.ENgetlinkvalue(i, LinkParameters.EN_HEADLOSS)[0];
                pu.status = ep.ENgetlinkvalue(i, LinkParameters.EN_STATUS)[0];
                pu.energy = ep.ENgetlinkvalue(i, LinkParameters.EN_ENERGY)[0];
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
                j.time = current;
                j.demand = ep.ENgetnodevalue(i, NodeParameters.EN_DEMAND);
                j.head = ep.ENgetnodevalue(i, NodeParameters.EN_HEAD);
                j.pressure = ep.ENgetnodevalue(i, NodeParameters.EN_PRESSURE);
                junctionsList.add(j);
                break;
            case EN_RESERVOIR:
                Reservoir r = new Reservoir();
                r.id = ep.ENgetnodeid(i);
                r.time = current;
                r.demand = ep.ENgetnodevalue(i, NodeParameters.EN_DEMAND);
                r.head = ep.ENgetnodevalue(i, NodeParameters.EN_HEAD);
                reservoirsList.add(r);
                break;
            case EN_TANK:
                Tank t = new Tank();
                t.id = ep.ENgetnodeid(i);
                t.time = current;
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
