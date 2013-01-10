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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_fRound_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_inVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_outVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_pPattern_DESCRIPTION;

import java.text.DecimalFormat;

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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.opengis.feature.simple.SimpleFeature;

@Description(OMSVECTORFIELDROUNDER_DESCRIPTION)
@Documentation(OMSVECTORFIELDROUNDER_DOCUMENTATION)
@Author(name = OMSVECTORFIELDROUNDER_AUTHORNAMES, contact = OMSVECTORFIELDROUNDER_AUTHORCONTACTS)
@Keywords(OMSVECTORFIELDROUNDER_KEYWORDS)
@Label(OMSVECTORFIELDROUNDER_LABEL)
@Name("_" + OMSVECTORFIELDROUNDER_NAME)
@Status(OMSVECTORFIELDROUNDER_STATUS)
@License(OMSVECTORFIELDROUNDER_LICENSE)
public class OmsVectorFieldRounder extends JGTModel {

    @Description(OMSVECTORFIELDROUNDER_inVector_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public SimpleFeatureCollection inVector;

    @Description(OMSVECTORFIELDROUNDER_fRound_DESCRIPTION)
    @In
    public String fRound = null;

    @Description(OMSVECTORFIELDROUNDER_pPattern_DESCRIPTION)
    @In
    public String pPattern = null;

    @Description(OMSVECTORFIELDROUNDER_outVector_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public SimpleFeatureCollection outVector;

    private DecimalFormat formatter = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }

        checkNull(pPattern, fRound);

        formatter = new DecimalFormat(pPattern);

        outVector = FeatureCollections.newCollection();

        int size = inVector.size();
        pm.beginTask("Rounding data...", size);
        FeatureIterator<SimpleFeature> inFeatureIterator = inVector.features();
        while( inFeatureIterator.hasNext() ) {
            SimpleFeature feature = inFeatureIterator.next();

            Object attribute = feature.getAttribute(fRound);
            if (attribute instanceof Number) {
                double num = ((Number) attribute).doubleValue();
                String numStr = formatter.format(num);
                numStr = numStr.replaceFirst(",", ".");
                num = Double.parseDouble(numStr);
                feature.setAttribute(fRound, num);
            }

            outVector.add(feature);
            pm.worked(1);
        }
        pm.done();

        inFeatureIterator.close();

    }

}
