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
package org.jgrasstools.gears.modules.v.featurefilter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.features.FilterUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

@Description("Module that creates a filteres feature collection.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Filter, Vector")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class FeatureFilter extends JGTModel {

    @Description("The features to filter.")
    @In
    public SimpleFeatureCollection inFeatures;

    @Description("The CQL filter.")
    @In
    public String pCql = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The filtered features.")
    @Out
    public SimpleFeatureCollection outFeatures;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outFeatures == null, doReset)) {
            return;
        }
        checkNull(inFeatures, pCql);

        Filter cqlFilter = FilterUtilities.getCQLFilter(pCql);
        SimpleFeatureCollection subCollection = inFeatures
                .subCollection(cqlFilter);
        
        outFeatures = FeatureCollections.newCollection();
        FeatureIterator<SimpleFeature> iterator = subCollection.features();
        while( iterator.hasNext() ) {
            SimpleFeature feature = iterator.next();
            outFeatures.add(feature);
        }
        subCollection.close(iterator);
    }

}
