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
package org.jgrasstools.gears.modules.v.attributesrounder;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.features.FeatureExtender;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.GeometryFactory;

@Description("Module that joins attributes from one featurecollection into another based on a common field.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Join, Vector")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class AttributesRounder extends JGTModel {

    @Description("The features of which to round a numeric value.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> inFeatures;

    @Description("The double field of the number to round.")
    @In
    public String fRound = null;

    @Description("The rounding pattern.")
    @In
    public String pPattern = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The modified features.")
    @Out
    public FeatureCollection<SimpleFeatureType, SimpleFeature> outFeatures;

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
                num = Double.parseDouble(numStr);
                feature.setAttribute(fRound, num);
            }

            outFeatures.add(feature);
            pm.worked(1);
        }
        pm.done();

        inFeatures.close(inFeatureIterator);

    }

}
