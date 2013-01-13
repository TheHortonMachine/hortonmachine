package org.jgrasstools.modules;

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMULTITCA_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMULTITCA_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMULTITCA_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMULTITCA_DOCUMENTATION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMULTITCA_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMULTITCA_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMULTITCA_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMULTITCA_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMULTITCA_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMULTITCA_inCp9_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMULTITCA_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMULTITCA_inPit_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMULTITCA_outMultiTca_DESCRIPTION;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.geomorphology.multitca.OmsMultiTca;

@Description(OMSMULTITCA_DESCRIPTION)
@Documentation(OMSMULTITCA_DOCUMENTATION)
@Author(name = OMSMULTITCA_AUTHORNAMES, contact = OMSMULTITCA_AUTHORCONTACTS)
@Keywords(OMSMULTITCA_KEYWORDS)
@Label(OMSMULTITCA_LABEL)
@Name("_" + OMSMULTITCA_NAME)
@Status(OMSMULTITCA_STATUS)
@License(OMSMULTITCA_LICENSE)
public class MultiTca extends JGTModel {
    @Description(OMSMULTITCA_inPit_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inPit = null;

    @Description(OMSMULTITCA_inFlow_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFlow = null;

    @Description(OMSMULTITCA_inCp9_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inCp9 = null;

    @Description(OMSMULTITCA_outMultiTca_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
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
