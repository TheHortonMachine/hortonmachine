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
package org.hortonmachine.gears.modules.v.vectortablejoiner;

import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_KEYWORDS;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_LABEL;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_LICENSE;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_NAME;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_STATUS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureExtender;
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

@Description(OMSVECTORTABLEJOINER_DESCRIPTION)
@Documentation(OMSVECTORTABLEJOINER_DOCUMENTATION)
@Author(name = OMSVECTORTABLEJOINER_AUTHORNAMES, contact = OMSVECTORTABLEJOINER_AUTHORCONTACTS)
@Keywords(OMSVECTORTABLEJOINER_KEYWORDS)
@Label(OMSVECTORTABLEJOINER_LABEL)
@Name(OMSVECTORTABLEJOINER_NAME)
@Status(OMSVECTORTABLEJOINER_STATUS)
@License(OMSVECTORTABLEJOINER_LICENSE)
public class OmsVectorTableJoiner extends HMModel {

    @Description(OMSVECTORTABLEJOINER_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector;

    @Description(OMSVECTORTABLEJOINER_TABLEDATA_DESCRIPTION)
    @In
    public Map<String, List<Object>> tabledata = null;

    @Description(OMSVECTORTABLEJOINER_F_COMMON_DESCRIPTION)
    @In
    public String fCommon = null;

    @Description(OMSVECTORTABLEJOINER_P_FIELDS_DESCRIPTION)
    @In
    public String pFields = null;

    @Description(OMSVECTORTABLEJOINER_OUT_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector;

    public static final String OMSVECTORTABLEJOINER_DESCRIPTION = "Joins the fields of a table to the features of a vector with the same value in a common field.";
    public static final String OMSVECTORTABLEJOINER_DOCUMENTATION = """
            Each feature takes the joined fields of the first row of the table with the same value of the \
            common field, which can have different names in the vector and in the table, as basin_id,id. \
            The values are matched as text, so a whole number matches also when it is stored as an \
            integer in one source and as a decimal number or a text in the other, as 12, 12.0 and "12"; \
            texts are matched without the spaces at their ends. Each joined field takes the type of its \
            first value in the table.

            It is an inner join: the features without a matching row are left out, and their number is \
            reported. The joined fields are added to the ones of the vector, so they must have names \
            that the vector doesn't have yet. In a shapefile the names are cut to 10 characters.
            """;
    public static final String OMSVECTORTABLEJOINER_KEYWORDS = "Join, Vector, Table, Attributes";
    public static final String OMSVECTORTABLEJOINER_LABEL = VECTORPROCESSING;
    public static final String OMSVECTORTABLEJOINER_NAME = "vjoin";
    public static final int OMSVECTORTABLEJOINER_STATUS = 40;
    public static final String OMSVECTORTABLEJOINER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSVECTORTABLEJOINER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSVECTORTABLEJOINER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSVECTORTABLEJOINER_IN_VECTOR_DESCRIPTION = "The vector to extend.";
    public static final String OMSVECTORTABLEJOINER_TABLEDATA_DESCRIPTION = "The table to join, as the values of each field by field name.";
    public static final String OMSVECTORTABLEJOINER_IN_TABLE_DESCRIPTION = "The table to join: a dbf file or the attribute table of a vector.";
    public static final String OMSVECTORTABLEJOINER_F_COMMON_DESCRIPTION = "The common field or, if it has different names in the two sources, the two names separated by a comma, first the one of the vector, then the one of the table.";
    public static final String OMSVECTORTABLEJOINER_P_FIELDS_DESCRIPTION = "The comma separated list of the fields of the table to join.";
    public static final String OMSVECTORTABLEJOINER_OUT_VECTOR_DESCRIPTION = "The joined vector.";

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }
        checkNull(inVector, tabledata, fCommon, pFields);

        String vectorField = fCommon.trim();
        String tableField = vectorField;
        if (fCommon.indexOf(',') != -1) {
            String[] split = fCommon.split(",");
            vectorField = split[0].trim();
            tableField = split[1].trim();
        }
        String vectorFieldName = FeatureUtilities.findAttributeName(inVector.getSchema(), vectorField);
        if (vectorFieldName == null) {
            throw new ModelsIllegalargumentException("Couldn't find the field in the vector: " + vectorField, this, pm);
        }
        List<Object> commonValues = tabledata.get(tableField);
        if (commonValues == null) {
            throw new ModelsIllegalargumentException("Couldn't find the field in the table: " + tableField, this, pm);
        }

        String[] fields = pFields.split(",");
        Class< ? >[] classes = new Class< ? >[fields.length];
        for( int i = 0; i < fields.length; i++ ) {
            fields[i] = fields[i].trim();
            List<Object> list = tabledata.get(fields[i]);
            if (list == null) {
                throw new ModelsIllegalargumentException("Couldn't find the field in the table: " + fields[i], this, pm);
            }
            if (FeatureUtilities.findAttributeName(inVector.getSchema(), fields[i]) != null) {
                throw new ModelsIllegalargumentException("The vector already has the field: " + fields[i], this, pm);
            }
            // the type of the first value
            classes[i] = String.class;
            for( Object value : list ) {
                if (value != null) {
                    classes[i] = value.getClass();
                    break;
                }
            }
        }

        // the first row of each value of the common field
        Map<String, Integer> key2Row = new HashMap<>();
        for( int row = 0; row < commonValues.size(); row++ ) {
            String key = toKey(commonValues.get(row));
            if (key != null) {
                key2Row.putIfAbsent(key, row);
            }
        }

        FeatureExtender fExt = new FeatureExtender(inVector.getSchema(), fields, classes);
        // a list keeps the features in their order
        ListFeatureCollection outFC = new ListFeatureCollection(fExt.getNewFeatureType());

        int size = inVector.size();
        int notJoined = 0;
        pm.beginTask("Merging data...", size);
        FeatureIterator<SimpleFeature> inFeatureIterator = inVector.features();
        try {
            while( inFeatureIterator.hasNext() ) {
                SimpleFeature feature = inFeatureIterator.next();
                pm.worked(1);

                String key = toKey(feature.getAttribute(vectorFieldName));
                Integer row = key == null ? null : key2Row.get(key);
                if (row == null) {
                    notJoined++;
                    continue;
                }
                Object[] newAttributes = new Object[fields.length];
                for( int i = 0; i < fields.length; i++ ) {
                    newAttributes[i] = tabledata.get(fields[i]).get(row);
                }
                outFC.add(fExt.extendFeature(feature, newAttributes));
            }
        } finally {
            inFeatureIterator.close();
        }
        pm.done();
        if (notJoined > 0) {
            pm.message("Features without a matching row, left out: " + notJoined);
        }
        outVector = outFC;
    }

    /**
     * The value of a common field as text, so that a whole number matches as integer, decimal or text.
     *
     * @param value the value.
     * @return the key, or null for an empty value.
     */
    public static String toKey( Object value ) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1E15) {
                return Long.toString((long) d);
            }
            return Double.toString(d);
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            // a number as text
            return toKey(Double.parseDouble(text));
        } catch (NumberFormatException e) {
            return text;
        }
    }

}
