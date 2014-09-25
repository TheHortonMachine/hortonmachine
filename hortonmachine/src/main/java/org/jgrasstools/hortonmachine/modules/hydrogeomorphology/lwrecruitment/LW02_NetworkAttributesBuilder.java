/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment;

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_LICENSE;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.network.networkattributes.OmsNetworkAttributesBuilder;

@Description("Simple call to the already existing module NetworkAttributesBuilder to create the vector of the network with hierarchical attributes based on an input raster network.")
@Author(name = "Silvia Franceschi, Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords(OMSNETWORKATTRIBUTESBUILDER_KEYWORDS)
@Label("HortonMachine/Hydro-Geomorphology/LWRecruitment")
@Name("LW02_NetworkAttributesBuilder")
@Status(Status.EXPERIMENTAL)
@License(OMSNETWORKATTRIBUTESBUILDER_LICENSE)
public class LW02_NetworkAttributesBuilder extends JGTModel {

    @Description("The extracted network raster map")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inNet = null;

    @Description("The map of flow directions")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFlow = null;

    @Description("The map of Total Contributing Areas")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inTca = null;

    @Description("The vector of the network")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outNet = null;

    @Execute
    public void process() throws Exception {
        OmsNetworkAttributesBuilder networkattributesbuilder = new OmsNetworkAttributesBuilder();
        networkattributesbuilder.inNet = getRaster(inNet);
        networkattributesbuilder.inFlow = getRaster(inFlow);
        networkattributesbuilder.inTca = getRaster(inTca);
        networkattributesbuilder.doHack = false;
        networkattributesbuilder.pm = pm;
        networkattributesbuilder.doProcess = doProcess;
        networkattributesbuilder.doReset = doReset;
        networkattributesbuilder.process();
        dumpRaster(networkattributesbuilder.outHack, null);
        dumpVector(networkattributesbuilder.outNet, outNet);

    }

    public static void main( String[] args ) throws Exception {

        String inNet = "D:/lavori_tmp/gsoc/raster/net_null.asc";
        String inFlow = "D:/lavori_tmp/gsoc/raster/flow.asc";
        String inTca = "D:/lavori_tmp/gsoc/raster/tca.asc";
        String outNet = "D:/lavori_tmp/gsoc/net_attribute.shp";
        LW02_NetworkAttributesBuilder networkAttributesBuilder = new LW02_NetworkAttributesBuilder();
        networkAttributesBuilder.inNet = inNet;
        networkAttributesBuilder.inFlow = inFlow;
        networkAttributesBuilder.inTca = inTca;
        networkAttributesBuilder.outNet = outNet;

        networkAttributesBuilder.process();


    }

}