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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections.OmsRiverSectionsExtractor.*;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections.OmsRiverSectionsExtractor;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

@Description(DESCRIPTION)
@Documentation(DOCUMENTATION)
@Author(name = AUTHORNAMES, contact = AUTHORCONTACTS)
@Keywords(KEYWORDS)
@Label(LABEL)
@Name(NAME)
@Status(STATUS)
@License(LICENSE)
public class RiverSectionsExtractor extends HMModel {

    @Description(inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev = null;

    @Description(inRiver_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inRiver = null;

    @Description(inRiverPoints_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inRiverPoints = null;

    @Description(inBridges_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inBridges = null;

    @Description(inSections_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inSections = null;

    @Description(pSectionsIntervalDistance_DESCRIPTION)
    @Unit("m")
    @In
    public double pSectionsIntervalDistance = 10.0;

    @Description(pSectionsWidth_DESCRIPTION)
    @Unit("m")
    @In
    public double pSectionsWidth = 10.0;

    @Description(pBridgeBuffer_DESCRIPTION)
    @Unit("m")
    @In
    public double pBridgeBuffer = 0.0;

    @Description(fBridgeWidth_DESCRIPTION)
    @In
    public String fBridgeWidth;

    @Description(outSections_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outSections = null;

    @Description(outSectionPoints_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outSectionPoints = null;

    @Description(outRiverPoints_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRiverPoints = null;

    @Execute
    public void process() throws Exception {
        OmsRiverSectionsExtractor extractor = new OmsRiverSectionsExtractor();
        extractor.inElev = getRaster(inElev);
        extractor.inRiver = getVector(inRiver);
        extractor.inRiverPoints = getVector(inRiverPoints);
        extractor.inBridges = getVector(inBridges);
        extractor.inSections = getVector(inSections);
        extractor.pSectionsIntervalDistance = pSectionsIntervalDistance;
        extractor.pSectionsWidth = pSectionsWidth;
        extractor.pBridgeBuffer = pBridgeBuffer;
        extractor.fBridgeWidth = fBridgeWidth;
        extractor.pm = pm;
        extractor.doProcess = doProcess;
        extractor.doReset = doReset;
        extractor.process();
        dumpVector(extractor.outSections, outSections);
        dumpVector(extractor.outSectionPoints, outSectionPoints);
        dumpVector(extractor.outRiverPoints, outRiverPoints);
    }
}
