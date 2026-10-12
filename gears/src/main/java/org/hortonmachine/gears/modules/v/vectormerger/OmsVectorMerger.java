/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.gears.modules.v.vectormerger;

import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.modules.v.vectormerger.OmsVectorMerger.OMSVECTORMERGER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.vectormerger.OmsVectorMerger.OMSVECTORMERGER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.vectormerger.OmsVectorMerger.OMSVECTORMERGER_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectormerger.OmsVectorMerger.OMSVECTORMERGER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.vectormerger.OmsVectorMerger.OMSVECTORMERGER_KEYWORDS;
import static org.hortonmachine.gears.modules.v.vectormerger.OmsVectorMerger.OMSVECTORMERGER_LABEL;
import static org.hortonmachine.gears.modules.v.vectormerger.OmsVectorMerger.OMSVECTORMERGER_LICENSE;
import static org.hortonmachine.gears.modules.v.vectormerger.OmsVectorMerger.OMSVECTORMERGER_NAME;
import static org.hortonmachine.gears.modules.v.vectormerger.OmsVectorMerger.OMSVECTORMERGER_STATUS;

import java.util.List;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.features.FeatureExtender;

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

@Description(OMSVECTORMERGER_DESCRIPTION)
@Documentation(OMSVECTORMERGER_DOCUMENTATION)
@Author(name = OMSVECTORMERGER_AUTHORNAMES, contact = OMSVECTORMERGER_AUTHORCONTACTS)
@Keywords(OMSVECTORMERGER_KEYWORDS)
@Label(OMSVECTORMERGER_LABEL)
@Name(OMSVECTORMERGER_NAME)
@Status(OMSVECTORMERGER_STATUS)
@License(OMSVECTORMERGER_LICENSE)
public class OmsVectorMerger extends HMModel {

    @Description(OMSVECTORMERGER_IN_VECTORS_DESCRIPTION)
    @In
    public List<SimpleFeatureCollection> inVectors;

    @Description(OMSVECTORMERGER_OUT_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector;

    public static final String OMSVECTORMERGER_DESCRIPTION = "Merges vectors with the same fields into a single one.";
    public static final String OMSVECTORMERGER_DOCUMENTATION = """
            The vectors must have the same geometry type and the same fields, with the same names, types \
            and order, and the same crs: a vector with more or fewer fields stops the merge, as one in \
            another crs, which would need VectorReprojector first. The features are copied as they are, the \
            ones of the first vector first, without joining the geometries that touch or overlap: the \
            merge of two maps of adjacent areas has both the features along their border.
            """;
    public static final String OMSVECTORMERGER_KEYWORDS = "IO, Feature, Vector, Merge";
    public static final String OMSVECTORMERGER_LABEL = VECTORPROCESSING;
    public static final String OMSVECTORMERGER_NAME = "vmerge";
    public static final int OMSVECTORMERGER_STATUS = 40;
    public static final String OMSVECTORMERGER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSVECTORMERGER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSVECTORMERGER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSVECTORMERGER_IN_VECTORS_DESCRIPTION = "The input vectors to be merged.";
    public static final String OMSVECTORMERGER_IN_VECTOR1_DESCRIPTION = "The first vector to be merged.";
    public static final String OMSVECTORMERGER_IN_VECTOR2_DESCRIPTION = "The second vector to be merged.";
    public static final String OMSVECTORMERGER_OUT_VECTOR_DESCRIPTION = "The output vector.";

    @Execute
    public void process() throws Exception {
        checkNull(inVectors);

        SimpleFeatureType firstType = null;

        FeatureExtender fEx = null;

        pm.beginTask("Merging features...", inVectors.size());
        try {
            ListFeatureCollection outFC = null;
            for( SimpleFeatureCollection featureCollection : inVectors ) {
                if (firstType == null) {
                    firstType = featureCollection.getSchema();
                    fEx = new FeatureExtender(firstType, new String[0], new Class< ? >[0]);
                    // a list keeps the features in the order of the vectors
                    outFC = new ListFeatureCollection(fEx.getNewFeatureType());
                } else {
                    SimpleFeatureType schema = featureCollection.getSchema();
                    int compare = DataUtilities.compare(firstType, schema);
                    if (compare != 0) {
                        throw new ModelsIllegalargumentException("Merging is done only on same feature types.", this, pm);
                    }
                    if (!HMCrsRegistry.crsEquals(firstType.getCoordinateReferenceSystem(),
                            schema.getCoordinateReferenceSystem())) {
                        throw new ModelsIllegalargumentException("Merging is done only on vectors with the same crs.", this,
                                pm);
                    }
                }
                SimpleFeatureIterator featureIterator = featureCollection.features();
                try {
                    while( featureIterator.hasNext() ) {
                        SimpleFeature f = featureIterator.next();
                        outFC.add(fEx.extendFeature(f, new Object[0]));
                    }
                } finally {
                    featureIterator.close();
                }
                pm.worked(1);
            }
            outVector = outFC;
        } finally {
            pm.done();
        }
    }
}
