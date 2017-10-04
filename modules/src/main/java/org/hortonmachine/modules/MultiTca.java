package org.hortonmachine.modules;

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_inCp9_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_inPit_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMULTITCA_outMultiTca_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.geomorphology.multitca.OmsMultiTca;

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

@Description(OMSMULTITCA_DESCRIPTION)
@Author(name = OMSMULTITCA_AUTHORNAMES, contact = OMSMULTITCA_AUTHORCONTACTS)
@Keywords(OMSMULTITCA_KEYWORDS)
@Label(OMSMULTITCA_LABEL)
@Name("_" + OMSMULTITCA_NAME)
@Status(OMSMULTITCA_STATUS)
@License(OMSMULTITCA_LICENSE)
public class MultiTca extends HMModel {
    @Description(OMSMULTITCA_inPit_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inPit = null;

    @Description(OMSMULTITCA_inFlow_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow = null;

    @Description(OMSMULTITCA_inCp9_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inCp9 = null;

    @Description(OMSMULTITCA_outMultiTca_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outMultiTca = null;

    @Execute
    public void process() throws Exception {
        OmsMultiTca multitca = new OmsMultiTca();
        multitca.inPit = getRaster(inPit);
        multitca.inFlow = getRaster(inFlow);
        multitca.inCp9 = getRaster(inCp9);
        multitca.pm = pm;
        multitca.doProcess = doProcess;
        multitca.doReset = doReset;
        multitca.process();
        dumpRaster(multitca.outMultiTca, outMultiTca);
    }
}
