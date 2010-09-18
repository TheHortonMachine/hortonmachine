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
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Status;

import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;

@Description("The time related parameters of the epanet inp file")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Epanet")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class EpanetTimeParameters extends JGTModel {

    @Description("The duration of the simulation in minutes. Default is 0.")
    @In
    public Double duration = null;

    @Description("Defines how often a new hydraulic state of the network is computed. In minutes. Default is 60 minutes.")
    @In
    public Double hydraulicTimestep = null;

    @Description("The interval between time periods in all time patterns. Default is 60 minutes.")
    @In
    public Double patternTimestep = null;

    @Description("The time offset at which all patterns will start.")
    @In
    public Double patternStart = null;

    @Description("Sets the timestep interval of the report. Default is 60 minutes.")
    @In
    public Double reportTimestep = null;

    @Description("The time offset at which the report will start.")
    @In
    public Double ReportStart = null;

    @Description("The time of the day at which the simulation begins [0-24].")
    @In
    public Double StartClockTime = null;

    @Description("Kind of postprocessing that should be done on time series.")
    @Label("STATISTIC")
    @In
    public String Statistic = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("Properties file containing the time options.")
    @In
    public String inFile = null;
    
    @Execute
    public void process() throws Exception {
        
    }

}
