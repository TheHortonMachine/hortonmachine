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
package org.hortonmachine.gears.modules.v.vectorfilter;

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

import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.modules.v.vectorfilter.OmsVectorFilter.OMSVECTORFILTER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.vectorfilter.OmsVectorFilter.OMSVECTORFILTER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.vectorfilter.OmsVectorFilter.OMSVECTORFILTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorfilter.OmsVectorFilter.OMSVECTORFILTER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.vectorfilter.OmsVectorFilter.OMSVECTORFILTER_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorfilter.OmsVectorFilter.OMSVECTORFILTER_KEYWORDS;
import static org.hortonmachine.gears.modules.v.vectorfilter.OmsVectorFilter.OMSVECTORFILTER_LABEL;
import static org.hortonmachine.gears.modules.v.vectorfilter.OmsVectorFilter.OMSVECTORFILTER_LICENSE;
import static org.hortonmachine.gears.modules.v.vectorfilter.OmsVectorFilter.OMSVECTORFILTER_NAME;
import static org.hortonmachine.gears.modules.v.vectorfilter.OmsVectorFilter.OMSVECTORFILTER_OUT_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorfilter.OmsVectorFilter.OMSVECTORFILTER_P_CQL_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorfilter.OmsVectorFilter.OMSVECTORFILTER_STATUS;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FilterUtilities;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.Filter;

@Description(OMSVECTORFILTER_DESCRIPTION)
@Documentation(OMSVECTORFILTER_DOCUMENTATION)
@Author(name = OMSVECTORFILTER_AUTHORNAMES, contact = OMSVECTORFILTER_AUTHORCONTACTS)
@Keywords(OMSVECTORFILTER_KEYWORDS)
@Label(OMSVECTORFILTER_LABEL)
@Name(OMSVECTORFILTER_NAME)
@Status(OMSVECTORFILTER_STATUS)
@License(OMSVECTORFILTER_LICENSE)
public class OmsVectorFilter extends HMModel {

    @Description(OMSVECTORFILTER_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector;

    @Description(OMSVECTORFILTER_P_CQL_DESCRIPTION)
    @In
    public String pCql = null;

    @Description(OMSVECTORFILTER_OUT_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector;

    public static final String OMSVECTORFILTER_DESCRIPTION = "Keeps the features of a vector that satisfy a filter on their attributes or on their geometry.";
    public static final String OMSVECTORFILTER_DOCUMENTATION = """
            The filter is written in ECQL, the extended Common Query Language of GeoTools and GeoServer. \
            It compares the fields, by the names they have in the vector, with values and with each other, \
            and joins the comparisons with AND, OR and NOT, as in area > 10000 AND avgelev < 1800. Text \
            values go between single quotes, as name = 'Rio Torto', and LIKE matches them with % as \
            wildcard: name LIKE 'Rio%'. Other forms are netnum IN (1, 2, 3), avgelev BETWEEN 1500 AND 2000 \
            and name IS NULL. The comparisons can use expressions too, as maxelev - minelev > 300.

            The geometry, the_geom in a shapefile, is filtered with the spatial functions, with geometries \
            in WKT and in the crs of the vector: INTERSECTS(the_geom, POINT(1638500 5112500)), \
            BBOX(the_geom, 1637000, 5111000, 1638000, 5112000) or DWITHIN(the_geom, POINT(1638500 \
            5112500), 100, meters).
            """;
    public static final String OMSVECTORFILTER_KEYWORDS = "Filter, Vector, CQL, ECQL, Select";
    public static final String OMSVECTORFILTER_LABEL = VECTORPROCESSING;
    public static final String OMSVECTORFILTER_NAME = "vfilter";
    public static final int OMSVECTORFILTER_STATUS = 40;
    public static final String OMSVECTORFILTER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSVECTORFILTER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSVECTORFILTER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSVECTORFILTER_IN_VECTOR_DESCRIPTION = "The vector to filter.";
    public static final String OMSVECTORFILTER_P_CQL_DESCRIPTION = "The ECQL filter, as area > 10000 AND name LIKE 'Rio%'.";
    public static final String OMSVECTORFILTER_OUT_VECTOR_DESCRIPTION = "The filtered vector.";

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }
        checkNull(inVector, pCql);

        Filter cqlFilter = FilterUtilities.getCQLFilter(pCql);
        SimpleFeatureCollection subCollection = inVector.subCollection(cqlFilter);

        outVector = new DefaultFeatureCollection();
        SimpleFeatureIterator iterator = subCollection.features();
        try {
            while( iterator.hasNext() ) {
                SimpleFeature feature = iterator.next();
                ((DefaultFeatureCollection) outVector).add(feature);
            }
        } finally {
            iterator.close();
        }
    }

}
