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
package org.jgrasstools.gears.modules.v.vectormerger;

import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollections;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.features.FeatureExtender;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

@Description("A simple module that merges same featurecollections into one single.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Feature, Vector, Merge")
@Label(JGTConstants.VECTORPROCESSING)
@Status(Status.EXPERIMENTAL)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class VectorMerger extends JGTModel {
    @Description("The input features.")
    @In
    public List<SimpleFeatureCollection> inGeodata;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The output features.")
    @Out
    public SimpleFeatureCollection outGeodata;

    @Execute
    public void process() throws Exception {
        checkNull(inGeodata);

        SimpleFeatureType firstType = null;

        FeatureExtender fEx = null;

        pm.beginTask("Merging features...", inGeodata.size());
        try {
            outGeodata = FeatureCollections.newCollection();
            for( SimpleFeatureCollection featureCollection : inGeodata ) {
                if (firstType == null) {
                    firstType = featureCollection.getSchema();
                    fEx = new FeatureExtender(firstType, new String[0], new Class< ? >[0]);
                } else {
                    SimpleFeatureType schema = featureCollection.getSchema();
                    int compare = DataUtilities.compare(firstType, schema);
                    if (compare != 0) {
                        throw new ModelsIllegalargumentException("Merging is done only on same feature types.", this);
                    }
                }
                SimpleFeatureIterator featureIterator = featureCollection.features();
                while( featureIterator.hasNext() ) {
                    SimpleFeature f = featureIterator.next();

                    SimpleFeature extendFeature = fEx.extendFeature(f, new Object[0]);

                    outGeodata.add(extendFeature);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }
}
