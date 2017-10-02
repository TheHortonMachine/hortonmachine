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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_F_COMMON_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_OUT_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_P_FIELDS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_STATUS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_TABLEDATA_DESCRIPTION;

import java.util.HashMap;
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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureExtender;
import org.opengis.feature.simple.SimpleFeature;

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
    public HashMap<String, List<Object>> tabledata = null;

    @Description(OMSVECTORTABLEJOINER_F_COMMON_DESCRIPTION)
    @In
    public String fCommon = null;

    @Description(OMSVECTORTABLEJOINER_P_FIELDS_DESCRIPTION)
    @In
    public String pFields = null;

    @Description(OMSVECTORTABLEJOINER_OUT_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }

        String[] fields = pFields.split(","); //$NON-NLS-1$
        Class< ? >[] classes = new Class< ? >[fields.length];
        for( int i = 0; i < fields.length; i++ ) {
            List<Object> list = tabledata.get(fields[i].trim());
            classes[i] = list.get(0).getClass();
        }

        String shapeField = fCommon;
        String tableField = fCommon;
        int comma = fCommon.indexOf(","); //$NON-NLS-1$
        if (comma != -1) {
            String[] split = fCommon.split(","); //$NON-NLS-1$
            shapeField = split[0].trim();
            tableField = split[1].trim();
        }

        List<Object> commonAttributeList = tabledata.get(tableField);

        FeatureExtender fExt = new FeatureExtender(inVector.getSchema(), fields, classes);

        outVector = new DefaultFeatureCollection();

        int size = inVector.size();
        pm.beginTask("Merging data...", size);
        FeatureIterator<SimpleFeature> inFeatureIterator = inVector.features();
        while( inFeatureIterator.hasNext() ) {
            SimpleFeature feature = inFeatureIterator.next();

            Object attribute = feature.getAttribute(shapeField);
            int index = commonAttributeList.indexOf(attribute);
            if (index == -1) {
                // try something if it is number
                if (attribute instanceof Number) {
                    Double doubleAttribute = ((Number) attribute).doubleValue();
                    index = commonAttributeList.indexOf(doubleAttribute);
                }
                if (index == -1) {
                    System.out.println("Jumped feature: " + feature.getID());
                    continue;
                }
            }
            Object[] newAttributes = new Object[fields.length];
            for( int i = 0; i < fields.length; i++ ) {
                List<Object> list = tabledata.get(fields[i]);
                Object object = list.get(index);
                newAttributes[i] = object;
            }

            SimpleFeature newFeature = fExt.extendFeature(feature, newAttributes);

            ((DefaultFeatureCollection) outVector).add(newFeature);
            pm.worked(1);
        }
        pm.done();

        inFeatureIterator.close();

    }

}
