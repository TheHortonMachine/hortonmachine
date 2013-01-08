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

import java.util.List;

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

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollections;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureExtender;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

@Description("Module for merging vecotrs into one single.")
@Documentation("OmsVectorMerger.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("IO, Feature, Vector, Merge")
@Label(JGTConstants.VECTORPROCESSING)
@Status(Status.CERTIFIED)
@Name("_vmerge")
@License("General Public License Version 3 (GPLv3)")
public class OmsVectorMerger extends JGTModel {
    @Description("The input vectors to be merged.")
    @In
    public List<SimpleFeatureCollection> inVectors;

    @Description("The output vector.")
    @Out
    public SimpleFeatureCollection outVector;

    @Execute
    public void process() throws Exception {
        checkNull(inVectors);

        SimpleFeatureType firstType = null;

        FeatureExtender fEx = null;

        pm.beginTask("Merging features...", inVectors.size());
        try {
            outVector = FeatureCollections.newCollection();
            for( SimpleFeatureCollection featureCollection : inVectors ) {
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

                    outVector.add(extendFeature);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }
}
