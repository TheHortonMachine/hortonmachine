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
package org.hortonmachine.hmachine.modules.demmanipulation.splitsubbasin;

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_inHack_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_outNetnum_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_outSubbasins_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_pHackorder_DESCRIPTION;

import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.libs.modules.ModelsSupporter;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.math.NumericsUtilities;

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

@Description(OMSSPLITSUBBASINS_DESCRIPTION)
@Author(name = OMSSPLITSUBBASINS_AUTHORNAMES, contact = OMSSPLITSUBBASINS_AUTHORCONTACTS)
@Keywords(OMSSPLITSUBBASINS_KEYWORDS)
@Label(OMSSPLITSUBBASINS_LABEL)
@Name(OMSSPLITSUBBASINS_NAME)
@Status(OMSSPLITSUBBASINS_STATUS)
@License(OMSSPLITSUBBASINS_LICENSE)
public class OmsSplitSubbasins extends HMModel {
    @Description(OMSSPLITSUBBASINS_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSSPLITSUBBASINS_inHack_DESCRIPTION)
    @In
    public GridCoverage2D inHack = null;

    @Description(OMSSPLITSUBBASINS_pHackorder_DESCRIPTION)
    @In
    public Double pHackorder = null;

    @Description(OMSSPLITSUBBASINS_outNetnum_DESCRIPTION)
    @Out
    public GridCoverage2D outNetnum = null;

    @Description(OMSSPLITSUBBASINS_outSubbasins_DESCRIPTION)
    @Out
    public GridCoverage2D outSubbasins = null;

    private int[][] dir = ModelsSupporter.DIR_WITHFLOW_ENTERING;

    private int nCols;
    private int nRows;
    private double hackOrder;

	private double hackNovalue;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outSubbasins == null, doReset)) {
            return;
        }
        checkNull(inFlow, inHack, pHackorder);


        hackOrder = pHackorder;

        HMRaster flowRasterW = null;
        HMRaster hackRasterW = null;
        HMRaster netRaster = null;
        HMRaster netNumberWR = null;
        HMRaster subbasinWR = null;
        try {
			flowRasterW = HMRaster.fromGridCoverageWritable(inFlow);
	        flowRasterW.makeNullBorders();
	        RegionMap regionMap = flowRasterW.getRegionMap();
	        nCols = regionMap.getCols();
	        nRows = regionMap.getRows();
	
			hackRasterW = HMRaster.fromGridCoverageWritable(inHack);
	        hackNovalue = hackRasterW.getNovalue();
	        hackRasterW.makeNullBorders();
	        
			netRaster = new HMRaster.HMRasterWritableBuilder().setName("net").setTemplate(flowRasterW)
					.setInitialValue(HMConstants.shortNovalue).setDoShort(true).setNoValue(HMConstants.shortNovalue).build();
	        
	        net(hackRasterW, netRaster);
	
			netNumberWR = netNumber(flowRasterW, hackRasterW, netRaster);
			subbasinWR = ModelsEngine.extractSubbasins(flowRasterW, netRaster, netNumberWR, pm);
	
	        outNetnum = netNumberWR.buildCoverage();
	        outSubbasins = subbasinWR.buildCoverage();
  		} finally {
			if (flowRasterW != null) flowRasterW.close();
			if (hackRasterW != null) hackRasterW.close();
			if (netRaster != null) netRaster.close();
			if (netNumberWR != null) netNumberWR.close();
			if (subbasinWR != null) subbasinWR.close();
  		}
    }
    /**
     * Return the map of the network with only the river of the choosen order.
     * 
     * @param hackRaster the hack stream map.
     * @param netRaster the network map to build on the required hack orders.
     * @return the map of the network with the choosen order.
     * @throws IOException 
     */
    private void net( HMRaster hackRaster, HMRaster netRaster ) throws IOException {
        // calculates the max order of basin (max hackstream value)
        pm.beginTask("Extraction of rivers of chosen order...", nRows);
        for( int r = 0; r < nRows; r++ ) {
            for( int c = 0; c < nCols; c++ ) {
                double value = hackRaster.getValue(c, r);
                if (!hackRaster.isNovalue(value)) {
                    /*
                     * if the hack value is in the asked range 
                     * => keep it as net
                     */
                    if (value <= hackOrder) {
                        netRaster.setValue(c, r, (short) 2);
                    } else {
                        hackRaster.setValue(c, r, hackNovalue);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

    private HMRaster netNumber( HMRaster flowR, HMRaster hacksR, HMRaster netR ) throws IOException {
        int drainingPixelNum = 0;
        int[] flowColRow = new int[2];

        
        HMRaster netNumberingRaster = new HMRaster.HMRasterWritableBuilder().setName("netnumbering").setTemplate(flowR)
				.setInitialValue(HMConstants.intNovalue).setDoInteger(true).setInitialValue(0).setNoValue(HMConstants.intNovalue).build();

        int n = 0;
        pm.beginTask("Numbering network...", nRows);
        for( int r = 0; r < nRows; r++ ) {
            for( int c = 0; c < nCols; c++ ) {
                flowColRow[0] = c;
                flowColRow[1] = r;
                if (!netR.isNovalue(netR.getValue(c, r)) && flowR.getValue(c, r) != 10.0
                        && NumericsUtilities.dEq(netNumberingRaster.getValue(c, r), 0.0)) {

                    boolean isSource = true;
                    for( int k = 1; k <= 8; k++ ) {
                        boolean isDraining = flowR.getValue(flowColRow[0] + dir[k][1], flowColRow[1] + dir[k][0]) == dir[k][2];
                        boolean isOnNet = !netR.isNovalue(
                                netR.getValue(flowColRow[0] + dir[k][1], flowColRow[1] + dir[k][0]));
                        if (isDraining && isOnNet) {
                            isSource = false;
                            break;
                        }
                    }

                    /*
                     * if it is source pixel, go down
                     */
                    if (isSource) {
                        n++;
                        netNumberingRaster.setValue(c, r, n);
                        if (!ModelsEngine.go_downstream(flowColRow, flowR.getValue(flowColRow[0], flowColRow[1])))
                            throw new ModelsIllegalargumentException("go_downstream failure...", this, pm);
                        /*
                         * while it is on the network, go downstream
                         */
                        while( !flowR.isNovalue(flowR.getValue(flowColRow[0], flowColRow[1]))
                                && netNumberingRaster.getValue(flowColRow[0], flowColRow[1]) == 0 ) {
                            /*
                             * calculate how many pixels drain into the current pixel.
                             */
                            drainingPixelNum = 0;
                            for( int k = 1; k <= 8; k++ ) {
                                if (!netR.isNovalue(netR.getValue(flowColRow[0] + dir[k][1], flowColRow[1] + dir[k][0]))
                                        && flowR.getValue(flowColRow[0] + dir[k][1], flowColRow[1] + dir[k][0]) == dir[k][2]) {
                                    drainingPixelNum++;
                                }
                            }

                            if (drainingPixelNum > 1) {
                                n++;
                            }
                            netNumberingRaster.setValue(flowColRow[0], flowColRow[1], n);

                            if (!ModelsEngine.go_downstream(flowColRow,
                                    flowR.getValue(flowColRow[0], flowColRow[1])))
                                throw new ModelsIllegalargumentException("go_downstream failure...", this, pm);
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        return netNumberingRaster;
    }

}
