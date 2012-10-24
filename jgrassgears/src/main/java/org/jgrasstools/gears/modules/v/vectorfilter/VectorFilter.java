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
package org.jgrasstools.gears.modules.v.vectorfilter;

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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollections;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FilterUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

@Description("Module that creates a subset of a vector based on a filtered vector.")
@Documentation("VectorFilter.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Filter, Vector, VectorReshaper")
@Label(JGTConstants.VECTORPROCESSING)
@Status(Status.CERTIFIED)
@Name("vfilter")
@License("General Public License Version 3 (GPLv3)")
public class VectorFilter extends JGTModel {

    @Description("The vector to filter.")
    @In
    public SimpleFeatureCollection inVector;

    @Description("The ECQL filter function.")
    @In
    public String pCql = null;

    @Description("The filtered vector.")
    @Out
    public SimpleFeatureCollection outVector;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }
        checkNull(inVector, pCql);

        Filter cqlFilter = FilterUtilities.getCQLFilter(pCql);
        SimpleFeatureCollection subCollection = inVector.subCollection(cqlFilter);

        outVector = FeatureCollections.newCollection();
        SimpleFeatureIterator iterator = subCollection.features();
        try {
            while( iterator.hasNext() ) {
                SimpleFeature feature = iterator.next();
                outVector.add(feature);
            }
        } finally {
            iterator.close();
        }
    }

}
