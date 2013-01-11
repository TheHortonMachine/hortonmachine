package org.jgrasstools.modules;

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGC_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGC_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGC_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGC_DOCUMENTATION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGC_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGC_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGC_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGC_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGC_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGC_inCp9_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGC_inNetwork_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGC_inSlope_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGC_outAggregateClasses_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGC_outClasses_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGC_pTh_DESCRIPTION;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.geomorphology.gc.OmsGc;

@Description(OMSGC_DESCRIPTION)
@Documentation(OMSGC_DOCUMENTATION)
@Author(name = OMSGC_AUTHORNAMES, contact = OMSGC_AUTHORCONTACTS)
@Keywords(OMSGC_KEYWORDS)
@Label(OMSGC_LABEL)
@Name("_" + OMSGC_NAME)
@Status(OMSGC_STATUS)
@License(OMSGC_LICENSE)
public class Gc extends JGTModel {
    @Description(OMSGC_inSlope_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inSlope = null;

    @Description(OMSGC_inNetwork_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inNetwork = null;

    @Description(OMSGC_inCp9_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inCp9 = null;

    @Description(OMSGC_pTh_DESCRIPTION)
    @In
    public int pTh = 0;

    @Description(OMSGC_outClasses_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outClasses = null;

    @Description(OMSGC_outAggregateClasses_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
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
