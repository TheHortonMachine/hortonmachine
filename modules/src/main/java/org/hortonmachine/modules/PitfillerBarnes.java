/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.modules;

import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfillerBarnes.OMSPITFILLERBARNES_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfillerBarnes.OMSPITFILLERBARNES_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfillerBarnes.OMSPITFILLERBARNES_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfillerBarnes.OMSPITFILLERBARNES_KEYWORDS;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfillerBarnes.OMSPITFILLERBARNES_LABEL;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfillerBarnes.OMSPITFILLERBARNES_LICENSE;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfillerBarnes.OMSPITFILLERBARNES_NAME;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfillerBarnes.OMSPITFILLERBARNES_STATUS;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfillerBarnes.OMSPITFILLERBARNES_pThreads_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfillerBarnes.OMSPITFILLERBARNES_pThres_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfillerBarnes.OMSPITFILLERBARNES_pTileSize_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfillerBarnes;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSPITFILLERBARNES_DESCRIPTION)
@Bibliography(OmsPitfillerBarnes.BIBLIOGRAPHY_BARNES_2016)
@Author(name = OMSPITFILLERBARNES_AUTHORNAMES, contact = OMSPITFILLERBARNES_AUTHORCONTACTS)
@Keywords(OMSPITFILLERBARNES_KEYWORDS)
@Label(OMSPITFILLERBARNES_LABEL)
@Name("_" + OMSPITFILLERBARNES_NAME)
@Status(OMSPITFILLERBARNES_STATUS)
@License(OMSPITFILLERBARNES_LICENSE)
public class PitfillerBarnes extends HMModel {
    @Description(inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev;

    @Description(doLargeFile_DESCRIPTION)
    @In
    public boolean doLargeFile = false;

    @Description(OMSPITFILLERBARNES_pTileSize_DESCRIPTION)
    @In
    public int pTileSize = 1024;

    @Description(OMSPITFILLERBARNES_pThreads_DESCRIPTION)
    @In
    public int pThreads = getDefaultThreadsNum();

    @Description(outPit_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outPit = null;

    @Description(outFlow_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outFlow = null;

    @Description(outTca_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTca = null;

    @Description(OMSPITFILLERBARNES_pThres_DESCRIPTION)
    @In
    public long pThres = 100;

    @Description(outNet_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNet = null;

    public static final String inElev_DESCRIPTION = "The map of digital elevation model (DEM). In large file mode it has to be a GeoTIFF.";
    public static final String doLargeFile_DESCRIPTION = "Process file to file, without loading the DEM in memory, for DEMs that do not fit in memory. The outputs are written as tiled GeoTIFFs (default is false).";
    public static final String outPit_DESCRIPTION = "The depitted elevation map.";
    public static final String outFlow_DESCRIPTION = "The optional map of D8 flow directions of the depitted map, flats included (10 for outlets).";
    public static final String outNet_DESCRIPTION = "The optional map of the network extracted with the threshold on the total contributing area (network cells have value 2).";
    public static final String outTca_DESCRIPTION ="The optional map of total contributing areas (in cells) of the flow directions. Integers when the number of valid cells allows it, else doubles.";

    @Execute
    public void process() throws Exception {
        checkNull(inElev, outPit);
        OmsPitfillerBarnes pitfiller = new OmsPitfillerBarnes();
        pitfiller.pm = pm;
        pitfiller.doProcess = doProcess;
        pitfiller.doReset = doReset;
        pitfiller.pTileSize = pTileSize;
        pitfiller.pThreads = pThreads;
        pitfiller.doFlow = outFlow != null && outFlow.trim().length() > 0;
        pitfiller.doTca = outTca != null && outTca.trim().length() > 0;
        pitfiller.doNet = outNet != null && outNet.trim().length() > 0;
        pitfiller.pThres = pThres;
        if (doLargeFile) {
            pitfiller.doLargeFile = true;
            pitfiller.inElevFile = inElev;
            pitfiller.outPitFile = outPit;
            pitfiller.outFlowFile = pitfiller.doFlow ? outFlow : null;
            pitfiller.outTcaFile = pitfiller.doTca ? outTca : null;
            pitfiller.outNetFile = pitfiller.doNet ? outNet : null;
            pitfiller.process();
        } else {
            pitfiller.inElev = getRaster(inElev);
            pitfiller.process();
            dumpRaster(pitfiller.outPit, outPit);
            if (pitfiller.doFlow) {
                dumpRaster(pitfiller.outFlow, outFlow);
            }
            if (pitfiller.doTca) {
                dumpRaster(pitfiller.outTca, outTca);
            }
            if (pitfiller.doNet) {
                dumpRaster(pitfiller.outNet, outNet);
            }
        }
    }
}
