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
package org.jgrasstools.gears.modules.v.vectorfieldrounder;

import java.text.DecimalFormat;

import oms3.annotations.Author;
import oms3.annotations.Documentation;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.opengis.feature.simple.SimpleFeature;

@Description("Module that joins attributes from one featurecollection into another based on a common field.")
@Documentation("VectorFieldRounder.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Join, VectorTableJoiner")
@Label(JGTConstants.VECTORPROCESSING)
@Status(Status.CERTIFIED)
@Name("vround")
@License("General Public License Version 3 (GPLv3)")
public class VectorFieldRounder extends JGTModel {

    @Description("The features of which to round a numeric value.")
    @In
    public SimpleFeatureCollection inFeatures;

    @Description("The double field of the number to round.")
    @In
    public String fRound = null;

    @Description("The rounding pattern.")
    @In
    public String pPattern = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The modified features.")
    @Out
    public SimpleFeatureCollection outFeatures;

    private DecimalFormat formatter = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outFeatures == null, doReset)) {
            return;
        }

        checkNull(pPattern, fRound);

        formatter = new DecimalFormat(pPattern);

        outFeatures = FeatureCollections.newCollection();

        int size = inFeatures.size();
        pm.beginTask("Rounding data...", size);
        FeatureIterator<SimpleFeature> inFeatureIterator = inFeatures.features();
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

            outFeatures.add(feature);
            pm.worked(1);
        }
        pm.done();

        inFeatureIterator.close();

    }

}
