/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.hmachine.modules.hillslopeanalyses.h2ca;

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_inAttribute_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_inNet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSH2CA_outAttribute_DESCRIPTION;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.FlowNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.utils.RegionMap;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

@Description(OMSH2CA_DESCRIPTION)
@Author(name = OMSH2CA_AUTHORNAMES, contact = OMSH2CA_AUTHORCONTACTS)
@Keywords(OMSH2CA_KEYWORDS)
@Label(OMSH2CA_LABEL)
@Name(OMSH2CA_NAME)
@Status(OMSH2CA_STATUS)
@License(OMSH2CA_LICENSE)
public class OmsH2cA extends HMModel {
    @Description(OMSH2CA_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSH2CA_inNet_DESCRIPTION)
    @In
    public GridCoverage2D inNet = null;

    @Description(OMSH2CA_inAttribute_DESCRIPTION)
    @In
    public GridCoverage2D inAttribute = null;

    @Description(OMSH2CA_outAttribute_DESCRIPTION)
    @Out
    public GridCoverage2D outAttribute = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outAttribute == null, doReset)) {
            return;
        }
        checkNull(inFlow, inNet, inAttribute);


        
        HMRaster flowRasterW = null;
        HMRaster netRaster = null;
        HMRaster attributeRaster = null;
        HMRaster h2caRaster = null;
        try {
			flowRasterW = HMRaster.fromGridCoverageWritable(inFlow);
			netRaster = HMRaster.fromGridCoverage(inNet);
			attributeRaster = HMRaster.fromGridCoverage(inAttribute);
	        RegionMap regionMap = flowRasterW.getRegionMap();
	        int cols = regionMap.getCols();
	        int rows = regionMap.getRows();
	
	        pm.beginTask("Marking the network...", rows); //$NON-NLS-1$
	        /*
	         * mark network as outlet, in order to easier stop on the net 
	         * while going downstream
	         */
	        for( int j = 0; j < rows; j++ ) {
	            for( int i = 0; i < cols; i++ ) {
	                if (netRaster.getValue(i, j) == FlowNode.NETVALUE)
	                    flowRasterW.setValue(i, j, FlowNode.OUTLET);
	            }
	            pm.worked(1);
	        }
	        pm.done();
	
			h2caRaster = new HMRaster.HMRasterWritableBuilder().setName("h2ca").setTemplate(flowRasterW)
					.setInitialValue(HMConstants.doubleNovalue).setNoValue(HMConstants.doubleNovalue).build();
	        
	        ModelsEngine.markHillSlopeWithLinkValue(flowRasterW, attributeRaster, h2caRaster, pm);
	
	        outAttribute = h2caRaster.buildCoverage();
        } finally {
			if (flowRasterW != null) {
				flowRasterW.close();
			}
			if (netRaster != null) {
				netRaster.close();
			}
			if (attributeRaster != null) {
				attributeRaster.close();
			}
			if (h2caRaster != null) {
				h2caRaster.close();
			}	
		}	

    }

}
