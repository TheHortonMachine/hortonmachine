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
package org.jgrasstools.modules;

import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.intensityclassifier.OmsHazardClassifier.AUTHORS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.intensityclassifier.OmsHazardClassifier.CONTACT;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.intensityclassifier.OmsHazardClassifier.DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.intensityclassifier.OmsHazardClassifier.KEYWORDS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.intensityclassifier.OmsHazardClassifier.LABEL;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.intensityclassifier.OmsHazardClassifier.LICENSE;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.intensityclassifier.OmsHazardClassifier.NAME;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.intensityclassifier.OmsHazardClassifier.STATUS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.intensityclassifier.OmsHazardClassifier.inIntensityTr100_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.intensityclassifier.OmsHazardClassifier.inIntensityTr200_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.intensityclassifier.OmsHazardClassifier.inIntensityTr30_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.intensityclassifier.OmsHazardClassifier.outHazardIP1_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.intensityclassifier.OmsHazardClassifier.outHazardIP2_DESCR;
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
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.intensityclassifier.OmsHazardClassifier;

@Description(DESCRIPTION)
@Author(name = AUTHORS, contact = CONTACT)
@Keywords(KEYWORDS)
@Label(LABEL)
@Name(NAME)
@Status(STATUS)
@License(LICENSE)
public class HazardClassifier extends JGTModel {

    @Description(inIntensityTr200_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inIntensityTr200;

    @Description(inIntensityTr100_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inIntensityTr100;

    @Description(inIntensityTr30_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inIntensityTr30;

    @Description(outHazardIP1_DESCR)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outHazardIP1 = null;

    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Description(outHazardIP2_DESCR)
    @In
    public String outHazardIP2 = null;

    @Execute
    public void process() throws Exception {
        OmsHazardClassifier classifier = new OmsHazardClassifier();
        classifier.inIntensityTr200 = getRaster(inIntensityTr200);
        classifier.inIntensityTr100 = getRaster(inIntensityTr100);
        classifier.inIntensityTr30 = getRaster(inIntensityTr30);
        classifier.pm = pm;
        classifier.process();
        dumpRaster(classifier.outHazardIP1, outHazardIP1);
        dumpRaster(classifier.outHazardIP2, outHazardIP2);
    }
}
