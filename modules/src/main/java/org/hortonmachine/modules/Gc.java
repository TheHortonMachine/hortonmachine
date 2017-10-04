package org.hortonmachine.modules;

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSGC_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSGC_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSGC_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSGC_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSGC_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSGC_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSGC_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSGC_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSGC_inCp9_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSGC_inNetwork_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSGC_inSlope_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSGC_outAggregateClasses_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSGC_outClasses_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSGC_pTh_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.geomorphology.gc.OmsGc;

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

@Description(OMSGC_DESCRIPTION)
@Author(name = OMSGC_AUTHORNAMES, contact = OMSGC_AUTHORCONTACTS)
@Keywords(OMSGC_KEYWORDS)
@Label(OMSGC_LABEL)
@Name("_" + OMSGC_NAME)
@Status(OMSGC_STATUS)
@License(OMSGC_LICENSE)
public class Gc extends HMModel {
    @Description(OMSGC_inSlope_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inSlope = null;

    @Description(OMSGC_inNetwork_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNetwork = null;

    @Description(OMSGC_inCp9_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inCp9 = null;

    @Description(OMSGC_pTh_DESCRIPTION)
    @In
    public int pTh = 0;

    @Description(OMSGC_outClasses_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outClasses = null;

    @Description(OMSGC_outAggregateClasses_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outAggregateClasses = null;

    @Execute
    public void process() throws Exception {
        OmsGc gc = new OmsGc();
        gc.inSlope = getRaster(inSlope);
        gc.inNetwork = getRaster(inNetwork);
        gc.inCp9 = getRaster(inCp9);
        gc.pTh = pTh;
        gc.pm = pm;
        gc.doProcess = doProcess;
        gc.doReset = doReset;
        gc.process();
        dumpRaster(gc.outClasses, outClasses);
        dumpRaster(gc.outAggregateClasses, outAggregateClasses);
    }

}
