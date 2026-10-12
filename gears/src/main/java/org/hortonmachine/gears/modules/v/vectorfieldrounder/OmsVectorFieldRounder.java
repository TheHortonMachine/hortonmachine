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
package org.hortonmachine.gears.modules.v.vectorfieldrounder;

import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.modules.v.vectorfieldrounder.OmsVectorFieldRounder.OMSVECTORFIELDROUNDER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.vectorfieldrounder.OmsVectorFieldRounder.OMSVECTORFIELDROUNDER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.vectorfieldrounder.OmsVectorFieldRounder.OMSVECTORFIELDROUNDER_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorfieldrounder.OmsVectorFieldRounder.OMSVECTORFIELDROUNDER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.vectorfieldrounder.OmsVectorFieldRounder.OMSVECTORFIELDROUNDER_KEYWORDS;
import static org.hortonmachine.gears.modules.v.vectorfieldrounder.OmsVectorFieldRounder.OMSVECTORFIELDROUNDER_LABEL;
import static org.hortonmachine.gears.modules.v.vectorfieldrounder.OmsVectorFieldRounder.OMSVECTORFIELDROUNDER_LICENSE;
import static org.hortonmachine.gears.modules.v.vectorfieldrounder.OmsVectorFieldRounder.OMSVECTORFIELDROUNDER_NAME;
import static org.hortonmachine.gears.modules.v.vectorfieldrounder.OmsVectorFieldRounder.OMSVECTORFIELDROUNDER_STATUS;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureUtilities;

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

@Description(OMSVECTORFIELDROUNDER_DESCRIPTION)
@Documentation(OMSVECTORFIELDROUNDER_DOCUMENTATION)
@Author(name = OMSVECTORFIELDROUNDER_AUTHORNAMES, contact = OMSVECTORFIELDROUNDER_AUTHORCONTACTS)
@Keywords(OMSVECTORFIELDROUNDER_KEYWORDS)
@Label(OMSVECTORFIELDROUNDER_LABEL)
@Name(OMSVECTORFIELDROUNDER_NAME)
@Status(OMSVECTORFIELDROUNDER_STATUS)
@License(OMSVECTORFIELDROUNDER_LICENSE)
public class OmsVectorFieldRounder extends HMModel {

    @Description(OMSVECTORFIELDROUNDER_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector;

    @Description(OMSVECTORFIELDROUNDER_F_ROUND_DESCRIPTION)
    @In
    public String fRound = null;

    @Description(OMSVECTORFIELDROUNDER_P_PATTERN_DESCRIPTION)
    @In
    public String pPattern = null;

    @Description(OMSVECTORFIELDROUNDER_OUT_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector;

    public static final String OMSVECTORFIELDROUNDER_DESCRIPTION = "Rounds the values of a numeric field of a vector to the decimals of a pattern.";
    public static final String OMSVECTORFIELDROUNDER_DOCUMENTATION = """
            The pattern is a Java DecimalFormat pattern, with the dot as decimal separator whatever the \
            language of the system: 0.00 rounds to two decimals, # to the integer. The values are rounded \
            half up, so 2.5 becomes 3 and 2.25 with 0.0 becomes 2.3. The values that are not numbers, as \
            the empty ones, are left as they are.

            The field keeps its type, and a shapefile stores the values with the number of decimals that \
            the field has in its table.
            """;
    public static final String OMSVECTORFIELDROUNDER_KEYWORDS = "Round, Vector, Attributes";
    public static final String OMSVECTORFIELDROUNDER_LABEL = VECTORPROCESSING;
    public static final String OMSVECTORFIELDROUNDER_NAME = "vround";
    public static final int OMSVECTORFIELDROUNDER_STATUS = 40;
    public static final String OMSVECTORFIELDROUNDER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSVECTORFIELDROUNDER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSVECTORFIELDROUNDER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSVECTORFIELDROUNDER_IN_VECTOR_DESCRIPTION = "The vector of which to round a numeric value.";
    public static final String OMSVECTORFIELDROUNDER_F_ROUND_DESCRIPTION = "The numeric field to round.";
    public static final String OMSVECTORFIELDROUNDER_P_PATTERN_DESCRIPTION = "The rounding pattern, as 0.00 for two decimals.";
    public static final String OMSVECTORFIELDROUNDER_OUT_VECTOR_DESCRIPTION = "The modified vector.";

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }

        checkNull(inVector, pPattern, fRound);

        String field = FeatureUtilities.findAttributeName(inVector.getSchema(), fRound);
        if (field == null) {
            throw new ModelsIllegalargumentException("Couldn't find field: " + fRound, this, pm);
        }

        // the dot as decimal separator, whatever the locale
        DecimalFormat formatter = new DecimalFormat(pPattern, DecimalFormatSymbols.getInstance(Locale.ROOT));
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        formatter.setGroupingUsed(false);

        outVector = new DefaultFeatureCollection();

        int size = inVector.size();
        pm.beginTask("Rounding data...", size);
        FeatureIterator<SimpleFeature> inFeatureIterator = inVector.features();
        try {
            while( inFeatureIterator.hasNext() ) {
                SimpleFeature feature = inFeatureIterator.next();

                Object attribute = feature.getAttribute(field);
                if (attribute instanceof Number) {
                    double num = ((Number) attribute).doubleValue();
                    num = formatter.parse(formatter.format(num)).doubleValue();
                    feature.setAttribute(field, num);
                }

                ((DefaultFeatureCollection) outVector).add(feature);
                pm.worked(1);
            }
        } finally {
            inFeatureIterator.close();
        }
        pm.done();
    }

}
