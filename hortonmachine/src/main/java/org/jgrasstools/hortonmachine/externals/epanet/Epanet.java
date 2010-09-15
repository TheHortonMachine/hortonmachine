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
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetNativeFunctions;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetWrapper;
import org.jgrasstools.hortonmachine.externals.epanet.core.LinkTypes;
import org.jgrasstools.hortonmachine.externals.epanet.core.Parameters;
import org.jgrasstools.hortonmachine.externals.epanet.core.TimeParameterCodes;

@Description("The main Epanet module")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Epanet")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class Epanet extends JGTModel {

    @Description("The junctions features.")
    @In
    public SimpleFeatureCollection inJunctions = null;

    @Description("The junctions features.")
    @In
    public SimpleFeatureCollection inTanks = null;

    @Description("The tanks features.")
    @In
    public SimpleFeatureCollection inReservoirs = null;

    @Description("The pumps features.")
    @In
    public SimpleFeatureCollection inPumps = null;

    @Description("The valves features.")
    @In
    public SimpleFeatureCollection inValves = null;

    @Description("The pipes features.")
    @In
    public SimpleFeatureCollection inPipes = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The file into which to write the inp.")
    @In
    public String outFile = null;

    private static final String NL = "\n";
    private static final String SPACER = " ";

    private static String folder = "D:\\development\\hydrologis-hg\\hydrologis\\epanet\\eu.hydrologis.jgrass.epanet\\example\\";

    @Execute
    public void process() throws Exception {

        // System.out.println("Dll Path " + System.getProperty("java.library.path"));
        String input = folder + "semplice.inp";
        String report = folder + "semplice.rpt";
        String report2 = folder + "semplice2.rpt";
        String output = folder + "semplice.out";

        EpanetWrapper ep = new EpanetWrapper();

        ep.ENopen(input, report, "");

        int linksNum = ep.ENgetcount(Components.EN_LINKCOUNT);
        System.out.println("Links found: " + linksNum);
        for( int i = 1; i <= linksNum; i++ ) {
            String linkId = ep.ENgetlinkid(i);
            LinkTypes type = ep.ENgetlinktype(i);
            float value = ep.ENgetlinkvalue(i, Parameters.EN_DIAMETER);
            System.out.println(linkId + " - " + type.getDescription() + " - " + value);
        }

        /* Compute ranges (max - min) */
        ep.ENsettimeparam(TimeParameterCodes.EN_STATISTIC, (long) EpanetNativeFunctions.EN_RANGE);

        /* Solve hydraulics */
        ep.ENsolveH();
        ep.ENsaveH();

        /* Define contents of the report */
        ep.ENresetreport();
        ep.ENsetreport("FILE " + report2);
        ep.ENsetreport("NODES ALL");
        ep.ENsetreport("PRESSURE PRECISION 1");
        ep.ENsetreport("PRESSURE ABOVE 20");

        /* Write the report to file */
        ep.ENreport();

        ep.ENclose();

    }

}
