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
package org.jgrasstools.modules;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_inVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_outVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_pTransX_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_pTransY_DESCRIPTION;
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
import org.jgrasstools.gears.modules.v.vectortransformer.OmsVectorTransformer;

@Description(OMSVECTORTRANSFORMER_DESCRIPTION)
@Documentation(OMSVECTORTRANSFORMER_DOCUMENTATION)
@Author(name = OMSVECTORTRANSFORMER_AUTHORNAMES, contact = OMSVECTORTRANSFORMER_AUTHORCONTACTS)
@Keywords(OMSVECTORTRANSFORMER_KEYWORDS)
@Label(OMSVECTORTRANSFORMER_LABEL)
@Name("_" + OMSVECTORTRANSFORMER_NAME)
@Status(OMSVECTORTRANSFORMER_STATUS)
@License(OMSVECTORTRANSFORMER_LICENSE)
public class VectorTransformer extends JGTModel {

    @Description(OMSVECTORTRANSFORMER_inVector_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVector;

    @Description(OMSVECTORTRANSFORMER_pTransX_DESCRIPTION)
    @In
    public double pTransX;

    @Description(OMSVECTORTRANSFORMER_pTransY_DESCRIPTION)
    @In
    public double pTransY;

    @Description(OMSVECTORTRANSFORMER_outVector_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outVector = null;

    @Execute
    public void process() throws Exception {
        OmsVectorTransformer vectortransformer = new OmsVectorTransformer();
        vectortransformer.inVector = getVector(inVector);
        vectortransformer.pTransX = pTransX;
        vectortransformer.pTransY = pTransY;
        vectortransformer.pm = pm;
        vectortransformer.doProcess = doProcess;
        vectortransformer.doReset = doReset;
        vectortransformer.process();
        dumpVector(vectortransformer.outVector, outVector);
    }
}
